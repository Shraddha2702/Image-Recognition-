/**
 * Created by Vivek on 30-01-2017.
 */
package reco.minorproject.project.imagereco;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.text.Text;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;

//import com.vivek.vizaid.vizaid.helper.ImageHelper;
//import com.vivek.vizaid.vizaid.helper.SelectImageActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class EmotionActivity extends Activity {

    // Flag to indicate which task is to be performed.
    private static final int REQUEST_SELECT_IMAGE = 0;


    // The button to select an image
    private Button mButtonSelectImage;

    // The URI of the image selected to detect.
    private Uri mImageUri;

    // The image selected to detect.
    private Bitmap mBitmap;

    // The edit to show status and result.
    private TextView mEditText;

    private EmotionServiceClient client;

    private TextToSpeech t1;
    private EditText ed1;
    private int flag = 0;

    ImageView share, favs;
    private Uri imageUri;
    String audiotext;

    Session session;

    // Folder path for Firebase Storage.
    String Storage_Path = "All_Image_Uploads/";

    // Root Database Name for Firebase Database.
    public static final String Database_Path = "All_Image_Uploads_Database";

    // Creating StorageReference and DatabaseReference object.
    StorageReference storageReference;
    DatabaseReference databaseReference;

    ProgressDialog progressDialog ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognize);

        session = new Session(getApplicationContext());

        if (client == null) {
            client = new EmotionServiceRestClient(getString(R.string.esubscription_key));
        }
        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                    String toSpeak = "Recognize Emotion Mode";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        String utteranceId = this.hashCode() + "";
                        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                    } else {
                        HashMap<String, String> map = new HashMap<>();
                        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
                        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, map);
                    }
                }
            }
        });
        if (flag == 0) {
            flag = 1;
            selectImage();
        } else {
            setContentView(R.layout.activity_emotion);
            mEditText = (TextView) findViewById(R.id.editTextResult);
            favs = (ImageView) findViewById(R.id.favs);
            share = (ImageView) findViewById(R.id.share);

            boolean blind = session.isBlind();
            if (blind == false) {
                Log.d("blind",""+blind);
                favs.setEnabled(true);
                share.setEnabled(true);

                // Assign FirebaseStorage instance to storageReference.
                storageReference = FirebaseStorage.getInstance().getReference();

                // Assign FirebaseDatabase instance with root database name.
                databaseReference = FirebaseDatabase.getInstance().getReference(Database_Path);
            }


        }

    }


    public void doRecognize() {
        mEditText.setText("\n\nRecognizing emotions ...\n");
        /*t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                    String toSpeak = "Analyzing";
                    String utteranceId=this.hashCode() + "";
                    t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                }
            }
        });*/
        // Do emotion detection using auto-detected faces.
        try {
            new doRequest().execute();
        } catch (Exception e) {
            mEditText.append("Error encountered. Exception is: " + e.toString());
        }


    }

    // Called when the "Select Image" button is clicked.
    public void selectImage() {
        //mEditText.setText("");
        Intent intent;
        intent = new Intent(EmotionActivity.this, SelectImageActivity.class);
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }

    // Called when image selection is done.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        setContentView(R.layout.activity_emotion);
        mEditText = (TextView) findViewById(R.id.editTextResult);
        Log.d("RecognizeActivity", "onActivityResult");
        switch (requestCode) {
            case REQUEST_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    // If image is selected successfully, set the image URI and bitmap.
                    mImageUri = data.getData();

                    Log.d("mImageUri", "" + mImageUri);
                    if (mImageUri == null) {
                        Intent in = new Intent();
                        setResult(0, in);
                        finish();
                    }

                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(mImageUri, getContentResolver());
                    Log.d("ContentResolver", "" + getContentResolver());
                    Log.d("Bitmap", "" + mBitmap);

                    if (mBitmap != null) {
                        // Show the image on screen.
                        ImageView imageView = (ImageView) findViewById(R.id.selectedImage);
                        imageView.setImageBitmap(mBitmap);

                        // Add detection log.
                        Log.d("RecognizeActivity", "Image: " + mImageUri + " resized to " + mBitmap.getWidth()
                                + "x" + mBitmap.getHeight());

                        doRecognize();
                    }
                }
                break;
            default:
                break;
        }
    }


    private List<RecognizeResult> processWithAutoFaceDetection() throws EmotionServiceException, IOException {
        Log.d("emotion", "Start emotion detection with auto-face detection");

        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        long startTime = System.currentTimeMillis();
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE STARTS HERE
        // -----------------------------------------------------------------------

        List<RecognizeResult> result = null;
        //
        // Detect emotion by auto-detecting faces in the image.
        //
        result = this.client.recognizeImage(inputStream);

        String json = gson.toJson(result);
        Log.d("result", json);

        Log.d("emotion", String.format("Detection done. Elapsed time: %d ms", (System.currentTimeMillis() - startTime)));
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE ENDS HERE
        // -----------------------------------------------------------------------
        return result;
    }


    private class doRequest extends AsyncTask<String, String, List<RecognizeResult>> {
        // Store error message
        private Exception e = null;

        @Override
        protected List<RecognizeResult> doInBackground(String... args) {
            try {
                return processWithAutoFaceDetection();
            } catch (Exception e) {
                this.e = e;    // Store error
            }
            return null;
        }

        private int count = 0;

        @Override
        protected void onPostExecute(List<RecognizeResult> result) {
            super.onPostExecute(result);
            // Display based on error existence
            String maxs = "";
            int max = 0;

            mEditText.setText("");

            if (e != null) {
                mEditText.setText("Error: " + e.getMessage());
                this.e = null;
            } else {
                if (result.size() == 0) {
                    mEditText.setText("No emotion detected.");
                } else {

                    // Covert bitmap to a mutable bitmap by copying it
                    Bitmap bitmapCopy = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    final Canvas faceCanvas = new Canvas(bitmapCopy);
                    faceCanvas.drawBitmap(mBitmap, 0, 0, null);
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(5);
                    paint.setColor(Color.RED);
                    count = 0;
                    for (RecognizeResult r : result) {
                        maxs = "";
                        max = 0;
                        mEditText.append(String.format("\nPerson #%1$d \n", count + 1));
                        if ((int) (r.scores.anger * 100) > max) {
                            max = (int) (r.scores.anger * 100);
                            maxs = "is angry.";
                        }
                        if ((int) (r.scores.contempt * 100) > max) {
                            max = (int) (r.scores.contempt * 100);
                            maxs = "is contempt.";
                        }
                        if ((int) (r.scores.disgust * 100) > max) {
                            max = (int) (r.scores.disgust * 100);
                            maxs = "is disgusted.";
                        }
                        if ((int) (r.scores.fear * 100) > max) {
                            max = (int) (r.scores.fear * 100);
                            maxs = "is scared.";
                        }
                        if ((int) (r.scores.happiness * 100) > max) {
                            max = (int) (r.scores.happiness * 100);
                            maxs = "is happy.";
                        }
                        if ((int) (r.scores.neutral * 100) > max) {
                            max = (int) (r.scores.neutral * 100);
                            maxs = "is neutral.";
                        }
                        if ((int) (r.scores.sadness * 100) > max) {
                            max = (int) (r.scores.sadness * 100);
                            maxs = "is sad.";
                        }
                        if ((int) (r.scores.surprise * 100) > max) {
                            max = (int) (r.scores.surprise * 100);
                            maxs = "is surprised.";
                        }
                        faceCanvas.drawRect(r.faceRectangle.left,
                                r.faceRectangle.top,
                                r.faceRectangle.left + r.faceRectangle.width,
                                r.faceRectangle.top + r.faceRectangle.height,
                                paint);
                        count++;
                        mEditText.append(maxs + '\n');
                        audiotext = maxs;

                    }
                    ImageView imageView = (ImageView) findViewById(R.id.selectedImage);
                    imageView.setImageDrawable(new BitmapDrawable(getResources(), mBitmap));

                    /////////////////////////////////////////////////////////////////////////////////////


                    t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                        @Override
                        public void onInit(int status) {
                            if (status != TextToSpeech.ERROR) {
                                t1.setLanguage(Locale.UK);
                                String toSpeak = mEditText.getText().toString();
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    String utteranceId = this.hashCode() + "";
                                    t1.speak("number of faces" + count + toSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                                } else {
                                    HashMap<String, String> map = new HashMap<>();
                                    map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
                                    t1.speak("number of faces" + count + toSpeak, TextToSpeech.QUEUE_FLUSH, map);
                                }
                                t1.setOnUtteranceProgressListener(new UtteranceProgressListener() {

                                    @Override
                                    public void onStart(String utteranceId) {
                                        // TODO Auto-generated method stub

                                    }

                                    @Override
                                    public void onError(String utteranceId) {
                                        // TODO Auto-generated method stub

                                    }

                                    @Override
                                    public void onDone(String utteranceId) {
                                        //do some work here

                                        //Intent in = new Intent();
                                        //setResult(0, in);
                                        //finish();

                                        boolean know = session.isBlind();
                                        if (know == true) {
                                            Intent in = new Intent();
                                            setResult(0, in);
                                            finish();
                                        } else {
                                            //nothing stay
                                            //SetonClickListener
                                            share = (ImageView)findViewById(R.id.share);
                                            favs = (ImageView) findViewById(R.id.favs);
                                            ////////////////////////-----------------Code  for  Listeners
                                            share.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    //Share Code --------------------------------------------------------------
                                                    // custom dialog
                                                    final Dialog dialog = new Dialog(EmotionActivity.this);
                                                    dialog.setContentView(R.layout.custom_share);
                                                    dialog.setTitle("Share Image ...");

                                                    // set the custom dialog components - text, image and button
                                                    final EditText email = (EditText) dialog.findViewById(R.id.msg);
                                                    Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
                                                    Button cancel = (Button) dialog.findViewById(R.id.dialogButtonCANCEL);
                                                    // if button is clicked, close the custom dialog
                                                    dialogButton.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            String address = email.getText().toString().trim();

                                                            imageUri = getImageUri(getApplicationContext(), mBitmap);

                                                            // CALL THIS METHOD TO GET THE ACTUAL PATH
                                                            //final File file = new File(getRealPathFromURI(imageUri));


                                                            //Intent Code ***************************************************
                                                            //Share Code here -----------------------------

                                                            try {
                                                                final Intent emailIntent = new Intent(
                                                                        android.content.Intent.ACTION_SEND);
                                                                emailIntent.setType("plain/text");

                                                                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                                                                        new String[]{address});
                                                                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                                                                        "Image Share");

                                                                //Image Uri check and send
                                                               /* if (imageUri != null) {
                                                                    Log.d("check share","imageuri not null");
                                                                    emailIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                                                                }*/
                                                                Log.d("check share",audiotext);
                                                                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, audiotext);
                                                                getApplicationContext().startActivity(Intent.createChooser(emailIntent,
                                                                        "Sending email..."));
                                                            } catch (Throwable t) {
                                                                Toast.makeText(getApplicationContext(), "Request failed try again: " + t.toString(),
                                                                        Toast.LENGTH_LONG).show();
                                                            }

                                                            dialog.dismiss();
                                                        }
                                                    });
                                                    cancel.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            dialog.dismiss();
                                                        }
                                                    });

                                                    dialog.show();
                                                }
                                            });

                                            //----------------------------------------------------------Share Code Complete ---------------------

                                            favs.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    ////////////////////////////////////////////////////////////////Add to Favs
                                                    UploadImageFileToFirebaseStorage(imageUri);
                                                }
                                            });

                                        }
                                    }
                                });
                            }
                        }
                    });

                    //mEditText.setSelection(0);
                }

            }
        }
    }

    // Creating Method to get the selected image file Extension from File Path URI.
    public String GetFileExtension(Uri uri) {

        ContentResolver contentResolver = getContentResolver();

        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();

        // Returning the file Extension.
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri)) ;

    }


    public void UploadImageFileToFirebaseStorage(Uri imageUri) {

        // Checking whether FilePathUri Is empty or not.
        if (imageUri != null) {

            Log.d("image Uri",""+imageUri);

           //progressDialog = new ProgressDialog(EmotionActivity.this);
            // Setting progressDialog Title.
            //progressDialog.setTitle("Image is Uploading...");

            // Showing progressDialog.
            //progressDialog.show();

            // Creating second StorageReference.
            StorageReference storageReference2nd = storageReference.child(Storage_Path + System.currentTimeMillis() + "." + GetFileExtension(imageUri));

            // Adding addOnSuccessListener to second StorageReference.
            storageReference2nd.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            // Getting image name from EditText and store into string variable.
                            String TempImageName = mEditText.getText().toString().trim();

                            // Hiding the progressDialog after done uploading.
                            progressDialog.dismiss();

                            // Showing toast message after done uploading.
                            Toast.makeText(getApplicationContext(), "Image Uploaded Successfully ", Toast.LENGTH_LONG).show();

                            @SuppressWarnings("VisibleForTests")
                            ImageUploadInfo imageUploadInfo = new ImageUploadInfo(TempImageName, taskSnapshot.getDownloadUrl().toString());

                            // Getting image upload ID.
                            String ImageUploadId = databaseReference.push().getKey();

                            // Adding image upload id s child element into databaseReference.
                            databaseReference.child(ImageUploadId).setValue(imageUploadInfo);
                        }
                    })
                    // If something goes wrong .
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {

                            // Hiding the progressDialog.
                            //progressDialog.dismiss();

                            // Showing exception erro message.
                            Toast.makeText(EmotionActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })

                    // On progress change upload time.
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                            // Setting progressDialog Title.
                            //progressDialog.setTitle("Image is Uploading...");

                        }
                    });
        }
        else {

            Toast.makeText(EmotionActivity.this, "Please Select Image or Add Image Name", Toast.LENGTH_LONG).show();

        }
    }


    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }

    public void onResume() {
        super.onResume();  // Always call the superclass method firs
    }

    public void onPause() {
        super.onPause();  // Always call the superclass method first
    }

    protected void onStop() {
        // call the superclass method first
        super.onStop();
    }


}
