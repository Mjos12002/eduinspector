package com.example.inspectorappupdate.viewmodel.agent

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inspectorappupdate.model.agent.AgentStatsData
import com.example.inspectorappupdate.model.agent.StockCard
import com.example.inspectorappupdate.repository.agent.AgentRepository
import com.example.inspectorappupdate.repository.agent.AgentResult
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

// The agent's numbers and the printed cards they still hold.
@RequiresApi(Build.VERSION_CODES.O)
class AgentStatsViewModel : ViewModel() {

    private val repository = AgentRepository()

    private val _stats = MutableLiveData<AgentResult<AgentStatsData>?>(null)
    val stats: LiveData<AgentResult<AgentStatsData>?> = _stats

    private val _stock = MutableLiveData<List<StockCard>>(emptyList())
    val stock: LiveData<List<StockCard>> = _stock

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    fun load(token: String) {
        if (_loading.value == true) return
        _loading.value = true
        viewModelScope.launch {
            val stats = async { repository.stats(token) }
            val stock = async { repository.stock(token) }
            _stats.value = stats.await()
            (stock.await() as? AgentResult.Success)?.let { _stock.value = it.data }
            _loading.value = false
        }
    }
}
