package com.yashsoni.visualrecognitionsample.activities;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassResult;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifiedImages;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyOptions;
import com.yashsoni.visualrecognitionsample.R;
import com.yashsoni.visualrecognitionsample.models.VisualRecognitionResponseModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class HomeActivity extends AppCompatActivity {

    // IBM WATSON VISUAL RECOGNITION RELATED
    private final String API_KEY = "ayZsvsj6j4ITxUbElQj2CS-fAlBdGckv3UEQ54ZgqpX6";

    Button btnFetchResults,btnPickImage;
    EditText etUrl;
    ProgressBar progressBar;
    View content;
    Single<ClassifiedImages> observable;
    private float threshold = (float) 0.6;
    private Uri imageUri;
    File file;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        observable = Single.create((SingleOnSubscribe<ClassifiedImages>) emitter -> {
            IamOptions options = new IamOptions.Builder()
                    .apiKey(API_KEY)
                    .build();

            VisualRecognition visualRecognition = new VisualRecognition("2018-03-19", options);
            
            ClassifyOptions classifyOptions = new ClassifyOptions.Builder()
                    .imagesFile(new File(getRealPathFromURI(this,imageUri)))
                    .classifierIds(Arrays.asList("default"))
                    .threshold(threshold)
                    .owners(Collections.singletonList("me"))
                    .build();
            ClassifiedImages classifiedImages = visualRecognition.classify(classifyOptions).execute();
            emitter.onSuccess(classifiedImages);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void goToNext(String url, List<ClassResult> resultList) {
        progressBar.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);

        // Checking if image has a class named "explicit". If yes, then reject and show an error msg as a Toast
        for (ClassResult result : resultList) {
            if(result.getClassName().equals("default")) {
                Toast.makeText(this, "CONTENT IS FOOD", Toast.LENGTH_LONG).show();
            }
        }

        Toast.makeText(this, "CONTENT IS NOT FOOD", Toast.LENGTH_LONG).show();

        // No Explicit content found, go ahead with processing results and moving to Results Activity
        ArrayList<VisualRecognitionResponseModel> classes = new ArrayList<>();
        for (ClassResult result : resultList) {
            VisualRecognitionResponseModel model = new VisualRecognitionResponseModel();
            model.setClassName(result.getClassName());
            model.setScore(result.getScore());
            classes.add(model);
        }
        /**
         * Uncomment following code for getting only 1 class as result
         * **/
       /* VisualRecognitionResponseModel model = new VisualRecognitionResponseModel();
        model.setClassName(resultList.get(0).getClassName());
        model.setScore(resultList.get(0).getScore());
        classes.add(model);*/

        Intent i = new Intent(HomeActivity.this, ResultsActivity.class);
        i.putExtra("url", imageUri.toString());
        i.putParcelableArrayListExtra("classes", classes);
        startActivity(i);
    }

    private void initializeViews() {

        btnFetchResults = findViewById(R.id.btn_fetch_results);
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);
        content = findViewById(R.id.ll_content);
        btnPickImage = findViewById(R.id.btn_pick_image);
        imageView = findViewById(R.id.imageView);

        btnPickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i,1);
            }
        });

        btnFetchResults.setOnClickListener(v -> {
            if (imageUri!=null) {
                content.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                observable.subscribe(new SingleObserver<ClassifiedImages>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(ClassifiedImages classifiedImages) {
                        System.out.println(classifiedImages.toString());
                        List<ClassResult> resultList = classifiedImages.getImages().get(0).getClassifiers().get(0).getClasses();
                        String url = classifiedImages.getImages().get(0).getSourceUrl();
                        goToNext(url, resultList);
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.println(e.getMessage());
                    }
                });
            } else {
                Toast.makeText(HomeActivity.this, "Please make sure image URL is proper!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode ==1 && resultCode == RESULT_OK){
            imageUri = data.getData();
            file = new File(getRealPathFromURI(this,imageUri));
            imageView.setImageURI(imageUri);
        }
    }
}