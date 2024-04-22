import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ayushchess.GameInfo
import com.example.ayushchess.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainViewModel : ViewModel() {

    private var _currentUser = MutableLiveData<User>()
    private var _currentUserSide = MutableLiveData<String>()
    private var _currentUserGameId = MutableLiveData<String>()


    private val database = FirebaseDatabase.getInstance()

    // Sets the currently authenticated user from Firebase
    fun setCurrentUser(user: User) {
        _currentUser.postValue(user)
    }

    fun setGameId(id: String) {
        _currentUserGameId.postValue(id)
    }

    fun setCurrentUserSide(side: String) {
        _currentUserSide.postValue(side)
    }


    // Retrieves the currently authenticated user
    fun getCurrentUser(): LiveData<User> {
        return _currentUser
    }

    fun getCurrentUserSide(): LiveData<String> {
        return _currentUserSide
    }

    fun getGameId(): LiveData<String> {
        return _currentUserGameId
    }



    // Join lobby with a pending gameId status
    fun joinLobby(uid: String): LiveData<GameInfo> {
        val lobbyRef = database.getReference("/lobby/$uid")

        val gameIdLiveData = MutableLiveData<GameInfo>()

        // Set initial lobby data for this user
        lobbyRef.setValue(mapOf("uid" to uid, "gameId" to "pending", "side" to "unknown"))

        // Listen for changes to the gameId field
        lobbyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val gameId = snapshot.child("gameId").getValue(String::class.java)
                val side = snapshot.child("side").getValue(String::class.java)
                if (gameId != null && gameId != "pending" && side != null && side != "pending") {
                    gameIdLiveData.postValue(GameInfo(gameId, side))  // Post the actual gameId once updated
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Log error or handle it according to your need
            }
        })
        return gameIdLiveData
    }
}
