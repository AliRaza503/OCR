package com.example.ocr.ui.main

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.ocr.R
import com.example.ocr.databinding.FragmentHomeScreenBinding
import com.google.android.datatransport.BuildConfig


class HomeScreenFragment : Fragment() {
    private lateinit var binding: FragmentHomeScreenBinding

    //Fab buttons appearance animation variables
    private val rotateOpenAnim: Animation by lazy {
        AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.fab_open
        )
    }
    private val rotateCloseAnim: Animation by lazy {
        AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.fab_closed
        )
    }
    private val appearAnim: Animation by lazy {
        AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.fabs_appear
        )
    }
    private val disappearAnim: Animation by lazy {
        AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.fab_disappear
        )
    }
    private var fabAddClicked: Boolean = false

    //Permissions
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestStoragePermissionLauncher: ActivityResultLauncher<String>

    /**
     * Camera Permission methods
     */
    private fun registerCameraPermission() {
        requestCameraPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (!granted) {
                    requestCameraPermission()
                } else {
                    //user clicked on allow button so proceed
                    //TODO: navigate to camera fragment
                }
            }
    }

    private fun requestCameraPermission() {

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                //TODO: navigate to camera fragment
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // This case means user previously denied the permission
                // So here we can display an explanation to the user
                // That why exactly we need this permission
                showPermissionAlert(
                    "Camera Permission",
                    "Permission required to scan documents\n" +
                            " You can still grant it from settings",
                ) { requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
            }
            else -> {
                // Everything is fine you can simply request the permission

                showPermissionAlert(
                    "Camera Permission",
                    "Permission required to scan documents",
                ) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts(
                        "package",
                        BuildConfig.APPLICATION_ID, null
                    )
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }

            }
        }
    }

    //To be called when camera is to be accessed
    fun callCameraPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * Storage Permission methods
     */
    private fun registerStoragePermission() {
        requestStoragePermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (!granted) {
                    requestStoragePermission()
                } else {
                    //user clicked on allow button so proceed
                    findNavController().navigate(R.id.action_homeScreenFragment_to_extractFromGalleryFragment)
                }
            }
    }

    //TO be called when storage permission is required
    private fun requestStoragePermission() {

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                findNavController().navigate(R.id.action_homeScreenFragment_to_extractFromGalleryFragment)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                // This case means user previously denied the permission
                // So here we can display an explanation to the user
                // That why exactly we need this permission
                showPermissionAlert(
                    "Storage Permission",
                    "Permission required to scan existing images",
                ) { requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE) }
            }
            else -> {
                // Everything is fine you can simply request the permission

                showPermissionAlert(
                    "Storage Permission",
                    "Permission required to scan existing images.\n" +
                            " You can still grant it from settings",
                ) {
                    //Open settings to get permissions
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts(
                            "package",
                            requireActivity().packageName,
                            null
                        )
                    })
                }
            }
        }
    }

    /**
     * To show alert dialog in case permissions are not granted
     */
    private fun showPermissionAlert(
        title: String,
        message: String,
        function: () -> Unit
    ) {
        val mDialog = Dialog(requireContext())
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        mDialog.setContentView(R.layout.dialog_permission_alert)
        //the dialog window will be transparent
        mDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val mTitleTv = mDialog.findViewById<View>(R.id.title_tv) as AppCompatTextView
        mTitleTv.text = title

        val mMessageTv = mDialog.findViewById<View>(R.id.message_tv) as AppCompatTextView
        mMessageTv.text = message

        val mNoBtn = mDialog.findViewById<View>(R.id.no_btn) as AppCompatTextView

        val mYesBtn = mDialog.findViewById<View>(R.id.yes_btn) as AppCompatTextView

        mYesBtn.setOnClickListener {
            function.invoke()
            mDialog.dismiss()
        }

        mNoBtn.setOnClickListener { mDialog.dismiss() }

        mDialog.setCancelable(true)
        mDialog.show()
        mDialog.window!!.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //It is necessary to register the permissions in on create method
        registerCameraPermission()
        registerStoragePermission()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //Inflate the layout for this fragment
        binding = FragmentHomeScreenBinding.inflate(inflater, container, false)
        if (!isFabIntroduced()) {
            val animation =
                AnimationUtils.loadAnimation(requireContext(), R.anim.fab_intro_anim).apply {
                    duration = 1500
                    interpolator = LinearInterpolator()
                }
            binding.backCircleToAnimate.visibility = View.VISIBLE
            binding.backCircleToAnimate.startAnimation(animation)
            //Show the introduction sheet with animation
            val blueCircleAnim =
                AnimationUtils.loadAnimation(requireContext(), R.anim.blue_oval_anim).apply {
                    duration = 1500
                    interpolator = AccelerateDecelerateInterpolator()
                    isFillEnabled = true
                    fillAfter = true
                }
            binding.blueCircleToAnimate.visibility = View.VISIBLE
            binding.fabIntroSheet.visibility = View.VISIBLE
            binding.blueCircleToAnimate.startAnimation(blueCircleAnim)
        }
        //Add FAB click listener
        binding.fabAdd.setOnClickListener {
            binding.backCircleToAnimate.visibility = View.INVISIBLE
            binding.blueCircleToAnimate.visibility = View.INVISIBLE
            binding.fabIntroSheet.visibility = View.INVISIBLE
            binding.backCircleToAnimate.clearAnimation()
            binding.blueCircleToAnimate.clearAnimation()
            setFabIntroduced()
            onAddButtonClicked()
        }
        //To extract image from gallery
        binding.fabOpenExisting.setOnClickListener {
            //Close the fab before proceeding
            binding.fabAdd.performClick()
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                findNavController().navigate(R.id.action_homeScreenFragment_to_extractFromGalleryFragment)
            }
        }
        //on clicking the root view clear focus of fab
        binding.focusView.setOnClickListener {
            if (fabAddClicked)
                binding.fabAdd.performClick()
        }
        return binding.root
    }


    //On add FAB click listener
    private fun onAddButtonClicked() {
        fabAddClicked = !fabAddClicked
        //If fab is clicked 1 time
        if (fabAddClicked) {
            //make visible and clickable
            binding.fabCaptureNew.apply {
                visibility = View.VISIBLE
                isClickable = true
            }
            binding.fabOpenExisting.apply {
                visibility = View.VISIBLE
                isClickable = true
            }
            //Set text visibility
            binding.captureNewTV.visibility = View.VISIBLE
            binding.openExistingTV.visibility = View.VISIBLE
            //Set opening animations
            binding.fabAdd.startAnimation(rotateOpenAnim)
            binding.fabCaptureNew.startAnimation(appearAnim)
            binding.fabOpenExisting.startAnimation(appearAnim)
            binding.captureNewTV.startAnimation(appearAnim)
            binding.openExistingTV.startAnimation(appearAnim)
            //Set background layout color
            binding.focusView.visibility = View.VISIBLE
        }
        //else if fab is clicked 2nd time
        else {
            //make invisible and non clickable
            binding.fabCaptureNew.apply {
                visibility = View.INVISIBLE
                isClickable = false
            }
            binding.fabOpenExisting.apply {
                visibility = View.INVISIBLE
                isClickable = false
            }
            //Set text invisible
            binding.captureNewTV.visibility = View.INVISIBLE
            binding.openExistingTV.visibility = View.INVISIBLE
            //set closing animations
            binding.fabAdd.startAnimation(rotateCloseAnim)
            binding.fabCaptureNew.startAnimation(disappearAnim)
            binding.fabOpenExisting.startAnimation(disappearAnim)
            //Set background layout color
            binding.focusView.visibility = View.GONE
        }
    }

    //Fab is introduced
    private fun setFabIntroduced() {
        val sharedPreferences =
            requireActivity().getSharedPreferences("FabIntro", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("Introduced", true)
        editor.apply()
    }

    //Check if the fab is introduced
    private fun isFabIntroduced(): Boolean {
        val sharedPreferences =
            requireActivity().getSharedPreferences("FabIntro", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("Introduced", false)
    }
}