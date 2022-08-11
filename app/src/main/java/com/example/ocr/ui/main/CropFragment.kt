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
        allClickListeners()
        return binding.root
    }

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
                    this@CropFragment.bitmap = bitmap.rotate(90f)
                    this.setImageBitmap(bitmap)
                    this.invalidate()
                }
            }
            //TODO: change icon tint when selected
        }
    }
}