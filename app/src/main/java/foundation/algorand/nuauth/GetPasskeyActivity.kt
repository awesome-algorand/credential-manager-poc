package foundation.algorand.nuauth

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.credentials.provider.PendingIntentHandler
import foundation.algorand.nuauth.databinding.ActivityGetPasskeyBinding

class GetPasskeyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGetPasskeyBinding
    val viewModel: GetPasskeyViewModel by viewModels()
    companion object {
        const val TAG = "GetPasskeyActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate($intent)")

        // Initialize Layout
        binding = ActivityGetPasskeyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.viewModel = viewModel

        // Handle Intent
        val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
        if (request != null) {
            val result = viewModel.processGetPasskey(request, intent.getBundleExtra("CREDENTIAL_DATA"))
            setResult(Activity.RESULT_OK, result)
        } else {
            binding.getPasskeyMessage.text = resources.getString(R.string.get_passkey_error)
        }
    }
}
