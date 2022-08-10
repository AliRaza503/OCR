package com.example.ocr.ui.main

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.ocr.databinding.FragmentExtractFromGalleryBinding
import com.example.ocr.ui.utils.UIUtils
import com.example.ocr.ui.utils.extensions.rotate


//object BitmapScaler {
//    // Scale and maintain aspect ratio given a desired width
//    // BitmapScaler.scaleToFitWidth(bitmap, 100);
//    fun scaleToFitWidth(b: Bitmap, width: Int): Bitmap {
//        val factor = width / b.width.toFloat()
//        return Bitmap.createScaledBitmap(b, width, (b.height * factor).toInt(), true)
//    }
//
//    // Scale and maintain aspect ratio given a desired height
//    // BitmapScaler.scaleToFitHeight(bitmap, 100);
//    fun scaleToFitHeight(b: Bitmap, height: Int): Bitmap {
//        val factor = height / b.height.toFloat()
//        return Bitmap.createScaledBitmap(b, (b.width * factor).toInt(), height, true)
//    }
//}

class ExtractFromGalleryFragment : Fragment() {

    private lateinit var binding: FragmentExtractFromGalleryBinding
    private var imgURI: Uri? = null
    private lateinit var bitmap: Bitmap
    private val getImageUri = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) {
        imgURI = it

        val imageFile = (UIUtils.getFileFromUri(requireContext(), imgURI))
        if (imageFile == null) {
            Toast.makeText(requireContext(), "Can't load the selected file!", Toast.LENGTH_SHORT)
                .show()
            return@registerForActivityResult
        }
        binding.imgView.setImageURI(imgURI)
        bitmap = binding.imgView.drawable.toBitmap()
        //Draw cropping rectangle and start working
//        initCropping()
        binding.imgInfo.text = "${bitmap.width} height ${bitmap.height}"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentExtractFromGalleryBinding.inflate(inflater, container, false)


        getImageUri.launch("image/*")
        //Set click listeners
        allClickListeners()
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun allClickListeners() {
        binding.cropCancelBtn.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.cropDoneBtn.setOnClickListener {
            //TODO: get crop image
        }
        binding.rotateImage.apply {
            setOnClickListener {
                binding.imgView.apply {
                    this@ExtractFromGalleryFragment.bitmap = bitmap.rotate(90f)
                    this.setImageBitmap(bitmap)
                    //Image is redrawn when rotated
                    this.imgDrawn = false
                    this.invalidate()
                }
            }
            //TODO: change icon tint when selected
        }
    }
}