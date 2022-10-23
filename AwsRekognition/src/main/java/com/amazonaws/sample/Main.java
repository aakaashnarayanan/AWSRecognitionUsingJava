package com.amazonaws.sample;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Scanner;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.rds.model.InvalidS3BucketException;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AgeRange;
import com.amazonaws.services.rekognition.model.Attribute;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;

public class Main {

	public static void LabelDetectionOnDevice(AmazonRekognition rekognitionClient, ByteBuffer imageBytes) {
		DetectLabelsRequest OffLabel = new DetectLabelsRequest().withImage(new Image().withBytes(imageBytes));
		DetectLabelsResult result = rekognitionClient.detectLabels(OffLabel);
		PrintLabel(result);
	}

	public static void FaceAnalysisOnDevice(AmazonRekognition rekognitionClient, ByteBuffer imageBytes) {
		DetectFacesRequest faceRequest = new DetectFacesRequest().withImage(new Image().withBytes(imageBytes))
				.withAttributes(Attribute.ALL);
		DetectFacesResult Faceresult = rekognitionClient.detectFaces(faceRequest);
		FaceAnalysisPrint(Faceresult);
	}

	public static void LabelDetectionOnS3(AmazonRekognition rekognitionClient, String bucket, String key) {
		try {
			DetectLabelsRequest request = new DetectLabelsRequest()
					.withImage(new Image().withS3Object(new S3Object().withName(key).withBucket(bucket)))
					.withMaxLabels(10).withMinConfidence(75F);
			DetectLabelsResult result = rekognitionClient.detectLabels(request);
			PrintLabel(result);
		} catch (InvalidS3BucketException S) {
			System.out.println("File name may be wrong..!");
		} catch (AmazonServiceException e) {
			e.printStackTrace();
		}
	}
	
	public static void PPFDetectionOnDevice(AmazonRekognition rekognitionClient, ByteBuffer imageByte)
	{
		
	}

	public static void FaceDetectionOnS3(AmazonRekognition rekognitionClient, String bucket, String key) {
		DetectFacesRequest Facerequest = new DetectFacesRequest()
				.withImage(new Image().withS3Object(new S3Object().withName(key).withBucket(bucket)))
				.withAttributes(Attribute.ALL);

		DetectFacesResult result = rekognitionClient.detectFaces(Facerequest);
		FaceAnalysisPrint(result);
	}

	public static void PrintLabel(DetectLabelsResult result) {
		List<Label> labels = result.getLabels();

		System.out.println("------ Labels -------");
		for (Label label : labels) {
			System.out.println(label.getName() + ": " + label.getConfidence().toString());
		}
	}

	public static void FaceAnalysisPrint(DetectFacesResult result) {
		List<FaceDetail> faceDetails = result.getFaceDetails();
		System.out.println("Total Number of face Detected.. : " + faceDetails.size());
		System.out.println("------ Face Feature -------");
		System.out.println();
		for (FaceDetail face : faceDetails) {
			AgeRange age = face.getAgeRange();
			System.out.println("Age Between " + age.getLow().toString() + " and " + age.getHigh().toString());
			System.out.println("Gender : " + face.getGender().getValue().toString());
			System.out.println("Smile : " + face.getSmile().getValue());
			System.out.println("Emotion : " + face.getEmotions().get(0).getType().toString());
		}
	}

	public static ByteBuffer LocalImgByte(String path) {
		ByteBuffer imageBytes = null;

		try (InputStream inputstream = new FileInputStream(new File(path))) {
			imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputstream));
		} catch (FileNotFoundException fileNotFound) {
			System.out.println("Check the Path given !");
		} catch (Exception e) {
			e.getStackTrace();
		}
		return imageBytes;
	}

	public static void uploadS3(AmazonS3 s3, String bucketName, String key, String path) {
		try {

			if (!s3.doesObjectExist(bucketName, key)) {
				s3.putObject(new PutObjectRequest(bucketName, key, new File(path)));
				System.out.println("File upload success..");
			} else {
				System.out.println("File name already exist!");
			}
		} catch (AmazonServiceException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		AWSCredentials credentials = new ProfileCredentialsProvider("Kash").getCredentials();
		AmazonS3 s3client = AmazonS3ClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion("ap-south-1").build();
		AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion("ap-south-1").build();

		String bucketName = "samplebucketkash";

		Scanner sc = new Scanner(System.in);
		System.out.println("Recogniton using S3[1] or onDevice[2] or save Image to s3[3]");
		int choice = sc.nextInt();

		if (choice == 1) {
			System.out.println("Choose LabelDetection [1], FaceAnalysis [2], MaskDetection [3]");
			int detectChoice = sc.nextInt();
			System.out.println("This are the list of File availabe in S3 bucket..");
			S3Objects.inBucket(s3client, bucketName).forEach((S3ObjectSummary objectSummary) -> {
				System.out.println(objectSummary.getKey());
			});
			System.out.println("Enter the name of the file");
			String fileName = sc.next();
			if (detectChoice == 1) {
				LabelDetectionOnS3(rekognitionClient, bucketName, fileName);
			} else if (detectChoice == 2) {
				FaceDetectionOnS3(rekognitionClient, bucketName, fileName);
			} else if (detectChoice == 3) {

			} else {
				System.out.println("the choice entered is wrong !");
			}

		} else if (choice == 2) {
			System.out.println("Enter the path : ");
			String pathString = sc.next();
			System.out.println("Choose LabelDetection [1], FaceAnalysis [2], MaskDetection [3]");
			int detectChoice = sc.nextInt();
			if (detectChoice == 1) {
				LabelDetectionOnDevice(rekognitionClient, LocalImgByte(pathString));
			} else if (detectChoice == 2) {
				FaceAnalysisOnDevice(rekognitionClient, LocalImgByte(pathString));
			} else if (detectChoice == 3) {

			} else {
				System.out.println("the choice entered is wrong !");
			}

		} else if (choice == 3) {
			System.out.println("Enter the path : ");
			String pathString = sc.next();
			System.out.println("Enter file name : ");
			String FileName = sc.next();

			uploadS3(s3client, bucketName, FileName, pathString);

		} else {
			System.out.println("The choice u entered is wrong !");
		}
		sc.close();
	}
}
