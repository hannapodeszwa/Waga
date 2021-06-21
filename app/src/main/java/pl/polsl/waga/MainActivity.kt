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
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel
import com.google.firebase.ml.vision.objects.FirebaseVisionObject
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.support.label.TensorLabel
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
    lateinit var imageBitmap: Bitmap
    private lateinit var IsProcessing :referenceBool
    private var recognizedFruit: String= ""
    private var toPrint:String = ""


    var selectedImage: Bitmap? = null
    lateinit var photoURI: Uri
    enum class UserPermission{
        CAMERA,
        WRITE_DATA
    }


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


        foodModel = FoodModel.newInstance(this)
        val yesButton: Button = findViewById(R.id.yesButton)
        val noButton: Button = findViewById(R.id.noButton)
        val startButton: Button = findViewById(R.id.startButton)
        yesButton.setVisibility(View.GONE);
        noButton.setVisibility(View.GONE);
        //BUTTONS
        yesButton.setOnClickListener {
            val toast = Toast.makeText(applicationContext, "Drukowanie etykiety dla " +toPrint, Toast.LENGTH_SHORT)
            toast.show()

            clearLabel()
        }
        noButton.setOnClickListener {
            // Do something in response to button click
        }

        startButton.setOnClickListener {
            decodeImage(imageBitmap,IsProcessing)


            startButton.setText("Rozpoznaj ponownie")
            yesButton.setVisibility(View.VISIBLE);
            noButton.setVisibility(View.VISIBLE);
            toPrint=recognizedFruit
            recognizedFruit=""
        }

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
            count++;

            if(isProcessing.value == false) {
                isProcessing.value = true
                thread {
                    val frame = Bitmap.createBitmap(textureView!!.width, textureView!!.height, Bitmap.Config.ARGB_8888)
                    textureView?.getBitmap(frame)
                    imageBitmap=frame
                    IsProcessing=isProcessing
                    // decodeImage(frame,isProcessing)
                }
            }
        }
    }

    suspend private fun asyncDecode(frame: Bitmap, isProcessing: referenceBool) {
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

    private fun decodeImage(img: Bitmap, isProcessing: referenceBool){
        //wersja 4 - owoce

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(150, 150, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        var tImage = TensorImage(DataType.FLOAT32)

        tImage.load(img)
        tImage = imageProcessor.process(tImage)


        val probabilityProcessor =
            TensorProcessor.Builder().add(NormalizeOp(0f, 255f)).build()
        var owocowyModel = Owoce.newInstance(this)

        val outputs =
            owocowyModel.process(probabilityProcessor.process(tImage.tensorBuffer))
        val outputBuffer = outputs.outputFeature0AsTensorBuffer
        val labelsList = arrayListOf("Jabłko", "Banan", "Karambola", "Guawa", "Kiwi","Mango", "Melon",
            "Pomarancza", "Brzoskwinia", "Gruszka", "Persymona", "Papaja", "Sliwka", "Granat")
        /*val labelsList = arrayListOf("Jabłko czerwone","Jabłko zielone", "Morela","Awokado",
            "Banan", "Borowka", "Kaktus", "Kantalupa", "Wisnia","Mandarynka", "Winogrono",
            "Kiwi", "Cytryna", "Limonka", "Mango",
      "Pomarancza", "Papaja", "Marakuja", "Brzoskiwnia", "Gruszka", "Ananas", "Sliwka", "Granat",
        "Malina", "Truskawka", "Arbuz")*/
        val tensorLabel = TensorLabel(labelsList, outputBuffer)
        var tmp=0
        var fruit =" nw co to "
        var probability = " "
        for(a in tensorLabel.categoryList)
        {
            if(a.score > 0.50 && tmp<a.score)
            {
                fruit=a.label
                probability = a.score.toString()
            }

        }
        recognizedFruit = fruit
        //imageLabel.text =  "Owoc : "+ owocek + "\nPrawdopodobieństwo: " + probability
        imageLabel.text =  "Czy twój produkt to:\n"+ recognizedFruit + "\nPrawdopodobieństwo: " + probability

        isProcessing.value = false
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