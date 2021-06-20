package com.example.objectdetection;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.metadata.MetadataExtractor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.min;

public class Detector {

    private final String modelFilename = "ssd_mobilenet_v1_1_metadata_1.tflite";
    private final String lableFilename = "labelmap.txt";
    private final int inputImgSizeX;
    private final int inputImgSizeY;
    private List<String> labels = new ArrayList<>();

    private static final int NUM_DETECTIONS = 10;
    //outputLocations: array of shape [BatchSize, NUM_DETECTIONS, 4]
    //contains the locations of detected bexes
    private float[][][] outputLocations;
    //outputClasses: array of shape [BatchSize, NUM_DETECTIONS]
    //contains the classes of detected boxes
    private float[][] outputClasses;
    //outputScores: array of shape [BatchSize, NUM_DETECTIONS]
    //contains the scores of detected boxes
    private float[][] outputScores;
    //numDetections: array of shape [BatchSize]
    //contains the number of detected boxes
    private float[] numDetections;

    public ByteBuffer imgData;
    private final Interpreter tflite;

    public Detector(Context context) throws IOException {
        MappedByteBuffer modelFile = loadModelFile(context.getAssets(), modelFilename);
        MetadataExtractor metadata = new MetadataExtractor(modelFile);
        InputStream labelsFile = metadata.getAssociatedFile(lableFilename);
        labels = loadLabelFile(labelsFile);

        tflite = new Interpreter(modelFile, null);

        int[] inputImageShape = tflite.getInputTensor(0).shape();   //{Batch_size, Height, Width, 3}
        inputImgSizeX = inputImageShape[1];
        inputImgSizeY = inputImageShape[2];

        //Creates a ByteBuffer that will hold the image data and can be read by the interpreter
        //In the quantized model, each pixel is represented by 3 bytes
        imgData = ByteBuffer.allocateDirect(inputImgSizeX * inputImgSizeY * 3);
        imgData.order(ByteOrder.nativeOrder());

        //Create containers for the model outputs
        outputLocations = new float[1][NUM_DETECTIONS][4];
        outputClasses = new float[1][NUM_DETECTIONS];
        outputScores = new float[1][NUM_DETECTIONS];
        numDetections = new float[1];
    }


    public List<Recognition> recognizeImage(final Bitmap bitmap, final int sensorOrientation)
    {
        //Pre-process the input image
        Bitmap ResizedImage = Bitmap.createScaledBitmap(bitmap, inputImgSizeX, inputImgSizeY, true);
        convertBitmapToByteBuffer(ResizedImage);

        Object[] inputArray = {imgData};        //why ?
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputLocations);
        outputMap.put(1, outputClasses);
        outputMap.put(2, outputScores);
        outputMap.put(3, numDetections);

        tflite.runForMultipleInputsOutputs(inputArray, outputMap); //run inference

        //Use the model's output number of detections
        int numDetectionOutput = min(NUM_DETECTIONS, (int)numDetections[0]);  //cast from array to integer

        final ArrayList<Recognition> recognitions = new ArrayList<>(numDetectionOutput);

        for(int i = 0; i < numDetectionOutput; i++)
        {
            final RectF detection = new RectF (outputLocations[0][i][1] * inputImgSizeX,
                    outputLocations[0][i][0] * inputImgSizeY,
                    outputLocations[0][i][3] * inputImgSizeX,
                    outputLocations[0][i][2] * inputImgSizeY);

            recognitions.add(
                    new Recognition("" + i, labels.get((int) outputClasses[0][i]), outputScores[0][i], detection)
            );
        }
        return recognitions;
    }








    //A function that loads the model.tflite file from the assets folders to the memory as a MappedByteBuffer that the
    //tflite interpreter accepts
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFileName) throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    //A function that reads the labels from the text file associated in the model metadata and stores
    //them into a list of strings.
    private static List<String> loadLabelFile(InputStream labelsFile) throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(labelsFile));
        String line;
        while ((line = reader.readLine()) != null){
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }


    //A function that will store the image data into a ByteBuffer that is accepted by the interpreter
    private void convertBitmapToByteBuffer(Bitmap bitmap){
        if (imgData == null)
            return;
        imgData.rewind();

        //An array that will hold the values stored in the bitmap
        int[] intValues = new int[inputImgSizeX * inputImgSizeY];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        //loop through all pixels
        for (int i = 0; i < inputImgSizeX; i++)
            for(int j = 0; j < inputImgSizeY; j++)
            {
                int pixelValue = intValues[i * inputImgSizeX + j];
                imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                imgData.put((byte) (pixelValue & 0xFF));
            }
    }


}



