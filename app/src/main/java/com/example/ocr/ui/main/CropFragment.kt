package com.example.ocr.ui.main

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.ocr.databinding.FragmentCropBinding
import com.example.ocr.ui.utils.extensions.rotate
import org.opencv.android.OpenCVLoader

//Use this code to make the image lesser in size and to ease the processing
object BitmapScaler {
    // Scale and maintain aspect ratio given a desired width
    // BitmapScaler.scaleToFitWidth(bitmap, 100);
    fun scaleToFitWidth(b: Bitmap, width: Int): Bitmap {
        val factor = width / b.width.toFloat()
        return Bitmap.createScaledBitmap(b, width, (b.height * factor).toInt(), true)
    }

    // Scale and maintain aspect ratio given a desired height
    // BitmapScaler.scaleToFitHeight(bitmap, 100);
    fun scaleToFitHeight(b: Bitmap, height: Int): Bitmap {
        val factor = height / b.height.toFloat()
        return Bitmap.createScaledBitmap(b, (b.width * factor).toInt(), height, true)
    }
}

class CropFragment : Fragment() {
    private lateinit var binding: FragmentCropBinding
    private lateinit var bitmap: Bitmap
    private lateinit var imgUri: Uri
    private val args: CropFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCropBinding.inflate(inflater, container, false)
        imgUri = Uri.parse(args.imgURI)
        binding.imgView.setImageURI(imgUri)
        bitmap = binding.imgView.drawable.toBitmap()
        OpenCVLoader.initDebug()
        allClickListeners()
        return binding.root
    }

    private fun allClickListeners() {
        binding.cropCancelBtn.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.cropDoneBtn.setOnClickListener {
            val tempBitmap = binding.imgView.getCroppedImage()
            //TODO: instead pass it to the paint fragment
            if (tempBitmap != null)
                binding.imgView.setImageBitmap(tempBitmap)
        }
        binding.rotateImage.apply {
            setOnClickListener {
                binding.imgView.apply {
                    this@CropFragment.bitmap = bitmap.rotate(90f)
                    this.setImageBitmap(bitmap)
                    this.invalidate()
                }
            }
            //TODO: change icon tint when selected
        }
    }
}