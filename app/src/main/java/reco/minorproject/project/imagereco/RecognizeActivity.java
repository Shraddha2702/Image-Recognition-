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
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.gson.Gson;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.LanguageCodes;
import com.microsoft.projectoxford.vision.contract.Line;
import com.microsoft.projectoxford.vision.contract.OCR;
import com.microsoft.projectoxford.vision.contract.Region;
import com.microsoft.projectoxford.vision.contract.Word;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;
//import com.vivek.vizaid.vizaid.helper.ImageHelper;
//import com.vivek.vizaid.vizaid.helper.SelectImageActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;


public class RecognizeActivity extends Activity {

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

    private int flag = 0;
    private TextToSpeech t1;

    TextRecognizer detector;

    ImageView share, favs;
    private Uri imageUri;
    String audiotext;
    Session session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                    String toSpeak = "Text Read Mode";
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
            setContentView(R.layout.activity_recognize);

            favs = (ImageView) findViewById(R.id.favs);
            share = (ImageView) findViewById(R.id.share);
            mEditText = (TextView) findViewById(R.id.editTextResult);
            session = new Session(getApplicationContext());
        }



    }


    // Called when the "Select Image" button is clicked.
    public void selectImage() {
        Intent intent;
        intent = new Intent(RecognizeActivity.this, SelectImageActivity.class);
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }

    // Called when image selection is done.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        setContentView(R.layout.activity_describe);
        //mButtonSelectImage = (Button)findViewById(R.id.buttonSelectImage);
        mEditText = (TextView) findViewById(R.id.editTextResult);
        Log.d("AnalyzeActivity", "onActivityResult");
        switch (requestCode) {
            case REQUEST_SELECT_IMAGE:
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

                        // Add detection log.
                        Log.d("AnalyzeActivity", "Image: " + mImageUri + " resized to " + mBitmap.getWidth()
                                + "x" + mBitmap.getHeight());

                        doRecognize();
                    }
                }
                break;
            default:
                break;
        }
    }

    public void doRecognize() {
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
        //mButtonSelectImage.setEnabled(false);
        //mEditText.setText("Analyzing...");

        try {

            detector = new TextRecognizer.Builder(this).build();
            if (detector.isOperational() && mBitmap != null) {
                Log.d("Detect now", "detecting");

                Frame frame = new Frame.Builder().setBitmap(mBitmap).build();
                SparseArray<TextBlock> textBlocks = detector.detect(frame);
                String blocks = "";
                String lines = "";
                String words = "";

                for (int index = 0; index < textBlocks.size(); index++) {
                    Log.d("Detecting", "Inside for Loop");
                    //extract scanned text blocks here
                    TextBlock tBlock = textBlocks.valueAt(index);
                    blocks = blocks + tBlock.getValue() + "\n" + "\n";
                    for (Text line : tBlock.getComponents()) {
                        //extract scanned text lines here
                        lines = lines + line.getValue() + "\n";
                        for (Text element : line.getComponents()) {
                            //extract scanned text words here
                            words = words + element.getValue() + ", ";
                        }

                        audiotext = lines;
                    }
                }
                if (textBlocks.size() == 0) {
                    Log.d("Scan ", "Failed");
                    mEditText.setText("Scan Failed: Found nothing to scan");
                } else {
                    Log.d("Here ", "on Scan Done");
                    //scanResults.setText(scanResults.getText() + "Blocks:" + "\n");
                    mEditText.setText(mEditText.getText() + blocks + "\n");
                }

                t1 =new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener()
                {
                    @Override
                    public void onInit(int status){
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
                                    //do some work here

                                    //Intent in = new Intent();
                                    //setResult(0, in);
                                    //finish();

                                    boolean know = session.isBlind();
                                    if (know == true) {
                                        Log.d("know",""+know);
                                        Intent in = new Intent();
                                        setResult(0, in);
                                        finish();
                                    } else {
                                        //nothing stay
                                        //SetonClickListener

                                        Log.d("know",""+know);
                                            share.setEnabled(true);
                                            favs.setEnabled(true);

                                        ////////////////////////-----------------Code  for  Listeners

                                        share.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                //Share Code --------------------------------------------------------------
                                                // custom dialog
                                                final Dialog dialog = new Dialog(getApplicationContext());
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
                                                            if (imageUri != null) {
                                                                emailIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                                                            }
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
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                });
            }

        } catch (Exception e) {
            mEditText.setText("Error encountered. Exception is: " + e.toString());
        }

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////



            /*share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Share Code --------------------------------------------------------------
                    // custom dialog
                    final Dialog dialog = new Dialog(getApplicationContext());
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
                                if (imageUri != null) {
                                    emailIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                                }
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

                }
            });*/
        }



    // =new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener()

    /*private String process() throws VisionServiceException, IOException {
        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());


        return "";
    }*/

    /*private class doRequest extends AsyncTask<String, String, String> {
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
        }*/

       /* @Override
        protected void onPostExecute(String data) {
            super.onPostExecute(data);
            // Display based on error existence
            mEditText.setText("");
            if (e != null) {
                mEditText.setText("Error: " + e.getMessage());
                this.e = null;
            } else {
                Gson gson = new Gson();
                OCR r = gson.fromJson(data, OCR.class);

                String result = "";
                for (Region reg : r.regions) {
                    for (Line line : reg.lines) {
                        for (Word word : line.words) {
                            result += word.text + " ";
                        }
                        result += "\n";
                    }
                    result += "\n\n";
                }

                mEditText.setText(result);
    t1 =new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener()
    {
        @Override
        public void onInit(int status){
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
                    //do some work here
                    Intent in = new Intent();
                    setResult(0, in);
                    finish();
                }
            });
        }
    }
    });*/

    //mButtonSelectImage.setEnabled(true);}

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
}
