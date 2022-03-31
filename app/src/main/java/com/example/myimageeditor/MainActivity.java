package com.example.myimageeditor;

import static java.security.AccessController.getContext;
import static java.sql.Types.NULL;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.camera2.CameraCharacteristics;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        init();
    }

    private static final int REQUEST_PERMISSIONS= 1234;
    private static final String[] PERMISSIONS= {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int PERMISSION_COUNT=2;
    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean notPermission(){
        for(int i=0;i<PERMISSION_COUNT;i++){
            if(checkSelfPermission(PERMISSIONS[i])!=PackageManager.PERMISSION_GRANTED){
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(notPermission()){
                requestPermissions(PERMISSIONS,REQUEST_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==REQUEST_PERMISSIONS && grantResults.length>0){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(notPermission()){
                    ((ActivityManager)this.getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
                    recreate();
                }
            }
        }
    }

    public void onBackPressed(){
        if(editMode){
            imageView.setImageBitmap(bitmap);
            findViewById(R.id.editScreen).setVisibility(View.GONE);
            findViewById(R.id.welcomeScreen).setVisibility(View.VISIBLE);
            editMode=false;

        }
        else{
            super.onBackPressed();
        }
    }

    private static final int REQUEST_PICK_IMAGE=12345;
    private ImageView imageView;
    private TextView heading;
    private Stack<Bitmap>stk;
    private Bitmap bitmap;
    private void init(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
            StrictMode.VmPolicy.Builder builder=new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }
        heading=findViewById(R.id.header_title);
        imageView=findViewById(R.id.imageView);
        if(bitmap==null)
            imageView.setImageDrawable(getResources().getDrawable(R.drawable.gradient));
        stk=new Stack<>();
        if(!MainActivity.this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            findViewById(R.id.takePic).setVisibility(View.GONE);
        }
        final Button selectImageButton=findViewById(R.id.selectPic);
        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                final Intent pickIntent=new Intent(Intent.ACTION_PICK);
                pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
                final Intent chooseIntent=Intent.createChooser(intent,"Select Image");
                startActivityForResult(chooseIntent,REQUEST_PICK_IMAGE);
            }
        });
        final Button takePhotoButton=findViewById(R.id.takePic);
        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent takePictureIntent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
              //  takePictureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
                takePictureIntent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
                takePictureIntent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
             //   if(takePictureIntent.resolveActivity(getPackageManager())!=null){
                    //create file for the photo
                try{
                    final File photoFile= createImageFile();
                    imageUri=Uri.fromFile(photoFile);
                    final SharedPreferences myPrefs=getSharedPreferences(appID,0);
                    myPrefs.edit().putString("path", photoFile.getAbsolutePath()).apply();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(takePictureIntent,REQUEST_IMAGE_CAPTURE);

                }
                catch (Exception e){
                    e.printStackTrace();
                }

           //     }
           //     else{
           //         Toast.makeText(MainActivity.this, "Camera app is not compatible", Toast.LENGTH_SHORT).show();
           //     }
            }
        });
        final Button rotateRightButtton=findViewById(R.id.rotateRgt);
        rotateRightButtton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(){
                    public  void run(){
                     //  for(int i=0;i<pixelCount;i++){
                 //           pixels[i]/=2;
                //        }
                        android.graphics.Matrix matrix= new android.graphics.Matrix();
                        matrix.postScale((float)1,(float)1);
                        matrix.postRotate(90);
                        stk.push(bitmap);
                        bitmap= Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                }.start();
            }
        });

        final Button rotateleftButtton=findViewById(R.id.rotatelft);
        rotateleftButtton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(){
                    public  void run(){
                        //  for(int i=0;i<pixelCount;i++){
                        //           pixels[i]/=2;
                        //        }
                        android.graphics.Matrix matrix= new android.graphics.Matrix();
                        matrix.postScale((float)1,(float)1);
                        matrix.postRotate(270);
                        stk.push(bitmap);
                        bitmap= Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                }.start();
            }
        });
        final Button undoButton=findViewById(R.id.undoButton);
        undoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(){
                    public  void run(){
                        //  for(int i=0;i<pixelCount;i++){
                        //           pixels[i]/=2;
                        //        }
                        Matrix matrix= new Matrix();
                        matrix.postScale((float)1,(float)1);
                        matrix.postRotate(270);
                        if(!stk.empty()){
                            bitmap= stk.peek();
                            stk.pop();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    imageView.setImageBitmap(bitmap);
                                }
                            });
                        }
                    }
                }.start();
            }
        });
        final Button saveButton=findViewById(R.id.saveImg);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
                final DialogInterface.OnClickListener dialogClickListner= new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(i==DialogInterface.BUTTON_POSITIVE){
                            final File outFile=createImageFile();
                            try {
                                FileOutputStream out=new FileOutputStream(outFile);
                                bitmap.compress(Bitmap.CompressFormat.JPEG,100,out);
                                imageUri=Uri.parse("file://"+outFile.getAbsolutePath());
                                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,imageUri));
                                Toast.makeText(MainActivity.this, "The Image was saved in "+ imageUri, Toast.LENGTH_SHORT).show();
                            }catch (Exception e){
                                e.printStackTrace();
                            }

                        }
                    }
                };
                builder.setMessage("Save current photo in Gallery ?").setPositiveButton("Yes",dialogClickListner).setNegativeButton("No",dialogClickListner).show();
            }
        });
        final Button backButton=findViewById(R.id.back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.editScreen).setVisibility(View.GONE);
                findViewById(R.id.welcomeScreen).setVisibility(View.VISIBLE);
                heading.setText("My Image Preview");
                imageView.setImageBitmap(bitmap);
                editMode=false;
            }
        });
        final Button cropButton=findViewById(R.id.cropImg);
        cropButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

    }

    private static final int REQUEST_IMAGE_CROP=1122;
    private static final int REQUEST_IMAGE_CAPTURE=1010;
    private static final String appID="photoEditor";
    private Uri imageUri, outputUri ;
    private File createImageFile(){
        final String timeStamp=new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final String imageFileName="/JPEG_"+timeStamp+".jpg";
        final File storageDir= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        return new File(storageDir+imageFileName);
    }



    private boolean editMode= false;

    private int height=0;
    private int width=0;
    private static final int MAX_PIXEL_COUNT=2048;
    private int[] pixels;
    private int pixelCount=0;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode!=RESULT_OK){
            return;
        }


        if(requestCode==REQUEST_IMAGE_CAPTURE){
            if(imageUri==null){
                final SharedPreferences preferences= getSharedPreferences(appID,0);
                final String path=preferences.getString("path","");
                if(path.length()<1){
                    recreate();
                    return;
                }
                imageUri=Uri.parse("file://"+path);
            }
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,imageUri));
        }
        else if(data==null){
            recreate();
            return;
        }
        else if(requestCode==REQUEST_PICK_IMAGE){
            imageUri=data.getData();
        }
        final ProgressDialog dialog=ProgressDialog.show(MainActivity.this,"Loading", "Please Wait", true);
        editMode=true;
        findViewById(R.id.welcomeScreen).setVisibility(View.GONE);
        findViewById(R.id.editScreen).setVisibility(View.VISIBLE);

        heading.setText("My Image Editor");
        new Thread(){
            public void run(){
                bitmap=null;
                final BitmapFactory.Options bmpOptions= new BitmapFactory.Options();
                bmpOptions.inBitmap=bitmap;
                bmpOptions.inJustDecodeBounds=true;
                try(InputStream input=getContentResolver().openInputStream(imageUri)){
                    bitmap=BitmapFactory.decodeStream(input,null,bmpOptions);
                }
                catch(IOException e){
                    e.printStackTrace();
                }
                bmpOptions.inJustDecodeBounds=false;
                width=bmpOptions.outWidth;
                height=bmpOptions.outHeight;
                int resizeScale=1;
                if(width>MAX_PIXEL_COUNT){
                    resizeScale=width/MAX_PIXEL_COUNT;
                }
                else if(height>MAX_PIXEL_COUNT){
                    resizeScale=height/MAX_PIXEL_COUNT;
                }
                if(width/resizeScale>MAX_PIXEL_COUNT || height/resizeScale>MAX_PIXEL_COUNT){
                    resizeScale++;
                }
                bmpOptions.inSampleSize=resizeScale;
                InputStream input=null;
                try{
                    input=getContentResolver().openInputStream(imageUri);
                }
                catch(FileNotFoundException e){
                    e.printStackTrace();
                    recreate();
                    return;
                }
                bitmap=BitmapFactory.decodeStream(input,null, bmpOptions);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bitmap);
                        dialog.cancel();
                    }
                });
                width=bitmap.getWidth();
                height=bitmap.getHeight();
                bitmap=bitmap.copy(Bitmap.Config.ARGB_8888, true);

                pixelCount=width*height;
                pixels=new int[pixelCount];
                bitmap.getPixels(pixels,0,width,0,0,width,height);
            }
        }.start();

    }
}