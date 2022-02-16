package com.sample.edgedetection.scan

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sample.edgedetection.R
import com.sample.edgedetection.REQUEST_CODE
import com.sample.edgedetection.SCANNED_RESULT
import com.sample.edgedetection.base.BaseActivity
import com.sample.edgedetection.view.PaperRectangle

import kotlinx.android.synthetic.main.activity_scan.*
import org.opencv.android.OpenCVLoader
import androidx.core.graphics.drawable.DrawableCompat


class ScanActivity : BaseActivity(), IScanView.Proxy {

    private val REQUEST_CAMERA_PERMISSION = 0
    private val EXIT_TIME = 2000

    private lateinit var mPresenter: ScanPresenter


    override fun provideContentViewId(): Int = R.layout.activity_scan

    override fun initPresenter() {
        mPresenter = ScanPresenter(this, this)
    }

    override fun prepare() {
        if (!OpenCVLoader.initDebug()) {
            Log.i(TAG, "loading opencv error, exit")
            finish()
        }
     if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }

        shut.setOnClickListener {
            mPresenter.shut()
        }
    }


    override fun onStart() {
        super.onStart()
        mPresenter.start()
        invalidateOptionsMenu()
    }

    override fun onStop() {
        super.onStop()
        mPresenter.stop()
    }

    override fun exit() {
        finish()
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        var allGranted = false
        var indexPermission = -1

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.count() == 1) {
                if (permissions.indexOf(android.Manifest.permission.CAMERA) >= 0) {
                    indexPermission = permissions.indexOf(android.Manifest.permission.CAMERA)
                }
                if (permissions.indexOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) >= 0) {
                    indexPermission =
                        permissions.indexOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                if (indexPermission >= 0 && grantResults[indexPermission] == PackageManager.PERMISSION_GRANTED) {
                    allGranted = true
                }
            }

            if (grantResults.count() == 2 && (
                        grantResults[permissions.indexOf(android.Manifest.permission.CAMERA)] == PackageManager.PERMISSION_GRANTED
                                && grantResults[permissions.indexOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)] == PackageManager.PERMISSION_GRANTED)
            ) {
                allGranted = true
            }
        }

        if (allGranted) {
            showMessage(R.string.camera_grant)
            mPresenter.initCamera()
            mPresenter.updateCamera()
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }

    override fun getDisplay(): Display = windowManager.defaultDisplay

    override fun getSurfaceView(): SurfaceView = surface

    override fun getPaperRect(): PaperRectangle = paper_rect

    override fun toggleInProgress(inProgress: Boolean) {
        runOnUiThread {
            shut.visibility = if (inProgress) View.GONE else View.VISIBLE
            progressBar.visibility = if (inProgress) View.VISIBLE else View.GONE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (null != data && null != data.extras) {
                    val path = data.extras!!.getString(SCANNED_RESULT)
                    setResult(Activity.RESULT_OK, Intent().putExtra(SCANNED_RESULT, path))
                    finish()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.scan_activity_menu, menu)

        menu?.findItem(R.id.action_torch)?.isVisible = mPresenter.isTorchAvailable()

        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_torch)?.icon?.let { icon ->
            val drawable = DrawableCompat.wrap(icon)
            val flashOn = mPresenter.isTorchOn()
            DrawableCompat.setTint(drawable, ContextCompat.getColor(this, if (flashOn) R.color.colorFlashOn else R.color.colorFlashOff))
            menu.findItem(R.id.action_torch).icon = drawable
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        if (item.itemId == R.id.action_torch) {
            mPresenter.toggleTorch()
            invalidateOptionsMenu()
        }

        return super.onOptionsItemSelected(item)
    }
}