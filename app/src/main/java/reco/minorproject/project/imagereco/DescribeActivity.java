/**
 * Created by Vivek on 30-01-2017.
 */
package reco.minorproject.project.imagereco;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.AnalysisResult;
import com.microsoft.projectoxford.vision.contract.Caption;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;
//import com.vivek.vizaid.vizaid.helper.ImageHelper;
//import com.vivek.vizaid.vizaid.helper.SelectImageActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

public class DescribeActivity extends Activity {

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

    private VisionServiceClient client;

    private TextToSpeech t1;
    private EditText ed1;
    private int flag = 0;

    ImageView share, favs;
    private Uri imageUri;
    String audiotext;
    Session session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (client == null) {
            client = new VisionServiceRestClient(getString(R.string.subscription_key));
        }
        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                    String toSpeak = "Describe Activity Mode";
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
            setContentView(R.layout.activity_describe);
            //mButtonSelectImage = (Button)findViewById(R.id.buttonSelectImage);
            mEditText = (TextView) findViewById(R.id.editTextResult);
            favs = (ImageView) findViewById(R.id.favs);
            share = (ImageView) findViewById(R.id.share);

        }
    }


    public void doDescribe() {
        //mButtonSelectImage.setEnabled(false);
        mEditText.setText("Describing...");
        /*t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                    String toSpeak = "Describing";
                    String utteranceId=this.hashCode() + "";
                    t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                }
            }
        });*/
        try {
            new doRequest().execute();
        } catch (Exception e) {
            mEditText.setText("Error encountered. Exception is: " + e.toString());
        }
    }

    // Called when the "Select Image" button is clicked.
    public void selectImage() {
        //mEditText.setText("");
        Intent intent;
        intent = new Intent(DescribeActivity.this, SelectImageActivity.class);
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }

    // Called when image selection is done.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        setContentView(R.layout.activity_describe);
        //mButtonSelectImage = (Button)findViewById(R.id.buttonSelectImage);
        mEditText = (TextView) findViewById(R.id.editTextResult);

        Log.d("DescribeActivity", "onActivityResult");
        switch (requestCode) {
            case REQUEST_SELECT_IMAGE://mButtonSelectImage = (Button)findViewById(R.id.buttonSelectImage);
                mEditText = (TextView) findViewById(R.id.editTextResult);

                if (resultCode == RESULT_OK) {
                    // If image is selected successfully, set the image URI and bitmap.
                    mImageUri = data.getData();
                    if (mImageUri == null) {
                        Intent in = new Intent();
                        setResult(0, in);
                        finish();
                    }
                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                            mImageUri, getContentResolver());
                    if (mBitmap != null) {
                        // Show the image on screen.
                        ImageView imageView = (ImageView) findViewById(R.id.selectedImage);
                        imageView.setImageBitmap(mBitmap);
                        Log.d("DescribeActivity", "Image: " + mImageUri + " resized to " + mBitmap.getWidth()
                                + "x" + mBitmap.getHeight());
                        doDescribe();
                    }
                }
                break;
            default:
                break;
        }
    }


    private String process() throws VisionServiceException, IOException {
        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        AnalysisResult v = this.client.describe(inputStream, 1);

        String result = gson.toJson(v);
        Log.d("result", result);

        return result;
    }

    private class doRequest extends AsyncTask<String, String, String> {
        // Store error message
        private Exception e = null;

        public doRequest() {
        }

        @Override
        protected String doInBackground(String... args) {
            try {
                return process();
            } catch (Exception e) {
                this.e = e;    // Store error
            }

            return null;
        }


        @Override
        protected void onPostExecute(String data) {
            super.onPostExecute(data);
            // Display based on error existence

            mEditText.setText("");
            if (e != null) {
                mEditText.setText("Error: " + e.getMessage());
                this.e = null;
            } else {
                Gson gson = new Gson();
                AnalysisResult result = gson.fromJson(data, AnalysisResult.class);

                /*mEditText.append("Image format: " + result.metadata.format + "\n");
                mEditText.append("Image width: " + result.metadata.width + ", height:" + result.metadata.height + "\n");
                mEditText.append("\n");*/

                for (Caption caption : result.description.captions) {
                    mEditText.append(caption.text + ". Accuracy " + (int) (caption.confidence * 100) + "%.");
                }
                /*mEditText.append("\n");

                for (String tag: result.description.tags) {
                    mEditText.append("Tag: " + tag + "\n");
                }
                mEditText.append("\n");

                //mEditText.append("\n--- Raw Data ---\n\n");
                /mEditText.append(data); */

                //ed1 = (EditText)result.description.captions.text;
                t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status != TextToSpeech.ERROR) {
                            t1.setLanguage(Locale.UK);
                            String toSpeak = mEditText.getText().toString();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                String utteranceId = this.hashCode() + "";
                                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                            } else {
                                HashMap<String, String> map = new HashMap<>();
                                map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
                                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, map);
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
                                }
                            });
                        }
                    }
                });
            }
        }
        //mButtonSelectImage.setEnabled(true);


        //}//
        /*public void onResume() {
            super.onResume();  // Always call the superclass method firs
        }

        public void onPause() {
            super.onPause();  // Always call the superclass method first
        }

        protected void onStop() {
            // call the superclass method first
            super.onStop();
        }*/

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

    }}