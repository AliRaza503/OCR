package com.example.ocr.ui.main

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.ocr.R
import com.example.ocr.databinding.FragmentExtractFromGalleryBinding
import com.example.ocr.ui.utils.UIUtils
import com.example.ocr.ui.utils.extensions.rotate
import com.example.ocr.ui.utils.extensions.scaledBitmap


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
        bitmap = BitmapFactory.decodeFile(imageFile.path)
        binding.imgView.setImageBitmap(bitmap)
        //Draw cropping rectangle and start working
        initCropping()
        binding.imgInfo.text = "${bitmap.width} height ${bitmap.height}"
    }

    /**
     * To make the crop rectangle visible and start cropping
     */
    private fun initCropping() {
        val scaledBitmap: Bitmap =
            bitmap.scaledBitmap(binding.imgCropHolder.width, binding.imgCropHolder.height)
        binding.imgView.setImageBitmap(scaledBitmap)
        val tempBitmap = (binding.imgView.drawable as BitmapDrawable).bitmap
        //This is to get the edge points of image
        val pointFs = getEdgePoints(tempBitmap)
        binding.cropRectangleView.setPoints(pointFs)
        binding.cropRectangleView.visibility = View.VISIBLE
        val padding = resources.getDimension(R.dimen.rectangle_dimens).toInt()
        val layoutParams =
            FrameLayout.LayoutParams(tempBitmap.width + padding, tempBitmap.height + padding)
        layoutParams.gravity = Gravity.CENTER
        binding.cropRectangleView.layoutParams = layoutParams
    }

    private fun getEdgePoints(tempBitmap: Bitmap): Map<Int, PointF> {
        //whole image coverage
        val pointFs = mutableListOf<PointF>()
        pointFs.add(PointF(20f, 20f))
        pointFs.add(PointF(tempBitmap.width.toFloat(), 20f))
        pointFs.add(PointF(20f, tempBitmap.height.toFloat()))
        pointFs.add(PointF(tempBitmap.width.toFloat(), tempBitmap.height.toFloat()))
        return binding.cropRectangleView.getOrderedValidEdgePoints(tempBitmap, pointFs)
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

    @SuppressLint("ClickableViewAccessibility")
    private fun allClickListeners() {
        binding.cropCancelBtn.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.cropDoneBtn.setOnClickListener {
            getCoppedImage()
        }
        binding.rotateImage.apply {
            setOnClickListener {
                binding.imgView.apply {
                    this@ExtractFromGalleryFragment.bitmap = bitmap!!.rotate(90f)
                    this.setImageBitmap(bitmap!!)
                }
            }
            //TODO: change icon tint when selected
        }
    }

    private fun getCoppedImage() {
        val points: Map<Int, PointF> = binding.cropRectangleView.getPoints()
        val xRatio: Float = bitmap.width.toFloat() / bitmap.width
        val yRatio: Float = bitmap.height.toFloat() / bitmap.height
        val pointPadding =
            requireContext().resources.getDimension(R.dimen.rectangle_dimens).toInt()
        val x1: Float = (points.getValue(0).x + pointPadding) * xRatio
        val x2: Float = (points.getValue(1).x + pointPadding) * xRatio
        val x3: Float = (points.getValue(2).x + pointPadding) * xRatio
        val x4: Float = (points.getValue(3).x + pointPadding) * xRatio
        val y1: Float = (points.getValue(0).y + pointPadding) * yRatio
        val y2: Float = (points.getValue(1).y + pointPadding) * yRatio
        val y3: Float = (points.getValue(2).y + pointPadding) * yRatio
        val y4: Float = (points.getValue(3).y + pointPadding) * yRatio
    }
}