package pl.polsl.waga

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel
import com.google.firebase.ml.vision.objects.FirebaseVisionObject
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions

import kotlinx.android.synthetic.main.activity_main.*

import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    val REQUEST_IMAGE_CAPTURE = 1
    var mCurrentPhotoPath: String = ""
    private var recognizedText = ""

    var selectedImage: Bitmap? = null
    lateinit var photoURI: Uri
    enum class UserPermission{
        CAMERA,
        WRITE_DATA
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissionsFor(arrayListOf(UserPermission.CAMERA, UserPermission.WRITE_DATA))
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {

            // Once the image is captured, get it from the saved location
            val f = File(mCurrentPhotoPath)
            val contentUri = Uri.fromFile(f)

            if (getBitmapFromUri(contentUri) != null){
                selectedImage = getBitmapFromUri(contentUri)!!
            }
            snapShotView.setImageBitmap(selectedImage)
        }
    }

    fun takePicture(view: View){

        clearLabel()
        dispatchTakePictureIntent()
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            var photoFile:File? = null

            try {
                //TODO: Clean job to clear all the used images
                photoFile = createImageFile()
            }catch (ex: IOException){

            }

            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(
                    this,
                    "pl.polsl.waga",
                    photoFile
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath()
        return image
    }

    private fun getBitmapFromUri(filePath: Uri): Bitmap? {
        var bitmap:Bitmap? = null
        try{
            var tempBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, filePath)
            bitmap = updateImage(tempBitmap)
        }catch (ex: IOException){

        }
        return bitmap
    }

    private fun updateImage(bitmap: Bitmap): Bitmap{

        val isLandScape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        var scaledImageWidth = 0.0
        var scaledImageHeight = 0.0

        when (isLandScape){

            (true)->{
                scaledImageHeight = snapShotView.height.toDouble()
                scaledImageWidth = bitmap.width.toDouble() * scaledImageHeight / bitmap.height.toDouble()
            }
            (false)->{
                scaledImageWidth = snapShotView.width.toDouble()
                scaledImageHeight = bitmap.height.toDouble() * scaledImageWidth / bitmap.width.toDouble()
            }
        }
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap,scaledImageWidth.toInt(),scaledImageHeight.toInt(),true)

        return resizedBitmap
    }



    /***
     *      _           _          _
     *     | |         | |        | |
     *     | |     __ _| |__   ___| |___
     *     | |    / _` | '_ \ / _ | / __|
     *     | |___| (_| | |_) |  __| \__ \
     *     |______\__,_|_.__/ \___|_|___/
     *
     *
     */
    // https://firebase.google.com/docs/ml-kit/android/label-images
    fun decodeImage(view: View){

        decodeImage()
    }

    private fun decodeImage(){
        val img = selectedImage?.let { it } ?: kotlin.run { return }
        val image = FirebaseVisionImage.fromBitmap(img)

        val options = FirebaseVisionObjectDetectorOptions.Builder()
            .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()



        /*   val localModel = LocalModel.Builder()
                   .setAssetFilePath("model.tflite")
                   .build()
           val customObjectDetectorOptions =
                   CustomObjectDetectorOptions.Builder(localModel)
                           .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                           .enableClassification()
                           .setClassificationConfidenceThreshold(0.5f)
                           .setMaxPerObjectLabelCount(3)
                           .build()

*/
        val detector = FirebaseVision.getInstance().getOnDeviceObjectDetector(options)

         detector.processImage(image)
                 .addOnSuccessListener {
                     // Task completed successfully
                     Toast.makeText(baseContext, "Cos jest",
                             Toast.LENGTH_SHORT).show()
                     setValuesToTextView(it)
                 }
                 .addOnFailureListener {
                     // Task failed with an exception
                     Toast.makeText(baseContext, "Oops, something went wrong!",
                             Toast.LENGTH_SHORT).show()
                 }





    }

    private fun setValuesToTextView(visionObjects : List<FirebaseVisionObject>) {
       for ((idx, obj) in visionObjects.withIndex()) {
            val box = obj.boundingBox
            var categoryName :String = ""
            if (obj.classificationCategory != FirebaseVisionObject.CATEGORY_UNKNOWN) {
                val confidence: Int = obj.classificationConfidence!!.times(100).toInt()
                when(obj.classificationCategory)
                {
                    FirebaseVisionObject.CATEGORY_FOOD->   categoryName = "food"
                    FirebaseVisionObject.CATEGORY_PLACE->   categoryName = "place"
                    FirebaseVisionObject.CATEGORY_FASHION_GOOD->   categoryName = "fashion food"
                    FirebaseVisionObject.CATEGORY_HOME_GOOD->   categoryName = "home good"
                    FirebaseVisionObject.CATEGORY_UNKNOWN->   categoryName = "unknown"
                    FirebaseVisionObject.CATEGORY_PLANT->   categoryName = "plant"

                }
                Toast.makeText(baseContext, "Detected object: ${idx}\n" + "Category: ${obj.classificationCategory}\n"
                        + "trackingId: ${obj.trackingId}\n"
                        + "boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})\n"
                        + "Confidence: ${confidence}%\n" + "Category Label is : ${categoryName}"
                    ,
                    Toast.LENGTH_SHORT).show()
                imageLabel.text= "Detected object: ${idx}\n" + "Category: ${obj.classificationCategory}\n" + "trackingId: ${obj.trackingId}\n" + "entityId: ${obj.entityId}\n" + "boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})\n" + "Confidence: ${confidence}%\n" + "Category Label is : ${categoryName}"
            }
        }
    }


    private fun processLabels(labels: List<FirebaseVisionImageLabel>){


        val lbl = labels.firstOrNull()
        var msg = lbl?.text + "," + lbl?.confidence
        updateLabel(msg)
        for (label in labels) {
            val text = label.text
            val entityId = label.entityId
            val confidence = label.confidence

            Log.d("TEXTRECOG",text + entityId + confidence)

        }

    }

    private fun updateLabel(message: String){

        this.imageLabel.text = message
    }
    private fun clearLabel(){

        this.imageLabel.text = ""
    }
    /***
     *      _____                    _         _
     *     |  __ \                  (_)       (_)
     *     | |__) ___ _ __ _ __ ___  _ ___ ___ _  ___  _ __  ___
     *     |  ___/ _ | '__| '_ ` _ \| / __/ __| |/ _ \| '_ \/ __|
     *     | |  |  __| |  | | | | | | \__ \__ | | (_) | | | \__ \
     *     |_|   \___|_|  |_| |_| |_|_|___|___|_|\___/|_| |_|___/
     *
     *
     */

    private fun checkAndRequestPermissionsFor(items: ArrayList<UserPermission>){

        var itemsRequirePermission = ArrayList<UserPermission>()
        for (item in items){

            if (!hasPermissionFor(item)){
                itemsRequirePermission.add(item)
            }
        }
        if (!itemsRequirePermission.isEmpty()){
            requestPermissionFor(itemsRequirePermission)
        }

    }

    private fun hasPermissionFor(item: UserPermission): Boolean{

        var isPermitted = false
        when (item){

            UserPermission.CAMERA ->{

                isPermitted = this.checkSelfPermission(Manifest.permission.CAMERA) === PackageManager.PERMISSION_GRANTED

            }
            UserPermission.WRITE_DATA ->{
                isPermitted = this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }
        return isPermitted
    }
    private fun requestPermissionFor(items: ArrayList<UserPermission>){

        var manisfestInfo = ArrayList<String>()
        for (item in items){

            manisfestInfo.add(getManisfestInfoFor(item))

        }
        val arrayOfPermissionItems = arrayOfNulls<String>(manisfestInfo.size)
        manisfestInfo.toArray(arrayOfPermissionItems)
        this.requestPermissions(arrayOfPermissionItems, 2)

    }

    private fun getManisfestInfoFor(item: UserPermission): String{

        var manifestString = ""
        when (item){

            UserPermission.CAMERA ->{

                manifestString = Manifest.permission.CAMERA
                //this.requestPermissions(arrayOf<String>(Manifest.permission.CAMERA), 1)

            }
            UserPermission.WRITE_DATA ->{
                manifestString = Manifest.permission.WRITE_EXTERNAL_STORAGE
                //this.requestPermissions(arrayOf<String>(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE), 2)
            }
        }
        return manifestString
    }


    private fun showAlert(message: String) {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Recognized Text")
        dialog.setMessage(message)
        dialog.setPositiveButton(" OK ",
            { dialog, id -> dialog.dismiss() })
        dialog.show()

    }
}