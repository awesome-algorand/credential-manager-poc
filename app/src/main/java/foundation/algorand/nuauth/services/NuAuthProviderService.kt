package foundation.algorand.nuauth.services

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.OutcomeReceiver
import android.os.CancellationSignal
import android.provider.ContactsContract.Directory.PACKAGE_NAME
import android.util.Log
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import androidx.credentials.provider.PublicKeyCredentialEntry
import foundation.algorand.nuauth.R

class NuAuthProviderService: CredentialProviderService() {
    companion object {
        const val PASSKEY_ID = "Algorand Wallet"
        const val FAKE_USERNAME = "bob"
        const val TAG = "NuAuthProviderService"
        const val GET_PASSKEY_INTENT = 5
        const val CREATE_PASSKEY_INTENT = 6
    }

    /**
     * Handle Create Credential Requests
     */
    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>
    ) {
        val response: BeginCreateCredentialResponse? = processCreateCredentialRequest(request)
        if (response != null) {
            callback.onResult(response)
        } else {
            callback.onError(CreateCredentialUnknownException())
        }
    }

    /**
     * Process incoming Create Credential Requests
     */
    private fun processCreateCredentialRequest(request: BeginCreateCredentialRequest): BeginCreateCredentialResponse? {
        when (request) {
            is BeginCreatePublicKeyCredentialRequest -> {
                return handleCreatePasskeyQuery(request)
            }
        }
        return null
    }

    /**
     * Create a new PassKey
     */
    private fun handleCreatePasskeyQuery(
        request: BeginCreatePublicKeyCredentialRequest
    ): BeginCreateCredentialResponse {
        Log.d(TAG, request.toString())
        val createEntries: MutableList<CreateEntry> = mutableListOf()
        createEntries.add( CreateEntry(
            PASSKEY_ID,
            createNewPendingIntent(PASSKEY_ID, CREATE_PASSKEY_INTENT)
        )
        )
        return BeginCreateCredentialResponse(createEntries)
    }
    /**
     * Handle Get Credential Requests
     */
    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        try {
            callback.onResult(processGetCredentialRequest(request))
        } catch (e: GetCredentialException) {
            callback.onError(GetCredentialUnknownException())
        }
    }

    /**
     * Fake a list of available PublicKeyCredential Entries
     */
    private fun processGetCredentialRequest(request: BeginGetCredentialRequest): BeginGetCredentialResponse{
        Log.v(TAG, "processing GetCredentialRequest")

        // TODO: Manage PublicKeyCredentials in a secure storage
        val fakeKey = PublicKeyCredentialEntry.Builder(
            this@NuAuthProviderService,
            FAKE_USERNAME,
            createNewPendingIntent(FAKE_USERNAME, GET_PASSKEY_INTENT),
            // TODO: filter the request for PublicKeyCredentialOptions
            request.beginGetCredentialOptions[0] as BeginGetPublicKeyCredentialOption
        )
            .setIcon(Icon.createWithResource(this@NuAuthProviderService, R.mipmap.ic_launcher))
            .build()
        Log.v(TAG, "created fake key with $FAKE_USERNAME")
        // Return the Response with list of available Keys
        return BeginGetCredentialResponse(listOf(fakeKey))
    }
    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>
    ) {
        Log.d(TAG, "onClearCredentialStateRequest")
        TODO("Not yet implemented")
    }

    private fun createNewPendingIntent(action: String, requestCode: Int): PendingIntent{
        val intent = Intent(action).setPackage(PACKAGE_NAME)
        return PendingIntent.getActivity(
            applicationContext, requestCode,
            intent, PendingIntent.FLAG_IMMUTABLE
        )
    }
}