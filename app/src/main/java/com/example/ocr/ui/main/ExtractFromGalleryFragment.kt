package com.example.ocr.ui.main

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.ocr.databinding.FragmentExtractFromGalleryBinding
import com.example.ocr.ui.utils.UIUtils


class ExtractFromGalleryFragment : Fragment() {

    private lateinit var binding: FragmentExtractFromGalleryBinding
    private var imgURI: Uri? = null
    private val getImageUri = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) {
        imgURI = it
        val imageFile = (UIUtils.getFileFromUri(requireContext(), imgURI))
        if (imageFile == null) {
            Toast.makeText(requireContext(), "No image loaded", Toast.LENGTH_SHORT)
                .show()
            findNavController().navigateUp()
            return@registerForActivityResult
        }
        val action =
            ExtractFromGalleryFragmentDirections.actionExtractFromGalleryFragmentToCropFragment(
                imgURI.toString()
            )
        findNavController().navigate(action)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentExtractFromGalleryBinding.inflate(inflater, container, false)
        getImageUri.launch("image/*")
        return binding.root
    }

}