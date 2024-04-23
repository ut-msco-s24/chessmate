package com.example.ayushchess

import MainViewModel
import android.app.ProgressDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.ayushchess.databinding.FragmentSecondBinding
import java.util.UUID

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */

class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtonPlay()

        binding.buttonLogout.setOnClickListener {
            (activity as MainActivity).authUser.logout()
        }

        binding.playAi.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment,)
        }
        binding.buttonTopplayers.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_TopPlayersFragment,)
        }
    }

    private fun setupButtonPlay() {
        binding.buttonPlay.setOnClickListener {
            val progressDialog = ProgressDialog.show(context, "", "Looking for a match...", true)
            viewModel.getCurrentUser().observe(viewLifecycleOwner) { user ->
                viewModel.joinLobby(UUID.randomUUID().toString()).observe(viewLifecycleOwner) { gameInfo ->
                    progressDialog.dismiss()
                    viewModel.setGameId(gameInfo.id)
                    viewModel.setCurrentUserSide(gameInfo.side)
                    val bundle = bundleOf("gameId" to gameInfo.id, "side" to gameInfo.side)
                    findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment, bundle)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
