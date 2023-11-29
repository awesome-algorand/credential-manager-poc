package foundation.algorand.nuauth

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.credentials.provider.PendingIntentHandler
import foundation.algorand.nuauth.credential.CredentialRepository
import foundation.algorand.nuauth.databinding.ActivityGetPasskeyBinding

class GetPasskeyActivity : AppCompatActivity() {
    private val credentialRepository: CredentialRepository = CredentialRepository()
    private lateinit var binding: ActivityGetPasskeyBinding
    val viewModel: GetPasskeyViewModel by viewModels()
    companion object {
        const val TAG = "GetPasskeyActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate($intent)")
        val keyPair = credentialRepository.getKeyPair(this@GetPasskeyActivity)
        Log.d(CreatePasskeyActivity.TAG, "${keyPair.public}")
        // Initialize Layout
        binding = ActivityGetPasskeyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.viewModel = viewModel

        // Handle Intent
        val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
        if (request != null) {
            val result = viewModel.processGetPasskey(request, intent.getBundleExtra("CREDENTIAL_DATA"))
            Log.d(TAG, "result: $result")
            setResult(Activity.RESULT_OK, result)
            finish()
        } else {
            binding.getPasskeyMessage.text = resources.getString(R.string.get_passkey_error)
        }
    }
}
