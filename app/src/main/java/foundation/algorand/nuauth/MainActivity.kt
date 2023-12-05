package foundation.algorand.nuauth

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import foundation.algorand.nuauth.credential.db.CredentialDatabase
import foundation.algorand.nuauth.databinding.ActivityMainBinding
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: CredentialDatabase
    companion object {
        val TAG = "MainActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate($intent)")
        lifecycleScope.launch {
            db = CredentialDatabase.getInstance(this@MainActivity)
            val credentials = db.credentialDao().getAll()
            credentials.collect() { credentialList ->
                Log.d(TAG, "db: $credentialList")
                val stuff = credentialList.map {
                    val user = it.userHandle
                    val origin = it.origin
                    "$user@$origin"
                }
                val listView = findViewById<ListView>(R.id.listView)
                val adapter: ArrayAdapter<*> = ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_list_item_1,  android.R.id.text1, stuff)
                listView.adapter = adapter
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }
}
