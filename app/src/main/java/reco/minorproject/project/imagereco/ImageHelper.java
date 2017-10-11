//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license.
//
// Microsoft Cognitive Services (formerly Project Oxford): https://www.microsoft.com/cognitive-services
//
// Microsoft Cognitive Services (formerly Project Oxford) GitHub:
// https://github.com/Microsoft/Cognitive-Vision-Android
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
package reco.minorproject.project.imagereco;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.StringReader;

/**
 * Defined several functions to load, draw, save, resize, and rotate images.
 */
public class ImageHelper {

    // The maximum side length of the image to detect, to keep the size of image less than 4MB.
    // Resize the image if its side length is larger than the maximum.
    private static final int IMAGE_MAX_SIDE_LENGTH = 1280;

    // Ratio to scale a detected face rectangle, the face rectangle scaled up looks more natural.
    private static final double FACE_RECT_SCALE_RATIO = 1.3;

    // Decode image from imageUri, and resize according to the expectedMaxImageSideLength
    // If expectedMaxImageSideLength is
    //     (1) less than or equal to 0,
    //     (2) more than the actual max size length of the bitmap
    //     then return the original bitmap
    // Else, return the scaled bitmap
    public static Bitmap loadSizeLimitedBitmapFromUri(
            Uri imageUri,
            ContentResolver contentResolver) {
        try {
            // Load the image into InputStream.
            InputStream imageInputStream = contentResolver.openInputStream(imageUri);

            Log.d("ImageHelper","74");
            // For saving memory, only decode the image meta and get the side length.
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            Rect outPadding = new Rect();
            Log.d("ImageHelper","79");
            BitmapFactory.decodeStream(imageInputStream, outPadding, options);

            // Calculate shrink rate when loading the image into memory.
            int maxSideLength =
                    options.outWidth > options.outHeight ? options.outWidth : options.outHeight;
            options.inSampleSize = 1;

            Log.d("ImageHelper","87");
            options.inSampleSize = calculateSampleSize(maxSideLength, IMAGE_MAX_SIDE_LENGTH);
            options.inJustDecodeBounds = false;
            imageInputStream.close();

            Log.d("ImageHelper","92");
            // Load the bitmap and resize it to the expected size length
            imageInputStream = contentResolver.openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(imageInputStream, outPadding, options);
            maxSideLength = bitmap.getWidth() > bitmap.getHeight()
                    ? bitmap.getWidth(): bitmap.getHeight();

            Log.d("ImageHelper","99");
            double ratio = IMAGE_MAX_SIDE_LENGTH / (double) maxSideLength;
            Log.d("ratio",""+ratio);
            if (ratio < 1) {
                bitmap = Bitmap.createScaledBitmap(
                        bitmap,
                        (int)(bitmap.getWidth() * ratio),
                        (int)(bitmap.getHeight() * ratio),
                        false);
            }
            Log.d("bitmapih",""+bitmap);

            Log.d("ImageHelper","109");
            return rotateBitmap(bitmap, getImageRotationAngle(imageUri, contentResolver));

        } catch (Exception e) {
            return null;
        }
    }

    // Return the number of times for the image to shrink when loading it into memory.
    // The SampleSize can only be a final value based on powers of 2.
    private static int calculateSampleSize(int maxSideLength, int expectedMaxImageSideLength) {
        int inSampleSize = 1;
        Log.d("ImageHelper","121");
        while (maxSideLength > 2 * expectedMaxImageSideLength) {
            maxSideLength /= 2;
            inSampleSize *= 2;
        }

        Log.d("inSampleSize",""+inSampleSize);
        return inSampleSize;
    }

    // Get the rotation angle of the image taken.
    private static int getImageRotationAngle(Uri imageUri, ContentResolver contentResolver) throws IOException {
        int angle = 0;
        Cursor cursor = contentResolver.query(imageUri,
                new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);
        Log.d("ImageHelper","136");
        Log.d("Cursor",""+ cursor);
        if (cursor != null) {
            if (cursor.getCount() == 1) {
                Log.d("Cursor is 1","yes");
                angle = 0;

                //------------------------------------------------------------Commented-----------------------------
                //cursor.moveToFirst();
                //Log.d("Cursor move to first",""+cursor.moveToFirst());
                //angle = cursor.getInt(0);
                //Log.d("Cursor angle",""+angle);
                /*ExifInterface exif = new ExifInterface(imageUri.getPath());
                Log.d("Check exif",""+exif);
                int orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                Log.d("Check Orientation",""+orientation);
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        angle = 270;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        angle = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        angle = 90;
                        break;
                    default:
                        break;}
                Log.d("Check angle",""+angle);*/

            }
            cursor.close();
        } else {
            Log.d("ImageHelper","144");
            ExifInterface exif = new ExifInterface(imageUri.getPath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    angle = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    angle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    angle = 90;
                    break;
                default:
                    break;
            }
        }
        Log.d("angle",""+angle);
        return angle;
    }

    // Rotate the original bitmap according to the given orientation angle
    private static Bitmap rotateBitmap(Bitmap bitmap, int angle) {
        // If the rotate angle is 0, then return the original image, else return the rotated image
        if (angle != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            Log.d("matrix",""+matrix);
            Log.d("ImageHelper","172");
            return Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } else {
            Log.d("bitmap",""+bitmap);
            return bitmap;
        }
    }
}
