package app.audionav.compass

import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    val heading = mutableIntStateOf(0)
    init {
        viewModelScope.launch {
            while(isActive) {
                heading.intValue = (heading.intValue + 1) % 360
                delay(1000)
            }
        }
    }
}