package com.example.ayushchess

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ayushchess.databinding.FragmentTopPlayersBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TopPlayersFragment : Fragment() {

    private var _binding: FragmentTopPlayersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTopPlayersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).supportActionBar?.show()
        setupRecyclerView()

    }

    private fun setupRecyclerView() {
        val adapter = TopPlayersAdapter()
        binding.rvTopPlayers.layoutManager = LinearLayoutManager(context)
        binding.rvTopPlayers.adapter = adapter
        fetchTopPlayers(adapter)
    }

    private fun fetchTopPlayers(adapter: TopPlayersAdapter) {
        FirebaseFirestore.getInstance().collection("wins")
            .orderBy("wins", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                val players = documents.map { doc ->
                    mapOf(
                        "username" to doc.getString("username").orEmpty(),
                        "wins" to (doc.getLong("wins") ?: 0L).toString()
                    )
                }
                adapter.submitList(players)
                Log.d("asdfa", players.toString())
            }
            .addOnFailureListener { exception ->
                Log.d("asdfa", exception.toString())
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
