package com.example.myfirstapplication.Screens.Photos

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.myfirstapplication.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoSelectActivity : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2
    private val PERMISSION_REQUEST_CODE = 100

    private lateinit var imageView: ImageView
    private lateinit var notificationManager: NotificationManager
    private lateinit var database: AppDatabase
    private lateinit var photoDao: PhotoDao

    private companion object {
        private const val CHANNEL_ID = "image_update_channel"
        private const val NOTIFICATION_ID = 1
    }

    private var currentScaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_INSIDE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_select)

        initToolbar()
        initUI()
        initDatabase()
        initNotificationManager()
        loadLastPhoto()
    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initUI() {
        imageView = findViewById(R.id.imageView)
        findViewById<Button>(R.id.btnSelectPhoto).setOnClickListener {
            if (checkAndRequestPermissions()) {
                showPhotoSelectionDialog()
            }
        }
        findViewById<Button>(R.id.btnSelectScaleType).setOnClickListener {
            showScaleTypeSelectionDialog()
        }
    }

    private fun initDatabase() {
        database = AppDatabase.getDatabase(this)
        photoDao = database.photoDao()
    }

    private fun initNotificationManager() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    private fun loadLastPhoto() {
        lifecycleScope.launch {
            val lastPhoto = withContext(Dispatchers.IO) {
                photoDao.getLastPhoto()
            }
            lastPhoto?.let {
                val uri = Uri.parse(it.uri)
                try {
                    Glide.with(this@PhotoSelectActivity)
                        .load(uri)
                        .into(imageView)
                    imageView.scaleType = currentScaleType
                } catch (e: Exception) {
                    Log.e("PhotoSelectActivity", "Failed to load image", e)
                    showAlertDialog("Error", "Failed to load the image.")
                }
            }
        }
    }



    private fun showScaleTypeSelectionDialog() {
        val scaleTypes = arrayOf("Original Size", "Aspect Fill", "Stretch to Fill")
        AlertDialog.Builder(this)
            .setTitle("Select Scale Type")
            .setItems(scaleTypes) { _, which ->
                currentScaleType = when (which) {
                    0 -> ImageView.ScaleType.CENTER_INSIDE
                    1 -> ImageView.ScaleType.CENTER_CROP
                    else -> ImageView.ScaleType.FIT_XY
                }
                imageView.scaleType = currentScaleType
                sendImageChangeNotification()
            }
            .show()
    }

    private fun showPhotoSelectionDialog() {
        val options = arrayOf("Choose from Gallery", "Take Photo with Camera")
        AlertDialog.Builder(this)
            .setTitle("Select Option")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> takePhoto()
                }
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, TAKE_PHOTO_REQUEST)
        } else {
            Log.e("PhotoSelectActivity", "No camera app found")
            showAlertDialog("Camera not available", "Your device does not support camera functionality.")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> handleImagePickResult(data)
                TAKE_PHOTO_REQUEST -> handlePhotoCaptureResult(data)
            }
        } else {
            showAlertDialog("Action canceled", "You canceled the photo selection.")
        }
    }

    private fun handleImagePickResult(data: Intent?) {
        val imageUri: Uri? = data?.data
        imageUri?.let {
            imageView.setImageURI(it)
            imageView.scaleType = currentScaleType
            savePhotoUri(it.toString())
            sendImageChangeNotification()
        }
    }

    private fun handlePhotoCaptureResult(data: Intent?) {
        val imageBitmap = data?.extras?.get("data") as? Bitmap
        imageBitmap?.let {
            imageView.setImageBitmap(it)
            imageView.scaleType = currentScaleType
            val imageUri = saveImageToGallery(it)
            savePhotoUri(imageUri.toString())
            sendImageChangeNotification()
        }
    }

    private fun savePhotoUri(uri: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val photo = Photo(uri = uri)
                photoDao.insert(photo)
            }
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Captured Image")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${getString(R.string.app_name)}")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        }

        return uri ?: Uri.EMPTY
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
            false
        } else {
            true
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val permissionsMap = mutableMapOf(
                Manifest.permission.CAMERA to PackageManager.PERMISSION_GRANTED,
                Manifest.permission.READ_EXTERNAL_STORAGE to PackageManager.PERMISSION_GRANTED
            )

            if (grantResults.isNotEmpty()) {
                permissions.forEachIndexed { index, permission ->
                    permissionsMap[permission] = grantResults[index]
                }
            }

            if (permissionsMap[Manifest.permission.CAMERA] == PackageManager.PERMISSION_GRANTED &&
                (permissionsMap[Manifest.permission.READ_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED ||
                        permissionsMap[Manifest.permission.READ_MEDIA_IMAGES] == PackageManager.PERMISSION_GRANTED)
            ) {
                showPhotoSelectionDialog()
            } else {
                showPermissionsDeniedDialog()
            }
        }
    }

    private fun showPermissionsDeniedDialog() {
        AlertDialog.Builder(this)
            .setMessage("Both Camera and Storage permissions are required to use this feature.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Image Update Channel", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notifications when the image changes"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendImageChangeNotification() {
        val intent = Intent(this, PhotoSelectActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your own icon
            .setContentTitle("Image Changed")
            .setContentText("The image in the ImageView has been updated.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
