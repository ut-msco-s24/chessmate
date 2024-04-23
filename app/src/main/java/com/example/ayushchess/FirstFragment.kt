package com.example.ayushchess

import MainViewModel
import android.app.AlertDialog
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.ayushchess.databinding.FragmentFirstBinding
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.File
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Rank
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlin.properties.Delegates

class FirstFragment : Fragment() {

    private var gameOverFlag: Boolean = false
    private var isVsPlayer: Boolean = false
    private val initialFen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    private lateinit var databaseReference: DatabaseReference
    private var gameId: String? = null
    private var playerSide: Side? = null
    val minutes: Long = 3
    val time: Long = 10 * minutes * 1000
    private var whiteTimeMillis: Long = time
    private var blackTimeMillis: Long = time

    private var whiteTimer: CountDownTimer? = null

    private var blackTimer: CountDownTimer? = null

    private var squareSize by Delegates.notNull<Int>()
    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!




    private fun createTimer(timeMillis: Long, side: Side): CountDownTimer {
        return object : CountDownTimer(timeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if(isAdded) {
                    if (side == Side.WHITE) {
                        whiteTimeMillis = millisUntilFinished
                        binding?.clockBottom?.text = formatTime(millisUntilFinished)

                    } else {
                        blackTimeMillis = millisUntilFinished
                        binding?.clockTop?.text = formatTime(millisUntilFinished)
                    }
                }
            }

            override fun onFinish() {
                this.cancel()
                if (isAdded) {
                    gameOver("${if (side == Side.WHITE) "Black" else "White"} wins on time")
                }
            }
        }
    }


    private fun initializeNewGame() { // Generate a unique game ID

        if(playerSide == Side.WHITE) {
            binding.topText.text = "Opponent"
            binding.bottomText.text = "You"
        }
        else {
            binding.topText.text = "You"
            binding.bottomText.text = "Opponent"
        }

        databaseReference.setValue(
            mapOf(
                "fen" to initialFen,
                "turn" to "init",
                "status" to "ongoing",
                "whiteTime" to whiteTimeMillis.toString(),
                "blackTime" to blackTimeMillis.toString()
            )
        )

    }

    private fun initializeNewGameAi() { // Generate a unique game ID
        playerSide = if (Math.random() < 0.50) Side.WHITE else Side.BLACK

        if(playerSide == Side.WHITE) {
            binding.topText.text = "Opponent"
            binding.bottomText.text = "You"
        }
        else {
            binding.topText.text = "You"
            binding.bottomText.text = "Opponent"
        }


        databaseReference.setValue(
            mapOf(
                "fen" to initialFen,
                "turn" to "init",
                "status" to "ongoing"
            )
        )

        val updates = hashMapOf<String, Any>(
            "fen" to initialFen,
            "turn" to "ai"
        )

        if(playerSide == Side.BLACK) {
            databaseReference.updateChildren(updates).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("UpdateFirebase", "Game updated successfully")
                } else {
                    Log.e("UpdateFirebase", "Failed to update game", task.exception)
                }
            }
        }

    }

    private val board: Board = Board()

    private fun onMoveMade() {
        if (board.sideToMove == Side.BLACK) {
            whiteTimer?.cancel()  // Cancel the current white timer
            whiteTimer = null  // Nullify the timer

            // Initialize and start black timer if it's not already running or null
            if (blackTimer == null) {
                blackTimer = createTimer(blackTimeMillis, Side.BLACK)
            }
            blackTimer?.start()
        } else {
            blackTimer?.cancel()  // Cancel the current black timer
            blackTimer = null  // Nullify the timer

            // Initialize and start white timer if it's not already running or null
            if (whiteTimer == null) {
                whiteTimer = createTimer(whiteTimeMillis, Side.WHITE)
            }
            whiteTimer?.start()
        }
    }

    private fun gameOverUpdate(status: String) {
        if (!isVsPlayer) {
            return
        }
        val updates = hashMapOf<String, Any>(
            "whiteTime" to whiteTimeMillis.toString(),
            "blackTime" to blackTimeMillis.toString(),
            "turn" to "opp",
            "status" to status
        )


        databaseReference.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("UpdateFirebase", "Game updated successfully")
            } else {
                Log.e("UpdateFirebase", "Failed to update game", task.exception)
            }
        }
    }

    private fun render() {
        if(!isAdded) {
            return
        }
        clearHighlights()  // Clear previous highlights
        if (board.isDraw) {
            if(board.isInsufficientMaterial) {
                gameOver("Draw by insufficent material")
            }
            if(board.isRepetition) {
                gameOver("Draw by repetition")
            }
            if(board.isStaleMate) {
                gameOver("Draw by stalemate")
            }
        }
        else if(board.isMated) {
            if (board.sideToMove == Side.BLACK) {
                gameOver("White wins by checkmate")
            }
            else {
                gameOver("Black wins by checkmate")
            }
        }
        if (selection != null) {
            val moves = board.legalMoves().filter { it.from == selection!!.s }
            moves.forEach { move ->
                val (rowIndex, colIndex) = translateSquareToBoardIndex(move.to)
                highlightSquare(rowIndex, colIndex)
            }
        }
        updatePieces()  // Update pieces on the board
    }

    private fun highlightSquare(rowIndex: Int, colIndex: Int) {
        val frameLayout = getViewFromGrid(binding.chessBoard, rowIndex, colIndex) as FrameLayout
        val squareImageView = frameLayout.getChildAt(0) as ImageView
        squareImageView.setImageResource(R.drawable.gray_square)
        squareImageView.setOnClickListener(null)
        squareImageView.setOnClickListener {
            val targetSquare = Square.squareAt(rowIndex * 8 + colIndex)

            // Check if the move is a pawn promotion
            fun isPromotionMove(from: Square, to: Square): Boolean {
                val piece = board.getPiece(from)
                return (piece == Piece.WHITE_PAWN && to.rank == Rank.RANK_8) ||
                        (piece == Piece.BLACK_PAWN && to.rank == Rank.RANK_1)
            }
            selection?.let { sel ->

                val afterMove =  {
                    selection = null
                    val newFen = board.fen
                    updateFirebaseGame(newFen)
                    render()
                }

                if (isPromotionMove(sel.s, targetSquare)) {
                    showPromotionDialog(sel.s, targetSquare, afterMove) // Handle promotion
                } else {
                    board.doMove(
                        Move(
                            sel.s,
                            Square.squareAt(rowIndex * 8 + colIndex)
                        )
                    )  // Pseudocode for move execution
                    afterMove()
                }
            }
        }
    }

    private fun setupFirebaseGameListener() {
        val gameRef = databaseReference
        gameRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("asdfadsf", "$snapshot")

                val fen = snapshot.child("fen").getValue(String::class.java) ?: return
                val turn = snapshot.child("turn").getValue(String::class.java) ?: return

                if(isVsPlayer) {
                    val blackTimeStr = snapshot.child("blackTime").getValue(String::class.java) ?: return
                    val whiteTimeStr = snapshot.child("whiteTime").getValue(String::class.java) ?: return
                    val status = snapshot.child("status").getValue(String::class.java);
                    if (status != null && status != "ongoing" && !gameOverFlag) {
                        gameOver(status)
                        return
                    }


                    whiteTimeMillis = whiteTimeStr.toLong()
                    blackTimeMillis = blackTimeStr.toLong()

                }

                var board2: Board? = null
                if (isVsPlayer) {
                    board2 = Board()
                    board2.loadFromFen(fen)
                    Log.d("asdfadsf", fen)
                    Log.d("tagsdfasd", playerSide.toString())
                }




                if (isVsPlayer &&  board2?.sideToMove == playerSide && turn != "init") {
                    board.loadFromFen(fen)
                    render()
                    onMoveMade()
                }
                else if (!isVsPlayer && turn == "player") {
                    board.loadFromFen(fen)
                    render()
                    onMoveMade()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to read game data", error.toException())
            }
        })
    }

    private fun updateFirebaseGame(newFen: String) {

        var updates: HashMap<String, Any>
        if(!isVsPlayer) {
            updates = hashMapOf(
                "fen" to newFen,
                "turn" to "ai"
            )
        }
        else {
            updates = hashMapOf(
                "fen" to newFen,
                "whiteTime" to whiteTimeMillis.toString(),
                "blackTime" to blackTimeMillis.toString(),
                "turn" to "opp"
            )
        }

        onMoveMade()
        databaseReference.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("UpdateFirebase", "Game updated successfully")
            } else {
                Log.e("UpdateFirebase", "Failed to update game", task.exception)
            }
        }
    }

    private fun formatTime(millis: Long): String {
        val minutes = (millis / 1000) / 60
        val seconds = (millis / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun pushWinner() {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("wins").document(uid)

        // Run a transaction to ensure atomic read and increment operations
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            if (!snapshot.exists()) {
                // If the document does not exist, create it with a win count of 1
                transaction.set(userRef, mapOf(
                    "uid" to uid,
                    "username" to username,
                    "wins" to 1
                ))
            } else {
                // If the document exists, increment the wins count
                val currentWins = snapshot.getLong("wins") ?: 0
                transaction.update(userRef, "wins", currentWins + 1)
            }
        }.addOnSuccessListener {
            Log.d("FirebaseFirestore", "User wins updated successfully.")
        }.addOnFailureListener { e ->
            Log.w("FirebaseFirestore", "Error updating user wins", e)
        }
    }


    private fun gameOver(message: String = "") {
        if(gameOverFlag) {
            return
        }


        whiteTimer?.cancel()
        blackTimer?.cancel()

        for (i in 0 until 8) {
            for (j in 0 until 8) {

                val v = getViewFromGrid(binding.chessBoard, i, j) as FrameLayout
                (v.getChildAt(0) as ImageView).setOnClickListener(null)
                (v.getChildAt(1) as ImageView).setOnClickListener(null)

            }
        }
        
        gameOverFlag = true;

        if(message.contains("wins")) {
            if(message.contains("White") && playerSide == Side.WHITE) {
                pushWinner()
            }
            if(message.contains("Black") && playerSide == Side.BLACK) {
                pushWinner()
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Game Over")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                findNavController().navigate(R.id.SecondFragment)
             }
            .show()
    }


    private fun showPromotionDialog(pawnSquare: Square, toSquare: Square, afterMove: () -> Unit) {
        val currentSide = board.getPiece(pawnSquare).pieceSide
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog, null)

        val queenView = dialogView.findViewById<ImageView>(R.id.queen)
        val rookView = dialogView.findViewById<ImageView>(R.id.rook)
        val bishopView = dialogView.findViewById<ImageView>(R.id.bishop)
        val knightView = dialogView.findViewById<ImageView>(R.id.knight)

        // Set images based on the side using the drawableMap
        queenView.setImageResource(
            drawableMap[if (currentSide == Side.WHITE) Piece.WHITE_QUEEN else Piece.BLACK_QUEEN]
                ?: 0
        )
        rookView.setImageResource(
            drawableMap[if (currentSide == Side.WHITE) Piece.WHITE_ROOK else Piece.BLACK_ROOK] ?: 0
        )
        bishopView.setImageResource(
            drawableMap[if (currentSide == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP]
                ?: 0
        )
        knightView.setImageResource(
            drawableMap[if (currentSide == Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT]
                ?: 0
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.setCanceledOnTouchOutside(false)
        // Listener to handle promotion piece selection
        val listener = View.OnClickListener { view ->
            val selectedPiece = when (view.id) {
                R.id.queen -> if (currentSide == Side.WHITE) Piece.WHITE_QUEEN else Piece.BLACK_QUEEN
                R.id.rook -> if (currentSide == Side.WHITE) Piece.WHITE_ROOK else Piece.BLACK_ROOK
                R.id.bishop -> if (currentSide == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
                R.id.knight -> if (currentSide == Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
                else -> null
            }
            selectedPiece?.let {
                val promotionMove = Move(pawnSquare, toSquare, it)
                board.doMove(promotionMove)
                afterMove()
            }
            dialog.dismiss()
        }

        // Assigning click listeners to image views
        queenView.setOnClickListener(listener)
        rookView.setOnClickListener(listener)
        bishopView.setOnClickListener(listener)
        knightView.setOnClickListener(listener)

        dialog.show()
    }


    data class Selection(val p: Piece, val s: Square)

    private var selection: Selection? = null;

    private fun clearHighlights() {
        for (i in 0 until 8) {
            for (j in 0 until 8) {
                val frameLayout = getViewFromGrid(binding.chessBoard, i, j) as FrameLayout
                val squareImageView = frameLayout.getChildAt(0) as ImageView
                squareImageView.setImageResource(if ((i + j) % 2 == 1) R.drawable.light_square else R.drawable.dark_square)
                squareImageView.setOnClickListener(null)  // Remove old click listeners
            }
        }
    }

    private val drawableMap = mapOf(
        Pair(Piece.WHITE_PAWN, R.drawable.white_pawn),
        Pair(Piece.WHITE_KNIGHT, R.drawable.white_knight),
        Pair(Piece.WHITE_BISHOP, R.drawable.white_bishop),
        Pair(Piece.WHITE_ROOK, R.drawable.white_rook),
        Pair(Piece.WHITE_QUEEN, R.drawable.white_queen),
        Pair(Piece.WHITE_KING, R.drawable.white_king),
        Pair(Piece.BLACK_PAWN, R.drawable.black_pawn),
        Pair(Piece.BLACK_KNIGHT, R.drawable.black_knight),
        Pair(Piece.BLACK_BISHOP, R.drawable.black_bishop),
        Pair(Piece.BLACK_ROOK, R.drawable.black_rook),
        Pair(Piece.BLACK_QUEEN, R.drawable.black_queen),
        Pair(Piece.BLACK_KING, R.drawable.black_king),
        Pair(Piece.NONE, 0)  // Assuming no drawable for empty squares
    )

    private fun updatePieces() {
        for (rank in Rank.values().reversed().subList(1, 9)) { // Start from rank 8 to rank 1
            for (file in File.values().toList().subList(0, 8)) { // Start from file A to file H
                val square = Square.encode(rank, file)
                val piece = board.getPiece(square)
                val (rowIndex, colIndex) = translateSquareToBoardIndex(square)
                val frameLayout =
                    getViewFromGrid(binding.chessBoard, rowIndex, colIndex) as FrameLayout

                val pieceImageView =
                    frameLayout.getChildAt(1) as ImageView  // Piece ImageView is assumed to be the second child
                val squareImageView = frameLayout.getChildAt(0) as ImageView

                val drawableId = drawableMap[piece] ?: 0
                if (drawableId > 0) {
                    if (piece.pieceType == PieceType.KING
                        &&  board.isKingAttacked
                        && piece.pieceSide == board.sideToMove) {
                        pieceImageView.setImageResource(R.drawable.red_king)
                    }
                    else {
                        pieceImageView.setImageResource(drawableId)
                    }
                    if (piece.pieceSide == playerSide) {
                        squareImageView.setOnClickListener {
                            selection = Selection(piece, square);
                            render();
                            Log.d(
                                "asdfasd",
                                "clicked piece"
                            )
                        }
                    }
                } else {
                    pieceImageView.setImageDrawable(null)  // Clear the ImageView if no piece is present
                }
            }
        }
    }


    private fun translateSquareToBoardIndex(square: Square): Pair<Int, Int> {
        // Implement the translation logic here. This will depend on how you've mapped the squares.
        // For a standard 8x8 chess board:
        val rowIndex = square.rank.ordinal// if rank 1 is at the bottom in your app
        val colIndex = square.file.ordinal // if file A is on the left in your app
        return Pair(rowIndex, colIndex)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)


        return binding.root
    }

    private val viewModel: MainViewModel by activityViewModels()

    private var username: String = ""
    private var uid: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).supportActionBar?.hide()

        viewModel.getCurrentUser().observe(viewLifecycleOwner) {
            username = it.name
            uid = it.uid
        }

        binding.clockBottom.text = formatTime(time)
        binding.clockTop.text = formatTime(time)
        gameId = arguments?.getString("gameId")

        if(gameId == null) {
            gameId = UUID.randomUUID().toString()
        }
        else {
            isVsPlayer = true
        }

        if(arguments?.getString("side") == "black") {
            playerSide  = Side.BLACK
        }
        else if(arguments?.getString("side") == "white") {
            playerSide = Side.WHITE
        }


        databaseReference = FirebaseDatabase.getInstance().getReference("games/$gameId")

        populateChessBoard()
        setupFirebaseGameListener()
        if (isVsPlayer) {
            initializeNewGame()
        } else {
            initializeNewGameAi()
        }


        binding.buttonResign.setOnClickListener {
            if (board.sideToMove != playerSide) {
                return@setOnClickListener
            }
            if (board.sideToMove == Side.BLACK) {
                gameOver("White wins by opposing resignation")
                gameOverUpdate("White wins by opposing resignation")
            }
            else {
                gameOver("Black wins by opposing resignation")
                gameOverUpdate("Black wins by opposing resignation")
            }
        }
        render()

    }

    private fun populateChessBoard() {
        board.loadFromFen(initialFen)
        val size = 8  // Size of the grid (chessboard is 8x8)
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val boardDimension = minOf(screenWidth, screenHeight) * 0.95
        squareSize = (boardDimension / size).toInt()  // Cast to int for use in layout params

        binding.chessBoard.apply {
            setPadding(0, 0, 0, 0)
            removeAllViews()

            for (i in 0 until size) {
                for (j in 0 until size) {
                    val frameLayout = FrameLayout(requireContext()).apply {
                        layoutParams = GridLayout.LayoutParams(
                            GridLayout.spec(i, GridLayout.FILL, 1f),
                            GridLayout.spec(j, GridLayout.FILL, 1f)
                        ).apply {
                            width = squareSize
                            height = squareSize
                        }
                    }

                    val squareImageView = ImageView(requireContext()).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        setImageResource(if ((i + j) % 2 == 1) R.drawable.dark_square else R.drawable.light_square)
                    }

                    val pieceImageView = ImageView(requireContext()).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        // Initially, no piece image
                    }

                    frameLayout.addView(squareImageView)
                    frameLayout.addView(pieceImageView)  // Piece ImageView is on top
                    addView(frameLayout)
                }
            }
        }

    }


    fun getViewFromGrid(gridLayout: GridLayout, row: Int, col: Int): View? {
        // Calculate the index based on the row and column
        val index = 63 - (row * gridLayout.columnCount + (7 - col))
        // Return the view at the calculated index
        return gridLayout.getChildAt(index)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
