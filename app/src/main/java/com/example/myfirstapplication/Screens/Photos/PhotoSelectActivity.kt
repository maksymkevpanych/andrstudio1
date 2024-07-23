package com.example.myfirstapplication.Screens.Photos

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.core.content.ContextCompat
import com.example.myfirstapplication.R

class PhotoSelectActivity : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2
    private val PERMISSION_REQUEST_CODE = 100

    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_select)

        // Initialize Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize ImageView
        imageView = findViewById(R.id.imageView)

        // Button to choose photo
        val btnSelectPhoto: Button = findViewById(R.id.btnSelectPhoto)
        btnSelectPhoto.setOnClickListener {
            if (checkAndRequestPermissions()) {
                showPhotoSelectionDialog()
            }
        }
    }

    private fun showPhotoSelectionDialog() {
        // AlertDialog to show options
        val options = arrayOf("Choose from Gallery", "Take Photo with Camera")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Option")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openGallery() // Choose from Gallery
                1 -> takePhoto()   // Take Photo
            }
        }
        builder.show()
    }

    private fun openGallery() {
        // Intent to pick an image from the gallery
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Ensure that there is a camera activity to handle the intent
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
                PICK_IMAGE_REQUEST -> {
                    val imageUri: Uri? = data?.data
                    imageUri?.let { imageView.setImageURI(it) }
                }
                TAKE_PHOTO_REQUEST -> {
                    val imageBitmap = data?.extras?.get("data") as? Bitmap
                    imageBitmap?.let { imageView.setImageBitmap(it) }
                }
            }
        } else {
            showAlertDialog("Action canceled", "You canceled the photo selection.")
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val listPermissionsNeeded = mutableListOf<String>()

        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)

        val readImagesPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA)
        }
        if (readImagesPermission != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listPermissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val perms: MutableMap<String, Int> = mutableMapOf()

            perms[Manifest.permission.CAMERA] = PackageManager.PERMISSION_GRANTED
            perms[Manifest.permission.READ_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED

            if (grantResults.isNotEmpty()) {
                for (i in permissions.indices) {
                    perms[permissions[i]] = grantResults[i]
                }
                if (perms[Manifest.permission.CAMERA] == PackageManager.PERMISSION_GRANTED &&
                    (perms[Manifest.permission.READ_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED ||
                            perms[Manifest.permission.READ_MEDIA_IMAGES] == PackageManager.PERMISSION_GRANTED)
                ) {
                    showPhotoSelectionDialog()
                } else {
                    showPermissionsDeniedDialog()
                }
            }
        }
    }

    private fun showPermissionsDeniedDialog() {
        AlertDialog.Builder(this)
            .setMessage("Both Camera and Storage permissions are required to use this feature.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish() // Close PhotoSelectActivity and return to MainActivity
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
