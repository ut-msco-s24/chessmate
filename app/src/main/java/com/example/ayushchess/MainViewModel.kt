package com.example.ayushchess

import androidx.lifecycle.ViewModel



class MainViewModel : ViewModel() {

    private var currentAuthUser = invalidUser






    // MainActivity gets updates on this via live data and informs view model
    fun setCurrentAuthUser(user: User) {
        currentAuthUser = user
    }


}
