package com.rdk.photofilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import org.opencv.core.Core;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;

import org.opencv.core.*;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.lang.Math;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    public static final int CAMERA_PERM_CODE = 101;
    public static final int CAMERA_REQUEST_CODE = 102;
    public static final int GALLERY_REQUEST_CODE = 105;
    Button resizeBtn;
    ImageView gaussBtn;
    ImageButton rotateLeftBtn;
    ImageButton rotateRightBtn;
    ImageView imageView;
    Uri imageUri;
    Bitmap grayBitmap;
    Bitmap imageBitmap;

    Bitmap actualBitmap;

    Bitmap timeBitmap;
    private AppCompatSeekBar brightnessSeekBar;
    private SeekBar contrastSeekBar;
    ImageButton cameraBtn;
    String currentPhotoPath;

    ImageView sepiaBtn;
    ImageView negativeBtn;
    ImageView redBtn;
    ImageView greenBtn;
    ImageView blueBtn;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        redBtn = (ImageView)findViewById(R.id.imageView15);
        greenBtn = (ImageView)findViewById(R.id.imageView16);
        blueBtn = (ImageView)findViewById(R.id.imageView17);

        imageView = (ImageView) findViewById(R.id.imageView);
        cameraBtn = findViewById(R.id.cameraBtn);
        sepiaBtn = (ImageView)findViewById(R.id.sepiaBtn);
        gaussBtn = (ImageView)findViewById(R.id.gaussBtn);
        rotateLeftBtn= findViewById(R.id.rotateLeft);
        rotateRightBtn=findViewById(R.id.rotateRight);
        negativeBtn = (ImageView)findViewById(R.id.imageView13);



        OpenCVLoader.initDebug();

        //Incercare imagine default
        //imageView.setImageResource(R.drawable.noimg);

        int[] colors = new int[90000];
        Arrays.fill(colors,255);
        imageBitmap = Bitmap.createBitmap(colors,300,300,Bitmap.Config.ARGB_8888);
        actualBitmap = Bitmap.createBitmap(colors,300,300,Bitmap.Config.ARGB_8888);


        brightnessSeekBar = (AppCompatSeekBar) findViewById(R.id.brightnessSeekBar);
        brightnessSeekBar.setOnSeekBarChangeListener(this);
        contrastSeekBar = (SeekBar)findViewById(R.id.contrastSeekBar);
        contrastSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Bitmap edited = increaseContrast(actualBitmap, progress);
                imageView.setImageBitmap(edited);
                timeBitmap=edited;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askCameraPermissions();
            }
        });



        sepiaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sepia();
            }
        });

        gaussBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gaussianFilter();
            }
        });

        rotateLeftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateLeft();
            }
        });

        negativeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getNegativeImage();
            }
        });

        redBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getRedImage();
            }
        });

        greenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getGreenImage();
            }
        });

        blueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getBlueImage();
            }
        });


        rotateRightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateRight();
            }
        });

    }

    private void askCameraPermissions() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.CAMERA}, CAMERA_PERM_CODE);
        }else {
            openCamera();
        }

    }
    public void onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if(requestCode == CAMERA_REQUEST_CODE){
            if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                openCamera();
            }
            else {
                Toast.makeText(this,"Camera Permission is Required to Use camera", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void openCamera(){
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(camera, CAMERA_REQUEST_CODE);

    }







    public void openGallery(View v){
        Intent myIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(myIntent,100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100 && resultCode ==RESULT_OK && data!=null){
            imageUri = data.getData();

            try {
                imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            imageView.setImageBitmap(imageBitmap);
            actualBitmap=imageBitmap;


        }
        if(requestCode==CAMERA_REQUEST_CODE && data!=null){
            imageBitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(imageBitmap);
            actualBitmap=imageBitmap;
        }
    }

    public void convertToGray(View v) {
        Mat rgba = new Mat();
        Mat grayMat = new Mat();

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inDither = false;
        o.inSampleSize=4;

        int width = imageBitmap.getWidth();
        int height = imageBitmap.getHeight();

        grayBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.RGB_565);

        //bitmap to mat

        Utils.bitmapToMat(imageBitmap,rgba);

        Imgproc.cvtColor(rgba,grayMat,Imgproc.COLOR_RGB2GRAY);

        Utils.matToBitmap(grayMat,grayBitmap);

        //imageBitmap = grayBitmap;
        imageView.setImageBitmap(grayBitmap);
        actualBitmap=grayBitmap;
    }




    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Bitmap edited = increaseBrightness(actualBitmap, progress);
        imageView.setImageBitmap(edited);
    }

    private Bitmap increaseBrightness(Bitmap imageBitmap, int value) {
        Mat src = new Mat(imageBitmap.getHeight(),imageBitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(imageBitmap,src);
        src.convertTo(src,-1,contrastSeekBar.getProgress(),value);
        Bitmap result = Bitmap.createBitmap(src.cols(),src.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src,result);
        return result;
    }

    private Bitmap increaseContrast(Bitmap imageBitmap, int progress) {
        Mat src = new Mat(imageBitmap.getHeight(),imageBitmap.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(imageBitmap,src);
        src.convertTo(src,-1,progress,brightnessSeekBar.getProgress());
        Bitmap result = Bitmap.createBitmap(src.cols(),src.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src,result);
        return result;
    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public void cannyEdges(View v){
        Mat src = new Mat(imageBitmap.getHeight(),imageBitmap.getWidth(), CvType.CV_8UC3);
        Mat srcBlur = new Mat();
        Mat detectedEdges = new Mat();
        Mat dst;
        Utils.bitmapToMat(imageBitmap,src);

        Imgproc.blur(src,srcBlur,new Size(5,5));
        double lowThresh = 15;
        final double RATIO = 5.0;
        final int KERNEL_SIZE = 3;
        Imgproc.Canny(srcBlur, detectedEdges, lowThresh, lowThresh * RATIO,  KERNEL_SIZE, false);

        dst = new Mat(src.size(), CvType.CV_8UC3, Scalar.all(0));
        src.copyTo(dst, detectedEdges);


        Bitmap detectedEdgesBitmap = Bitmap.createBitmap(imageBitmap.getWidth(),imageBitmap.getHeight(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(detectedEdges, detectedEdgesBitmap);

        imageView.setImageBitmap(detectedEdgesBitmap);
        actualBitmap=detectedEdgesBitmap;



    }

    public void resizeImage(){
        int width = imageBitmap.getWidth();
        int height = imageBitmap.getHeight();
        int newWidth = 200;
        int newHeight = 200;

// calculate the scale - in this case = 0.4f
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

// createa matrix for the manipulation
        Matrix matrix = new Matrix();
// resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);

// recreate the new Bitmap
        Bitmap resizedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0,
                width, height, matrix, true);

// make a Drawable from Bitmap to allow to set the BitMap
// to the ImageView, ImageButton or what ever
        BitmapDrawable bmd = new BitmapDrawable(resizedBitmap);


// set the Drawable on the ImageView
        imageView.setImageDrawable(bmd);

    }

    public Mat convolveSepia(Mat img){

        Mat res = new Mat(img.rows(),img.cols(),CvType.CV_32FC3);

        for(int i=0;i<img.rows();i++){
            for(int j = 0; j<img.cols();j++){
                double[] rgb = img.get(i,j);
                double R = rgb[0];
                double G = rgb[1];
                double B = rgb[2];
                double newRed = (double)((int)(0.393*R + 0.769*G + 0.189*B));
                double newGreen = (double)((int)(0.349*R + 0.686*G + 0.168*B));
                double newBlue = (double)((int)(0.272*R + 0.534*G + 0.131*B));

                double[] modifiedRGB ={newRed,newGreen,newBlue};
                res.put(i,j,modifiedRGB);
            }
        }
        return res;
    }

    public void sepia(){
        Mat rgba = new Mat();
        Utils.bitmapToMat(imageBitmap,rgba);
        Mat resized;
        Size newSize;
        if(imageBitmap.getWidth()>250 || imageBitmap.getHeight() > 250) {
            Log.i("IfDimensiune", "Entered then");
            resized = new Mat(imageBitmap.getHeight() / 7, imageBitmap.getWidth() / 7, CvType.CV_8UC3);
            newSize = new Size(imageBitmap.getWidth()/7,imageBitmap.getHeight()/7);
        }else{
            Log.i("IfDimensiune", "Entered else");
            resized = new Mat(imageBitmap.getHeight(), imageBitmap.getWidth(), CvType.CV_8UC3);
            newSize = new Size(imageBitmap.getWidth(),imageBitmap.getHeight());
        }

        Imgproc.resize(rgba,resized,newSize,0,0,Imgproc.INTER_AREA);

        Mat newMat = convolveSepia(resized);
        newMat.convertTo(newMat, CvType.CV_8UC3);

        Bitmap result = Bitmap.createBitmap(newMat.cols(),newMat.rows(),Bitmap.Config.RGB_565);
        Utils.matToBitmap(newMat,result);

        imageView.setImageBitmap(result);
        actualBitmap=result;

    }

    public void gaussianFilter(){
        Mat rgba = new Mat();

        Utils.bitmapToMat(imageBitmap,rgba);
        Mat resized;
        Size newSize;

        if(imageBitmap.getWidth()>300 || imageBitmap.getHeight() > 300) {
            Log.i("IfDimensiune", "Entered then");
            resized = new Mat(imageBitmap.getHeight() / 10, imageBitmap.getWidth() / 10, CvType.CV_8UC3);
            newSize = new Size(imageBitmap.getWidth()/10,imageBitmap.getHeight()/10);
        }else{
            Log.i("IfDimensiune", "Entered else");
            resized = new Mat(imageBitmap.getHeight(), imageBitmap.getWidth(), CvType.CV_8UC3);
            newSize = new Size(imageBitmap.getWidth(),imageBitmap.getHeight());
        }
        Imgproc.resize(rgba,resized,newSize,0,0,Imgproc.INTER_AREA);

        Mat dst = new Mat(resized.rows(),resized.cols(), CvType.CV_8UC3);

        int w =7;

        float sigma =(float)w/6;
        int k = w/2;
        double[][] G=new double[7][7];
        for(int x=0; x<w; x++){
            for(int y=0; y<w; y++){
                G[x][y] = 1.0/(2 * Math.PI*sigma*sigma) * Math.exp(-(((x - k)*(x - k) + (y - k)*(y - k)) / (2 * sigma*sigma)));
            }
        }

        for(int x=k; x<resized.rows()-k;x++){
            for(int y = k; y<resized.cols()-k;y++){
                int auxR = 0;
                int auxG = 0;
                int auxB = 0;
                for(int i = -k; i<=k; i++){
                    for(int j=-k; j<=k;j++){
                        double[] rgb = resized.get(x+i,y+j);
                        auxR+=rgb[0]*G[i+k][j+k];
                        auxG+=rgb[1]*G[i+k][j+k];
                        auxB+=rgb[2]*G[i+k][j+k];
                    }
                }
                double[] new_rgb = {auxR, auxG, auxB};
                dst.put(x,y,new_rgb);
            }
        }

        Bitmap newBitmap = Bitmap.createBitmap(dst.cols(),dst.rows(), imageBitmap.getConfig());
        Utils.matToBitmap(dst,newBitmap);
        imageView.setImageBitmap(newBitmap);
        actualBitmap=newBitmap;

    }

    public Mat rotate90Left(Mat src){
        Mat newMat = new Mat(src.cols(),src.rows(),src.type());
        for(int i=0;i<src.rows();i++){
            for(int j=0;j<src.cols();j++){
                double[] srcRgb= src.get(i,j);
                newMat.put(src.cols()-j-1,i,srcRgb);
            }
        }
        return newMat;
    }

    public Mat rotate90Right(Mat src){
        Mat newMat = new Mat(src.cols(),src.rows(),src.type());
        for(int i=0;i<src.rows();i++){
            for(int j=0;j<src.cols();j++){
                double[] srcRgb= src.get(i,j);
                newMat.put(j,src.rows()-1-i,srcRgb);
            }
        }
        return newMat;
    }

    public void rotateLeft(){
        Mat rgba = new Mat();

        Utils.bitmapToMat(imageBitmap,rgba);
        Mat resized;
        Size newSize;

        if(imageBitmap.getWidth()>300 || imageBitmap.getHeight() > 300) {
            Log.i("IfDimensiune", "Entered then");
            resized = new Mat(imageBitmap.getHeight() / 10, imageBitmap.getWidth() / 10, CvType.CV_8UC3);
            newSize = new Size(imageBitmap.getWidth()/10,imageBitmap.getHeight()/10);
        }else{
            Log.i("IfDimensiune", "Entered else");
            resized = new Mat(imageBitmap.getHeight(), imageBitmap.getWidth(), CvType.CV_8UC3);
            newSize = new Size(imageBitmap.getWidth(),imageBitmap.getHeight());
        }
        Imgproc.resize(rgba,resized,newSize,0,0,Imgproc.INTER_AREA);

        Mat dst;
        dst = rotate90Left(resized);

        Bitmap newBitmap = Bitmap.createBitmap(dst.cols(),dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst,newBitmap);
        imageBitmap = newBitmap;
        imageView.setImageBitmap(imageBitmap);
        actualBitmap=imageBitmap;



    }

    public void rotateRight(){
        Mat rgba = new Mat();

        Utils.bitmapToMat(imageBitmap,rgba);
        Mat resized;
        Size newSize;

        if(imageBitmap.getWidth()>300 || imageBitmap.getHeight() > 300) {
            Log.i("IfDimensiune", "Entered then");
            resized = new Mat(imageBitmap.getHeight() / 10, imageBitmap.getWidth() / 10, CvType.CV_8UC3);
            newSize = new Size(imageBitmap.getWidth()/10,imageBitmap.getHeight()/10);
        }else{
            Log.i("IfDimensiune", "Entered else");
            resized = new Mat(imageBitmap.getHeight(), imageBitmap.getWidth(), CvType.CV_8UC3);
            newSize = new Size(imageBitmap.getWidth(),imageBitmap.getHeight());
        }
        Imgproc.resize(rgba,resized,newSize,0,0,Imgproc.INTER_AREA);

        Mat dst;
        dst = rotate90Right(resized);

        Bitmap newBitmap = Bitmap.createBitmap(dst.cols(),dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst,newBitmap);
        imageBitmap = newBitmap;
        imageView.setImageBitmap(imageBitmap);
        actualBitmap=imageBitmap;



    }

    public Mat getRed(Mat src){
        Mat newMat = new Mat(src.rows(),src.cols(),CvType.CV_8UC3);
        for(int i=0;i<src.rows();i++){
            for(int j=0;j<src.cols();j++){
                double[] srcRgb= src.get(i,j);
                double auxR = srcRgb[0];
                double auxG =0;
                double auxB =0;
                double[] new_rgb = {auxR, auxG, auxB};
                newMat.put(i,j,new_rgb);
            }
        }
        return newMat;
    }

    public Mat getGreen(Mat src){
        Mat newMat = new Mat(src.rows(),src.cols(),CvType.CV_8UC3);
        for(int i=0;i<src.rows();i++){
            for(int j=0;j<src.cols();j++){
                double[] srcRgb= src.get(i,j);
                double auxR = 0;
                double auxG =srcRgb[1];
                double auxB =0;
                double[] new_rgb = {auxR, auxG, auxB};
                newMat.put(i,j,new_rgb);
            }
        }
        return newMat;
    }

    public Mat getBlue(Mat src){
        Mat newMat = new Mat(src.rows(),src.cols(),CvType.CV_8UC3);
        for(int i=0;i<src.rows();i++){
            for(int j=0;j<src.cols();j++){
                double[] srcRgb= src.get(i,j);
                double auxR = 0;
                double auxG =0;
                double auxB =srcRgb[2];
                double[] new_rgb = {auxR, auxG, auxB};
                newMat.put(i,j,new_rgb);
            }
        }
        return newMat;
    }

    public Mat getNegative(Mat src){
        Mat newMat = new Mat(src.rows(),src.cols(),CvType.CV_8UC3);
        for(int i=0;i<src.rows();i++){
            for(int j=0;j<src.cols();j++){
                double[] srcRgb= src.get(i,j);
                double auxR = 255-srcRgb[0];
                double auxG =255 - srcRgb[1];
                double auxB =255- srcRgb[2];
                double[] new_rgb = {auxR, auxG, auxB};
                newMat.put(i,j,new_rgb);
            }
        }
        return newMat;
    }

    public void getRedImage(){
        Mat rgba = new Mat();
        Utils.bitmapToMat(imageBitmap,rgba);
        Mat resized;
        Size newSize;
        if(imageBitmap.getWidth()>250 || imageBitmap.getHeight() > 250) {
            Log.i("IfDimensiune", "Entered then");
            resized = new Mat(imageBitmap.getHeight() / 7, imageBitmap.getWidth() / 7, CvType.CV_8UC3);
            newSize = new Size(imageBitmap.getWidth()/7,imageBitmap.getHeight()/7);
        }else{
            Log.i("IfDimensiune", "Entered else");
            resized = new Mat(imageBitmap.getHeight(), imageBitmap.getWidth(), CvType.CV_8UC3);
            newSize = new Size(imageBitmap.getWidth(),imageBitmap.getHeight());
        }

        Imgproc.resize(rgba,resized,newSize,0,0,Imgproc.INTER_AREA);

        Mat newMat = getRed(resized);
        newMat.convertTo(newMat, CvType.CV_8UC3);

        Bitmap result = Bitmap.createBitmap(newMat.cols(),newMat.rows(),Bitmap.Config.RGB_565);
        Utils.matToBitmap(newMat,result);

        imageView.setImageBitmap(result);
        actualBitmap=result;

    }

    public void getGreenImage(){
        Mat rgba = new Mat();
        Utils.bitmapToMat(imageBitmap,rgba);
        Mat resized;
        Size newSize;
        if(imageBitmap.getWidth()>250 || imageBitmap.getHeight() > 250) {
            Log.i("IfDimensiune", "Entered then");
            resized = new Mat(imageBitmap.getHeight() / 7, imageBitmap.getWidth() / 7, CvType.CV_8UC3);
            newSize = new Size(imageBitmap.getWidth()/7,imageBitmap.getHeight()/7);
        }else{
            Log.i("IfDimensiune", "Entered else");
            resized = new Mat(imageBitmap.getHeight(), imageBitmap.getWidth(), CvType.CV_8UC3);
            newSize = new Size(imageBitmap.getWidth(),imageBitmap.getHeight());
        }

        Imgproc.resize(rgba,resized,newSize,0,0,Imgproc.INTER_AREA);

        Mat newMat = getGreen(resized);
        newMat.convertTo(newMat, CvType.CV_8UC3);

        Bitmap result = Bitmap.createBitmap(newMat.cols(),newMat.rows(),Bitmap.Config.RGB_565);
        Utils.matToBitmap(newMat,result);

        imageView.setImageBitmap(result);
        actualBitmap=result;

    }


    public void getBlueImage(){
        Mat rgba = new Mat();
        Utils.bitmapToMat(imageBitmap,rgba);
        Mat resized;
        Size newSize;
        if(imageBitmap.getWidth()>250 || imageBitmap.getHeight() > 250) {
            Log.i("IfDimensiune", "Entered then");
            resized = new Mat(imageBitmap.getHeight() / 7, imageBitmap.getWidth() / 7, CvType.CV_8UC3);
            newSize = new Size(imageBitmap.getWidth()/7,imageBitmap.getHeight()/7);
        }else{
            Log.i("IfDimensiune", "Entered else");
            resized = new Mat(imageBitmap.getHeight(), imageBitmap.getWidth(), CvType.CV_8UC3);
            newSize = new Size(imageBitmap.getWidth(),imageBitmap.getHeight());
        }

        Imgproc.resize(rgba,resized,newSize,0,0,Imgproc.INTER_AREA);

        Mat newMat = getBlue(resized);
        newMat.convertTo(newMat, CvType.CV_8UC3);

        Bitmap result = Bitmap.createBitmap(newMat.cols(),newMat.rows(),Bitmap.Config.RGB_565);
        Utils.matToBitmap(newMat,result);

        imageView.setImageBitmap(result);
        actualBitmap=result;

    }

    public void getNegativeImage(){
        Mat rgba = new Mat();
        Utils.bitmapToMat(imageBitmap,rgba);
        Mat resized;
        Size newSize;
        if(imageBitmap.getWidth()>250 || imageBitmap.getHeight() > 250) {
            Log.i("IfDimensiune", "Entered then");
            resized = new Mat(imageBitmap.getHeight() / 7, imageBitmap.getWidth() / 7, CvType.CV_8UC3);
            newSize = new Size(imageBitmap.getWidth()/7,imageBitmap.getHeight()/7);
        }else{
            Log.i("IfDimensiune", "Entered else");
            resized = new Mat(imageBitmap.getHeight(), imageBitmap.getWidth(), CvType.CV_8UC3);
            newSize = new Size(imageBitmap.getWidth(),imageBitmap.getHeight());
        }

        Imgproc.resize(rgba,resized,newSize,0,0,Imgproc.INTER_AREA);

        Mat newMat = getNegative(resized);
        newMat.convertTo(newMat, CvType.CV_8UC3);

        Bitmap result = Bitmap.createBitmap(newMat.cols(),newMat.rows(),Bitmap.Config.RGB_565);
        Utils.matToBitmap(newMat,result);

        imageView.setImageBitmap(result);
        actualBitmap=result;

    }






}
