package com.example.ocr.UI.Main

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.ocr.databinding.FragmentExtractFromGalleryBinding

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
        bitmap = MediaStore.Images.Media.getBitmap(
            requireActivity().contentResolver,
            imgURI
        )
        binding.imgView.setImageBitmap(bitmap)
        binding.imgInfo.text = "${bitmap.width} height ${bitmap.height}"
    }

//    private val Int.toDp get() = this / Resources.getSystem().displayMetrics.density.toInt()

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

    private fun allClickListeners() {
        binding.cropCancelBtn.setOnClickListener {
            findNavController().navigateUp()
        }
        //TODO: implement going forward with doneBtn clickListener
        binding.rotateImage.apply {
            setOnClickListener {
                binding.imgView.apply {
                    bitmap = bitmap.rotate(90f)
                    this.setImageBitmap(bitmap)
                }
            }
            //TODO: change icon tint when selected
        }
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}