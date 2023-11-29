package foundation.algorand.nuauth

import android.content.Intent
import androidx.biometric.BiometricPrompt
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.webauthn.AuthenticatorAttestationResponse
import androidx.credentials.webauthn.FidoPublicKeyCredential
import androidx.credentials.webauthn.PublicKeyCredentialCreationOptions
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.util.concurrent.Executor
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


/**
 * Authentication Provider Activity
 *
 * Called by {@link foundation.algorand.nuauth.services.NuAuthProviderService }
 *
 * @see <a href="https://developer.android.com/training/sign-in/credential-provider#handle-passkey-credential">Handle Passkey Credential</a>
 * @see <a href="https://developer.android.com/training/sign-in/credential-provider#passkeys-implement">Implement Passkey Credential</a>
 */
class CreatePasskeyActivity : AppCompatActivity() {
    companion object {
        const val TAG = "AuthProviderActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_passkey)
        Log.d(TAG, "onCreate($intent)")

        val beginRequest =
            PendingIntentHandler.retrieveBeginGetCredentialRequest(intent)
        Log.d(TAG, beginRequest.toString())
        val request = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
        Log.d(TAG, request.toString())

        val getRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
        Log.d(TAG, getRequest.toString())

        if (request != null && request.callingRequest is CreatePublicKeyCredentialRequest) {
            val publicKeyRequest: CreatePublicKeyCredentialRequest =
                request.callingRequest as CreatePublicKeyCredentialRequest
            createPasskey(
                publicKeyRequest.requestJson,
                request.callingAppInfo,
                publicKeyRequest.clientDataHash,
                null
            )
        }
    }

    private class MainThreadExecutor : Executor {
        private val handler: Handler = Handler(Looper.getMainLooper())
        override fun execute(r: Runnable) {
            handler.post(r)
        }
    }

    private fun createPasskey(
        requestJson: String,
        callingAppInfo: CallingAppInfo?,
        clientDataHash: ByteArray?,
        accountId: String?
    ) {
        Log.d(TAG, callingAppInfo.toString())
        val request = PublicKeyCredentialCreationOptions(requestJson)

        val biometricPrompt = BiometricPrompt(
            this,
            MainThreadExecutor(),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(
                errorCode: Int, errString: CharSequence
            ) {
                super.onAuthenticationError(errorCode, errString)
                finish()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                finish()
            }

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                super.onAuthenticationSucceeded(result)
                // Generate a credentialId
                val credentialId = ByteArray(32)
                SecureRandom().nextBytes(credentialId)

                // Generate a credential key pair
                val spec = ECGenParameterSpec("secp256r1")
                val keyPairGen = KeyPairGenerator.getInstance("EC")
                keyPairGen.initialize(spec)
                val keyPair = keyPairGen.genKeyPair()

                // Save passkey in your database as per your own implementation

                // Create AuthenticatorAttestationResponse object to pass to
                // FidoPublicKeyCredential

                val response = AuthenticatorAttestationResponse(
                    requestOptions = request,
                    credentialId = credentialId,
                    credentialPublicKey = keyPair.public.encoded,
                    origin = getAppOrigin(callingAppInfo!!),
                    up = true,
                    uv = true,
                    be = true,
                    bs = true
                )

                val credential = FidoPublicKeyCredential(
                    rawId = credentialId, response = response, authenticatorAttachment = "platform"
                )

                val intent = Intent("foundation.algorand.nuauth.GET_PASSKEY_ACTION")

                val createPublicKeyCredResponse =
                    CreatePublicKeyCredentialResponse(credential.json())

                // Set the CreateCredentialResponse as the result of the Activity
                PendingIntentHandler.setCreateCredentialResponse(
                    intent, createPublicKeyCredResponse
                )
//                setResult(Activity.RESULT_OK, intent)
//                finish()
            }
        }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Use your screen lock")
            .setSubtitle("Create passkey for ${request.rp.name}")
            .setNegativeButtonText("Cancel Maybe?")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
//                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun getAppOrigin(info: CallingAppInfo): String {
        val cert = info.signingInfo.apkContentsSigners[0].toByteArray()
//        val cert = "C7:87:AC:46:8B:A2:84:2E:04:FD:C7:B5:EB:C5:35:BC:EA:9C:98:9D:3D:4D:D4:89:48:27:BC:10:BD:6E:3D:99".toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val certHash = md.digest(cert)
        // This is the format for origin
        return "android:apk-key-hash:${Base64.encode(certHash)}"
    }
}
