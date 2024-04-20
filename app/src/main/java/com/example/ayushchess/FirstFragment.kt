package com.example.ayushchess

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
import kotlin.properties.Delegates

class FirstFragment : Fragment() {

    private var isWhiteTurn: Boolean = true
    val minutes: Long = 3
    val time: Long = 60 * minutes * 1000
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
                    val sideWin = if (side == Side.WHITE) Side.BLACK else Side.WHITE
                    gameOver("${if (side == Side.WHITE) "Black" else "White"} wins on time")
                }
            }
        }
    }

    private val board: Board = Board()

    private fun onMoveMade() {
        if (isWhiteTurn) {
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
        isWhiteTurn = !isWhiteTurn
    }

    private fun render() {
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
        squareImageView.setOnClickListener {
            onMoveMade()
            Log.d("asdfads", "click")
            val targetSquare = Square.squareAt(rowIndex * 8 + colIndex)

            // Check if the move is a pawn promotion
            fun isPromotionMove(from: Square, to: Square): Boolean {
                val piece = board.getPiece(from)
                return (piece == Piece.WHITE_PAWN && to.rank == Rank.RANK_8) ||
                        (piece == Piece.BLACK_PAWN && to.rank == Rank.RANK_1)
            }
            selection?.let { sel ->
                if (isPromotionMove(sel.s, targetSquare)) {
                    showPromotionDialog(sel.s, targetSquare) // Handle promotion
                } else {
                    board.doMove(
                        Move(
                            sel.s,
                            Square.squareAt(rowIndex * 8 + colIndex)
                        )
                    )  // Pseudocode for move execution
                }

                selection = null
                render()
            }
        }
    }

    private fun formatTime(millis: Long): String {
        val minutes = (millis / 1000) / 60
        val seconds = (millis / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun gameOver(message: String = "") {

        whiteTimer?.cancel()
        blackTimer?.cancel()

        for (i in 0 until 8) {
            for (j in 0 until 8) {

                val v = getViewFromGrid(binding.chessBoard, i, j) as FrameLayout
                (v.getChildAt(0) as ImageView).setOnClickListener(null)
                (v.getChildAt(1) as ImageView).setOnClickListener(null)

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


    private fun showPromotionDialog(pawnSquare: Square, toSquare: Square) {
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
                render()
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

    var selection: Selection? = null;

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

    val drawableMap = mapOf(
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
                    if (piece.pieceSide == board.sideToMove) {
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


    fun translateSquareToBoardIndex(square: Square): Pair<Int, Int> {
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).supportActionBar?.hide()


        binding.clockBottom.text = formatTime(time)
        binding.clockTop.text = formatTime(time)
        populateChessBoard()
        binding.buttonResign.setOnClickListener {
            if (board.sideToMove == Side.BLACK) {
                gameOver("White wins by opposing resignation")
            }
            else {
                gameOver("Black wins by opposing resignation")
            }
        }
        render()

    }

    private fun populateChessBoard() {
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
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
