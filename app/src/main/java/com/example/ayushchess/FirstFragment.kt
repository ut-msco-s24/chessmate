package com.example.ayushchess

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.example.ayushchess.databinding.FragmentFirstBinding
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Square
import kotlin.properties.Delegates

class FirstFragment : Fragment() {

    private var squareSize by Delegates.notNull<Int>()
    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val board: Board = Board()

    fun render() {
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
            Pair(Piece.BLACK_KING, R.drawable.black_king)
        )

        Piece.allPieces.forEach { piece ->
            val locations = board.getPieceLocation(piece)
            locations.forEach { square ->
                // Translate the library's Square to your board's index
                val (rowIndex, colIndex) = translateSquareToBoardIndex(square)

                // Find the ImageView in your board's representation and set its image
                val drawableId = drawableMap[piece]
                placePiece(drawableId, piece, square, rowIndex, colIndex)
            }
        }

        val moves = board.legalMoves()
//        moves.filter { move ->  selection!!.s == move.from }
//        val squares = moves.map { }
    }

    fun onMovementSquareSelected() {
        render()
    }

    data class Selection(val p: Piece, val s: Square)
    var selection: Selection? = null;

    private fun placePiece(
        drawableId: Int?,
        piece: Piece,
        square: Square,
        rowIndex: Int,
        colIndex: Int
    ) {
        drawableId?.let {
            Log.d(
                "asdfsda",
                "Adding piece ${piece.value()} at square ${square.value()} $rowIndex $colIndex"
            )
            val imageView = ImageView(context).apply {
                layoutParams = GridLayout.LayoutParams(
                    GridLayout.spec(rowIndex, GridLayout.FILL, 1f),
                    GridLayout.spec(colIndex, GridLayout.FILL, 1f)
                ).apply {
                    width = squareSize
                    height = squareSize
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageDrawable(ContextCompat.getDrawable(requireContext(), it))
                setOnClickListener {

                    selection = Selection(piece, square)
                    onPieceSelected()
                    Log.d("asda", "Selected piece $selection")
                }
            }
            binding.chessBoard.addView(imageView)
        }
    }

    fun translateSquareToBoardIndex(square: Square): Pair<Int, Int> {
        // Implement the translation logic here. This will depend on how you've mapped the squares.
        // For a standard 8x8 chess board:
        val rowIndex = 7 - square.rank.ordinal // if rank 1 is at the bottom in your app
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

    private fun onPieceSelected() {
        // set the onclick listener for the squares the piece can move to/capture

        render()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        populateChessBoard()
        render()

    }

    private fun populateChessBoard() {
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        val size = 8 // Size of the grid (chessboard is 8x8)
        val displayMetrics = resources.displayMetrics

        // Calculate the size of each square based on the screen size,
        // ensuring the chessboard takes up no more than 50% of the width and height.
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val boardDimension = screenWidth.coerceAtMost(screenHeight) * 0.95
        squareSize = (boardDimension / size).toInt() // Cast to int for use in layout params

        // Set padding to the GridLayout
        binding.chessBoard.apply {
            // Remove padding/margins if any, set them to zero or handle as per your design
            setPadding(0, 0, 0, 0)
            layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
                width = boardDimension.toInt() // Cast to int for use in layout params
                height = boardDimension.toInt() // Cast to int for use in layout params
                // Set margins if any, to zero
                setMargins(0, 0, 0, 0)
            }
        }

        // Remove all views before adding new ones, to refresh the grid.
        binding.chessBoard.removeAllViews()

        for (i in 0 until size) {
            for (j in 0 until size) {
                val imageView = ImageView(context).apply {
                    layoutParams = GridLayout.LayoutParams(
                        GridLayout.spec(i, GridLayout.FILL, 1f),
                        GridLayout.spec(j, GridLayout.FILL, 1f)
                    ).apply {
                        width = squareSize
                        height = squareSize
                    }
                    scaleType = ImageView.ScaleType.FIT_XY
                    background = if ((i + j) % 2 == 0)
                        ContextCompat.getDrawable(requireContext(), R.drawable.light_square)
                    else
                        ContextCompat.getDrawable(requireContext(), R.drawable.dark_square)
                }
                binding.chessBoard.addView(imageView)

            }

        }


        // After all squares have been added, update the GridLayout's size based on the chessboard's dimension
        binding.chessBoard.layoutParams = binding.chessBoard.layoutParams.apply {
            this.width = boardDimension.toInt()  // Cast to int for use in layout params
            this.height = boardDimension.toInt()  // Cast to int for use in layout params
        }

        val v = getViewFromGrid(binding.chessBoard, 5, 5)
        v.visibility = View.GONE;


    }

    fun getViewFromGrid(gridLayout: GridLayout, row: Int, col: Int): View? {
        // Calculate the index based on the row and column
        val index = row * gridLayout.columnCount + col
        // Return the view at the calculated index
        return gridLayout.getChildAt(index)
    }






    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
