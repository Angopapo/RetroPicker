package br.com.packapps.retropicker.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.app.Fragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import br.com.packapps.retropicker.Util.Const;
import br.com.packapps.retropicker.callback.CallbackPicker;
import br.com.packapps.retropicker.config.Retropicker;

import static android.content.ContentValues.TAG;

/**
 * @Author Paulo linhares 20/02/2018
 */
public class RetroPickerFragment extends Fragment {

    private static final int REQUEST_TAKE_PHOTO = 100;
    private static final int REQUEST_OPEN_GALLERY = 101;

    private static final String ARG_PARAM2 = "param2";


    private int actionType;
    private String mParam2;

    private String mCurrentPhotoPath;
    private CallbackPicker callbackPicker;
    private Activity activity;

    public RetroPickerFragment() {
        // Required empty public constructor
    }


    public static RetroPickerFragment newInstance(int type_action, String param2) {
        RetroPickerFragment fragment = new RetroPickerFragment();
        Bundle args = new Bundle();
        args.putInt(Const.Params.TYPE_ACTION, type_action);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            actionType = getArguments().getInt(Const.Params.TYPE_ACTION);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        //### axecute action
        executeAction();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d("TAG", "onAttach");
        this.activity = (Activity) context;
    }

    private void executeAction() {
        switch (actionType){
            case Retropicker.CAMERA_PICKER:
                callCameraIntent();
                break;
            case Retropicker.GALLERY_PICKER:
                openGallery();
                break;
            //TODO gallery
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        startActivityForResult(intent, REQUEST_OPEN_GALLERY);
    }

    //open intent at camera
    private void callCameraIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(activity, "Erro ao abrir a câmera. Por favor tente novamente.", Toast.LENGTH_SHORT).show();
                mCurrentPhotoPath = null;
                ex.printStackTrace();

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(activity,
                        activity.getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    //create file_paths for used with URI
    private File createImageFile() throws IOException {
        // Create an image file_paths name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file_paths: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }


    private Bitmap getPicBitmap() throws IOException {

	    /* Get the size of the ImageView */
//        int targetW = ivPhotoDoc.getWidth();
//        int targetH = ivPhotoDoc.getHeight();
//
        int targetW = 520;
        int targetH = 520;

		/* Get the size of the image */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

		/* Figure out which way needs to be reduced less */
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        }

		/* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);


        return bitmap;

    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                activity.getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //###Camera
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == activity.RESULT_OK){
            Log.d(TAG, "result from Camera");
            //getting photo for mCurrentImage
            //set image pic
            Bitmap bitmap = null;
            try {
                bitmap = getPicBitmap();
                callbackPicker.onSuccess(bitmap, mCurrentPhotoPath);
            } catch (IOException e) {
                callbackPicker.onFailure(e);
                e.printStackTrace();
            }


        }else if (requestCode == REQUEST_OPEN_GALLERY && resultCode == activity.RESULT_OK){
            Log.d(TAG, "result from Gallery");

            Uri uri = null;
            if (data != null) {
                uri = data.getData();
                Log.i("TAG", "Uri: " + uri.toString());
                mCurrentPhotoPath = uri.toString();
                try {
                    Bitmap bitmap = getBitmapFromUri(uri);
                    callbackPicker.onSuccess(bitmap, mCurrentPhotoPath);
                } catch (IOException e) {
                    Log.e(TAG, "error getting bitmap from image comming gallery" );
                    callbackPicker.onFailure(e);
                    e.printStackTrace();
                }


            }
        }
    }

    public void setCallBack(CallbackPicker callbackPicker) {
        this.callbackPicker = callbackPicker;
    }

}
