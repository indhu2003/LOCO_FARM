package com.example.demoapp;

import static android.app.Activity.RESULT_OK;

import static androidx.core.content.PermissionChecker.checkSelfPermission;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import android.Manifest;

import android.content.Intent;
import android.content.pm.PackageManager;

import android.provider.MediaStore;
import android.widget.Toast;





import com.example.demoapp.ml.TestNew;




import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;



public class DiseaseFragment extends Fragment {


        private static final int IMAGE_SIZE = 224;

        private Button cameraBtn;
        private Button galleryBtn;
        private ImageView imageView;
        private TextView resultTextView;

        private int maxPos = 0;

        private int imageSize = 224;






        @SuppressLint("MissingInflatedId")
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_disease, container, false);

            cameraBtn = view.findViewById(R.id.button_capture_image);
            galleryBtn = view.findViewById(R.id.button_analyze_image);
            imageView = view.findViewById(R.id.imageView);
            resultTextView = view.findViewById(R.id.textView_prediction);

            cameraBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PermissionChecker.PERMISSION_GRANTED) {
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(cameraIntent, 3);
                    } else {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                    }
                }
            });

            galleryBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(galleryIntent, 1);
                }
            });

            return view;
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            if (requestCode == 100) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 3);
                } else {
                    Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }


        private void classifyImage(Bitmap image) {
            try {
                TestNew model = TestNew.newInstance(getContext());

                // Creates inputs for reference.
                TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, imageSize, imageSize, 3}, DataType.FLOAT32);
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
                byteBuffer.order(ByteOrder.nativeOrder());

                int[] intValues = new int[imageSize* imageSize];
                image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
                int pixel = 0;
                //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
                for (int i = 0; i < imageSize; i++) {
                    for (int j = 0; j < imageSize; j++) {
                        int val = intValues[pixel++]; // RGB
                        byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f));
                        byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f));
                        byteBuffer.putFloat((val & 0xFF) * (1.f));
                    }
                }

                inputFeature0.loadBuffer(byteBuffer);

                // Runs model inference and gets result.
                TestNew.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                float[] confidences = outputFeature0.getFloatArray();
                // find the index of the class with the biggest confidence.
                int maxPos = 0;
                float maxConfidence = 0;
                for (int i = 0; i < confidences.length; i++) {
                    if (confidences[i] > maxConfidence) {
                        maxConfidence = confidences[i];
                        maxPos = i;
                    }
                }
                String[] labels = new String[14];
                int cnt = 0;
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getActivity().getAssets().open("test_label.txt")));
                    String line = bufferedReader.readLine();
                    while (line != null) {
                        labels[cnt] = line;
                        cnt++;
                        line = bufferedReader.readLine();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                resultTextView.setText(labels[getMax(outputFeature0.getFloatArray())] + "   ");

                // Releases model resources if no longer used.
                model.close();
            } catch (IOException e) {
                // TODO Handle the exception
            }
        }

        int getMax(float[] arr) {
            int max = 0;
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] > arr[max])
                    max = i;
            }

            return max;
        }
    }