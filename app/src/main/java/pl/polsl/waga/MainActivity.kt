package pl.polsl.waga

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel
import com.google.firebase.ml.vision.objects.FirebaseVisionObject
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import pl.polsl.waga.ml.FoodModel
import pl.polsl.waga.ml.Owoce
import java.io.*
import java.nio.ByteBuffer
import java.util.*
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    val REQUEST_IMAGE_CAPTURE = 1
    var mCurrentPhotoPath: String = ""
    private var recognizedText = ""
    lateinit var detector: ObjectDetector
    // private var interpreter: Interpreter? = null
    private lateinit var foodModel: FoodModel



    var selectedImage: Bitmap? = null
    lateinit var photoURI: Uri
    enum class UserPermission{
        CAMERA,
        WRITE_DATA
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        checkAndRequestPermissionsFor(arrayListOf(UserPermission.CAMERA, UserPermission.WRITE_DATA))
//    }
//
//    override fun onResume() {
//        super.onResume()
//    }
//
//    override fun onPause() {
//        super.onPause()
//    }






    private var count = 0

    private var textureView: TextureView? = null

    companion object {
        private const val TAG = "AndroidCameraApi"
        private val ORIENTATIONS = SparseIntArray()
        private const val REQUEST_CAMERA_PERMISSION = 200

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
    }

    lateinit var cameraId: String
    protected var cameraDevice: CameraDevice? = null
    protected var cameraCaptureSessions: CameraCaptureSession? = null
    protected var captureRequestBuilder: CaptureRequest.Builder? = null
    private var imageDimension: Size? = null
    private var imageReader: ImageReader? = null
    private val file: File? = null
    private var mBackgroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null
    private data class referenceBool (var value: Boolean)
    private var isProcessing : referenceBool = referenceBool(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textureView = findViewById<View>(R.id.texture) as TextureView
        assert(textureView != null)
        textureView!!.surfaceTextureListener = textureListener

         foodModel = FoodModel.newInstance(this)
      /*  val options: ObjectDetector.ObjectDetectorOptions =
            ObjectDetector.ObjectDetectorOptions.builder().setMaxResults(1).build()
        Log.e("aa", "222222222")

        detector = ObjectDetector.createFromFileAndOptions(this,
                "FoodModel.tflite", options)*/

        //takePictureButton = findViewById<View>(R.id.btn_takepicture) as Button
        //assert(takePictureButton != null)
        //takePictureButton!!.setOnClickListener { takePicture() }



    }



    var textureListener: TextureView.SurfaceTextureListener = object :
        TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            //open your camera here
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            // Transform you image captured size according to the surface width and height
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
            // Start changes
            // Get the bitmap



            // Do whatever you like with the frame
            //frameProcessor?.processFrame(frame)
            count++;

            if(isProcessing.value == false) {
                isProcessing.value = true
                thread {
                    val frame = Bitmap.createBitmap(textureView!!.width, textureView!!.height, Bitmap.Config.ARGB_8888)
                    textureView?.getBitmap(frame)
                    decodeImage(frame,isProcessing)
                }


                //val promise = launch(CommonPool1) {
                //    asyncDecode(frame,isProcessing)
                //}
            }
            // End changes
        }
    }

    suspend private fun asyncDecode(frame: Bitmap, isProcessing: referenceBool) {
        //decodeImage(frame)
        isProcessing.value = false
    }


    private val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened")
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice!!.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice!!.close()
            cameraDevice = null
        }
    }

    protected fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("Camera Background")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    protected fun stopBackgroundThread() {
        mBackgroundThread!!.quitSafely()
        try {
            mBackgroundThread!!.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    protected fun createCameraPreview() {
        try {
            val texture = textureView!!.surfaceTexture!!
            texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
            val surface = Surface(texture)
            captureRequestBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder!!.addTarget(surface)
            cameraDevice!!.createCaptureSession(
                Arrays.asList(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        //The camera is already closed
                        if (null == cameraDevice) {
                            return
                        }
                        // When the session is ready, we start displaying the preview.
                        cameraCaptureSessions = cameraCaptureSession
                        updatePreview()
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        Toast.makeText(
                            this@MainActivity,
                            "Configuration change",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun openCamera() {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        Log.e(TAG, "is camera open")
        try {
            cameraId = manager.cameraIdList[0]
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CAMERA_PERMISSION
                )
                return
            }
            manager.openCamera(cameraId, stateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        Log.e(TAG, "openCamera X")
    }

    protected fun updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return")
        }
        captureRequestBuilder!!.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSessions!!.setRepeatingRequest(
                captureRequestBuilder!!.build(),
                null,
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun closeCamera() {
        if (null != cameraDevice) {
            cameraDevice!!.close()
            cameraDevice = null
        }
        if (null != imageReader) {
            imageReader!!.close()
            imageReader = null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(
                    this@MainActivity,
                    "Sorry!!!, you can't use this app without granting permission",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (textureView!!.isAvailable) {
            openCamera()
        } else {
            textureView!!.surfaceTextureListener = textureListener
        }
    }

    override fun onPause() {
        closeCamera();
        stopBackgroundThread()
        super.onPause()
    }





//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
//
//            // Once the image is captured, get it from the saved location
//            val f = File(mCurrentPhotoPath)
//            val contentUri = Uri.fromFile(f)
//
//            if (getBitmapFromUri(contentUri) != null){
//                selectedImage = getBitmapFromUri(contentUri)!!
//            }
//            snapShotView.setImageBitmap(selectedImage)
//        }
//    }
//
//    fun takePicture(view: View){
//
//        clearLabel()
//        dispatchTakePictureIntent()
//    }
//
//    private fun dispatchTakePictureIntent() {
//        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        if (takePictureIntent.resolveActivity(packageManager) != null) {
//            var photoFile:File? = null
//
//            try {
//                //TODO: Clean job to clear all the used images
//                photoFile = createImageFile()
//            }catch (ex: IOException){
//
//            }
//
//            if (photoFile != null) {
//                photoURI = FileProvider.getUriForFile(
//                    this,
//                    "pl.polsl.waga",
//                    photoFile
//                )
//                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
//                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
//            }
//        }
//    }
//
//    @Throws(IOException::class)
//    private fun createImageFile(): File {
//        // Create an image file name
//        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
//        val imageFileName = "JPEG_" + timeStamp + "_"
//        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//        val image = File.createTempFile(
//            imageFileName, /* prefix */
//            ".jpg", /* suffix */
//            storageDir      /* directory */
//        )
//
//        // Save a file: path for use with ACTION_VIEW intents
//        mCurrentPhotoPath = image.getAbsolutePath()
//        return image
//    }
//
//    private fun getBitmapFromUri(filePath: Uri): Bitmap? {
//        var bitmap:Bitmap? = null
//        try{
//            var tempBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, filePath)
//            bitmap = updateImage(tempBitmap)
//        }catch (ex: IOException){
//
//        }
//        return bitmap
//    }
//
//    private fun updateImage(bitmap: Bitmap): Bitmap{
//
//        val isLandScape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
//        var scaledImageWidth = 0.0
//        var scaledImageHeight = 0.0
//
//        when (isLandScape){
//
//            (true)->{
//                scaledImageHeight = snapShotView.height.toDouble()
//                scaledImageWidth = bitmap.width.toDouble() * scaledImageHeight / bitmap.height.toDouble()
//            }
//            (false)->{
//                scaledImageWidth = snapShotView.width.toDouble()
//                scaledImageHeight = bitmap.height.toDouble() * scaledImageWidth / bitmap.width.toDouble()
//            }
//        }
//        val resizedBitmap = Bitmap.createScaledBitmap(bitmap,scaledImageWidth.toInt(),scaledImageHeight.toInt(),true)
//
//        return resizedBitmap
//    }



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
    //fun decodeImage(view: View){

    //    decodeImage()
    //}

    private fun decodeImage(img: Bitmap, isProcessing: referenceBool){
       /* //wersja 1

        val image = FirebaseVisionImage.fromBitmap(img)
        val options = FirebaseVisionObjectDetectorOptions.Builder()
            .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
         val detector = FirebaseVision.getInstance().getOnDeviceObjectDetector(options)
          detector.processImage(image)
           .addOnSuccessListener {
               // Task completed successfully
               Toast.makeText(baseContext, "Cos jest: " + count,
                   Toast.LENGTH_SHORT).show()
               setValuesToTextView(it)
               //detector.close()
               isProcessing.value = false
           }
           .addOnFailureListener {
               // Task failed with an exception
               Toast.makeText(baseContext, "Oops, something went wrong!",
                   Toast.LENGTH_SHORT).show()
               //detector.close()
               isProcessing.value = false
           }*/
        ////////////////////////////////////

        //wersja 2
        /*Log.e("aa", "1111111")
       val image= TensorImage.fromBitmap(img)
        val options: ObjectDetector.ObjectDetectorOptions =
           ObjectDetector.ObjectDetectorOptions.builder().setMaxResults(1).build()
        Log.e("aa", "222222222")
        detector = ObjectDetector.createFromFileAndOptions(this,
               "FoodModel.tflite", options)
        val results: List<Detection> = detector.detect(image)
*/

        //wersja 3
       /* val image= TensorImage.fromBitmap(img)

        val outputs: MutableList<Category> = foodModel.process(image)
            .probabilityAsCategoryList.apply { sortByDescending { it.score } }.take(1) as MutableList<Category>
        var i=0;
        imageLabel.text=""

        if(outputs.isNotEmpty())
        {
            imageLabel.text= "Detected object: ${outputs.get(0).displayName}\n" +"label: ${outputs.get(0).label}\n" + "Probability: ${outputs.get(0).score}\n"

            //setValuesToTextView3(outputs)
          /*  for (obj in outputs) {
                imageLabel.text= "Detected object: ${obj.displayName}\n" +"label: ${obj.label}\n" + "Probability: ${obj.score}\n"
            }*/
            isProcessing.value = false
        }
        else
        {
            imageLabel.text= "Nie rozpoznano obiektu"
            isProcessing.value = false
        }*/

        //wersja 4 - owoce
        val image= TensorImage.fromBitmap(img)
        val byteBuffer = image.buffer
       /* val byteBuffer: ByteBuffer = ByteBuffer.allocate(128*128*3)
        byteBuffer.rewind()
        if (img != null) {
            img.copyPixelsToBuffer(byteBuffer)
        }*/

        var input = TensorBuffer.createFixedSize(intArrayOf(1, 150, 150, 3), DataType.FLOAT32)
        input.loadBuffer(byteBuffer)


      var owocowyModel = Owoce.newInstance(this)

        val outputs = owocowyModel.process(input)

        imageLabel.text=""

            imageLabel.text= "Detected object: ${outputs}\n"

            isProcessing.value = false
    }

    private fun setValuesToTextView2(visionObjects : List<Detection>) {
        for ((idx, obj) in visionObjects.withIndex()) {
            val box = obj.boundingBox
            var categoryName :String = ""
            //if (obj. obj.classificationCategory != FirebaseVisionObject.CATEGORY_UNKNOWN) {
               /* val confidence: Int = obj.classificationConfidence!!.times(100).toInt()
                when(obj.classificationCategory)
                {
                    FirebaseVisionObject.CATEGORY_FOOD->   categoryName = "food"
                    FirebaseVisionObject.CATEGORY_PLACE->   categoryName = "place"
                    FirebaseVisionObject.CATEGORY_FASHION_GOOD->   categoryName = "fashion food"
                    FirebaseVisionObject.CATEGORY_HOME_GOOD->   categoryName = "home good"
                    FirebaseVisionObject.CATEGORY_UNKNOWN->   categoryName = "unknown"
                    FirebaseVisionObject.CATEGORY_PLANT->   categoryName = "plant"

                }*/
                Toast.makeText(baseContext, "Detected object: ${idx}\n" + "Category: ${obj.categories}\n"
                        + "boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})\n"
                        + "Category Label is : ${categoryName}"
                    ,
                    Toast.LENGTH_SHORT).show()
                imageLabel.text= "Detected object: ${idx}\n" + "Category: ${obj.categories}\n" +  "boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})\n" + "Category Label is : ${categoryName}"
          //  }
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
    private fun setValuesToTextView3(visionObjects : List<Category>) {
        for ((idx, obj) in visionObjects.withIndex()) {
                imageLabel.text= "Detected object: ${obj.displayName}\n" + "Probability: ${obj.score}\n"
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