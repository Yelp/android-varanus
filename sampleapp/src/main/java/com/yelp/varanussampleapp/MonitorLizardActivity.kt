package com.yelp.varanussampleapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import com.yelp.android.varanus.shutoff.NetworkShutoffManager
import com.yelp.android.varanus.util.CoroutineScopeAndJob
import com.yelp.android.varanus.util.JobBasedScope
import com.yelp.varanus.sampleapp.R
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

const val url = "https://devnull-as-a-service.com/dev/null/varanus/"
const val dummy_text = " OMNOMNOM "
private const val LOGTAG = "VARANUS_MAIN"

private const val INSECT_SIZE = 1
private const val FRUIT_SIZE = 5
private const val FISH_SIZE = 10

class MonitorLizardActivity: AppCompatActivity(),
        CoroutineScopeAndJob by JobBasedScope(Dispatchers.IO){

    private lateinit var client: OkHttpClient
    private lateinit var alertIssuer: LogUploader
    lateinit var shutoffManager: NetworkShutoffManager
    private val textFields: HashMap<String, FoodInfoUpdater> = hashMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitor_lizard)
        alertIssuer = LogUploader(this)
        client = MonitorLizardOkhttpClientFactory.configureOkhttpClient(this, alertIssuer)
        setUpTextFields()

        setUpButton(R.id.insect_button, "insect", INSECT_SIZE)
        setUpButton(R.id.fruit_button, "fruit", FRUIT_SIZE)
        setUpButton(R.id.fish_button, "fish", FISH_SIZE)
    }

    private fun setUpButton(id: Int, foodName: String, size: Int) {
        findViewById<Button>(id).setOnClickListener {
            launch {
                val body = RequestBody.create("text/plain".toMediaType(), dummy_text.repeat(size))
                val request = Request.Builder().url(url + foodName).post(body).build()
                Log.d(LOGTAG, "Sending a network request of size ${body.contentLength()}" +
                        " for $foodName")
                client.newCall(request).execute()
            }
        }
    }

    private fun setUpTextFields() {
        textFields["insect"] = FoodInfoUpdater(
                R.id.insect_counter_text,
                R.id.insect_weight_text,
                R.string.insect_counter,
                R.string.insect_counter_weight)

        textFields["fruit"] = FoodInfoUpdater(
                R.id.fruit_counter_text,
                R.id.fruit_weight_text,
                R.string.fruit_counter,
                R.string.fruit_counter_weight)

        textFields["fish"] = FoodInfoUpdater(
                R.id.fish_counter_text,
                R.id.fish_weight_text,
                R.string.fish_counter,
                R.string.fish_counter_weight)

        textFields["total"] = FoodInfoUpdater(
                R.id.total_counter_text,
                R.id.total_weight_text,
                R.string.total_counter,
                R.string.total_counter_weight)
        updateText(alertIssuer.foodStats)
    }


    fun updateText(newData: Map<String, LogUploader.Counter>) {
        val endpointsShutOff = mutableListOf<String>()
        textFields.forEach { (endpoint, textField) ->
            textField.update(endpoint, newData)
            if (shutoffManager.shouldDropRequest(endpoint)) {
                endpointsShutOff.add(endpoint)
            }
        }
        val hungerStatus = findViewById<TextView>(R.id.lizard_status)

        hungerStatus.text =  when (endpointsShutOff.size) {
            0 -> getString(R.string.fed_not_too_much)
            1 -> getString(R.string.fed_too_much_specific, endpointsShutOff[0])
            else -> getString(R.string.fed_too_much_total)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_monitor_lizard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    inner class FoodInfoUpdater(
            counterId: Int,
            weightId: Int,
            private val counterStringId: Int,
            private val weightStringId: Int
    ) {
        private val counterTextView = findViewById<TextView>(counterId)
        private val weightTextView = findViewById<TextView>(weightId)

        fun update(endpoint: String, newData: Map<String, LogUploader.Counter>) {
            counterTextView.text = getString(counterStringId, newData[endpoint]?.count)
            weightTextView.text = getString(weightStringId, newData[endpoint]?.size)
        }
    }
}
