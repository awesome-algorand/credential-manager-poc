package foundation.algorand.nuauth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.credentials.webauthn.AuthenticatorAssertionResponse
import androidx.credentials.webauthn.FidoPublicKeyCredential
import androidx.credentials.webauthn.PublicKeyCredentialRequestOptions
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class GetPasskeyViewModel: ViewModel() {
    companion object {
        const val TAG = "GetPasskeyViewModel"
    }

    private val _origin = MutableLiveData<String>().apply {
        value = ""
    }
    val origin: LiveData<String> = _origin

    fun setOrigin(o: String){
        _origin.value = o
    }
    private val _requestJson = MutableLiveData<String>().apply {
        value = ""
    }
    val requestJson: LiveData<String> = _requestJson

    fun setRequestJson(jsonStr: String){
        _requestJson.value = JSONObject(jsonStr).toString(2)
    }

    private val _callingApp = MutableLiveData<String>().apply {
        value = ""
    }

    val callingApp: LiveData<String> = _callingApp
    fun setCallingAppPackage(name: String){
        _callingApp.value = name
    }
    fun processGetPasskey(request: ProviderGetCredentialRequest, requestInfo: Bundle?): Intent{
        Log.d(TAG, "processGetPasskey($request)")

        // Set Origin
        request.callingAppInfo.origin?.let { setOrigin(it) }

        // Set Calling App Package
        val packageName = request.callingAppInfo.packageName
        setCallingAppPackage(packageName)

        // Set Request JSON, TODO: Handle multiple options
        val option = request.credentialOptions[0] as GetPublicKeyCredentialOption
        setRequestJson(option.requestJson)
        return handleGetPasskey(request, requestInfo)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun handleGetPasskey(request: ProviderGetCredentialRequest, requestInfo: Bundle?): Intent{
        Log.d(TAG, "handleGetPasskey($request, $requestInfo)")
        val option = request.credentialOptions[0] as GetPublicKeyCredentialOption
        val requestOptions = PublicKeyCredentialRequestOptions(option.requestJson)
//        val credIdEnc = requestInfo!!.getString("credId")
        val credId = Base64.decode("BqvEZnea9fYG9xHPeeiAag")
        val origin = appInfoToOrigin(request.callingAppInfo)
        Log.d(TAG, origin)
        val response = AuthenticatorAssertionResponse(
            requestOptions = requestOptions,
            credentialId = credId,
            origin = appInfoToOrigin(request.callingAppInfo),
            up = true,
            uv = true,
            be = true,
            bs = true,
            userHandle = "es256".toByteArray(),
            packageName = "com.google.android.gms"
        )
        Log.d(TAG, response.toString())
        val spec = ECGenParameterSpec("secp256r1")
        val keyPairGen = KeyPairGenerator.getInstance("EC");
        keyPairGen.initialize(spec)
        val keyPair = keyPairGen.genKeyPair()

        val sig = Signature.getInstance("SHA256withECDSA");
        sig.initSign(keyPair.private)
        sig.update(response.dataToSign())
        response.signature = sig.sign()

        val credential = FidoPublicKeyCredential(
            rawId = credId, response = response, authenticatorAttachment = "cross-platform"
        )
        Log.d(TAG, credential.toString())
        val result = Intent()
        val passkeyCredential = PublicKeyCredential(credential.json())
        PendingIntentHandler.setGetCredentialResponse(
            result, GetCredentialResponse(passkeyCredential)
        )
        return result
    }
    @OptIn(ExperimentalEncodingApi::class)
    fun appInfoToOrigin(info: CallingAppInfo): String {
        val cert = info.signingInfo.apkContentsSigners[0].toByteArray()
        val md = MessageDigest.getInstance("SHA-256");
        val certHash = md.digest(cert)
        // This is the format for origin
        return "android:apk-key-hash:${Base64.encode(certHash)}"
    }
}
