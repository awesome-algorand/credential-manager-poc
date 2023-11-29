package foundation.algorand.nuauth

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import foundation.algorand.nuauth.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        val TAG = "MainActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
//        loginCredentialManager()
    }

    fun loginCredentialManager(){
        val credentialManager = CredentialManager.create(this@MainActivity)
        // Retrieves the user's saved password for your app from their
        // password provider.
        val getPasswordOption = GetPasswordOption()

        // Get passkey from the user's public key credential provider.
        val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(
            requestJson = JSONObject("{\n" +
                    "    \"challenge\": \"7JxImNaaWXUkQM3E8kcSPGyrj2Uc0nKUdWSudbA6eQU\",\n" +
                    "    \"timeout\": 1800000,\n" +
                    "    \"rpId\": \"nest-authentication-api.onrender.com\"\n" +
                    "}").toString()
        )

        val getCredRequest = GetCredentialRequest(
            listOf(getPasswordOption, getPublicKeyCredentialOption)
        )

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    // Use an activity-based context to avoid undefined system UI
                    // launching behavior.
                    context = this@MainActivity,
                    request = getCredRequest
                )
                handleSignIn(result)
            } catch (e : GetCredentialException) {
                handleFailure(e)
            }
        }

    }
    private fun handleFailure(e: GetCredentialException){
        Log.e(TAG, e.toString())
    }
    private fun handleSignIn(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is PublicKeyCredential -> {
                val responseJson = credential.authenticationResponseJson
                Log.d(TAG, responseJson)
                // Share responseJson i.e. a GetCredentialResponse on your server to
                // validate and  authenticate
            } else -> {
            // Catch any unrecognized credential type here.
            Log.e(TAG, "Unexpected type of credential")
        }
        }
    }
}
