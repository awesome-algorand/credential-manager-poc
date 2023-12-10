package foundation.algorand.nuauth

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderGetCredentialRequest
import foundation.algorand.nuauth.webauthn.AuthenticatorAssertionResponse
import androidx.credentials.webauthn.FidoPublicKeyCredential
import androidx.credentials.webauthn.PublicKeyCredentialRequestOptions
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import foundation.algorand.nuauth.credential.CredentialRepository
import foundation.algorand.nuauth.webauthn.WebAuthnUtils
import org.json.JSONObject
import java.security.*
import java.security.interfaces.ECPrivateKey
class GetPasskeyViewModel: ViewModel() {
    private val credentialRepository = CredentialRepository()
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
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun processGetPasskey(context: Context, request: ProviderGetCredentialRequest, requestInfo: Bundle?): Intent{
        Log.d(TAG, "processGetPasskey($request)")

        // Set Origin
        request.callingAppInfo.origin?.let { setOrigin(it) }

        // Set Calling App Package
        val packageName = request.callingAppInfo.packageName
        setCallingAppPackage(packageName)

        // Set Request JSON, TODO: Handle multiple options
        val option = request.credentialOptions[0] as GetPublicKeyCredentialOption
        setRequestJson(option.requestJson)
        return handleGetPasskey(context, request, requestInfo)
    }
    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun handleGetPasskey(context: Context, request: ProviderGetCredentialRequest, requestInfo: Bundle?): Intent{
        Log.d(TAG, "handleGetPasskey($request, $requestInfo)")
        val option = request.credentialOptions[0] as GetPublicKeyCredentialOption
        val requestOptions = PublicKeyCredentialRequestOptions(option.requestJson)
        Log.d(TAG, "requestOptions: ${option.requestJson}")
        val credIdEnc = requestInfo!!.getString("credentialId")
        Log.d(TAG, "credIdEnc: $credIdEnc")
        val credId = WebAuthnUtils.b64Decode(credIdEnc!!)
        val clientDataHash = option.requestData.getByteArray("androidx.credentials.BUNDLE_KEY_CLIENT_DATA_HASH")
        Log.d(TAG, "clientDataHash: ${clientDataHash!!.contentToString()}")
        val packageName = request.callingAppInfo.packageName
        val userHandle = requestInfo.getString("userHandle")
        val origin = WebAuthnUtils.appInfoToOrigin(request.callingAppInfo)
        Log.d(TAG, "origin: $origin")
        val response = AuthenticatorAssertionResponse(
            requestOptions = requestOptions,
            credentialId = credId,
//            origin = WebAuthnUtils.appInfoToOrigin(request.callingAppInfo),
            origin = "webauthn.io",
            up = true,
            uv = false,
            be = false,
            bs = false,
            userHandle = userHandle!!.toByteArray(),
//            packageName = packageName,
//            clientDataHash = clientDataHash
        )

//        response.authenticatorData = getAuthData(origin, credIdEnc, keyPair = keyPair)
        val keyPair = credentialRepository.getKeyPair(context, credId)

        //TODO: Fix signature issues
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(keyPair.private )
        sig.update(response.dataToSign())
        response.signature = sig.sign()
        Log.d(TAG, "signature: ${response.signature.contentToString()}")
        val credential = FidoPublicKeyCredential(
            rawId = credId, response = response, authenticatorAttachment = "platform"
        )
        Log.d(TAG, credential.toString())
        val result = Intent()
        Log.d(TAG, "credential: ${credential.json()}")
        val passkeyCredential = PublicKeyCredential(credential.json())
        val getCredResponse = GetCredentialResponse(passkeyCredential)
        Log.d(TAG, getCredResponse.toString())
        PendingIntentHandler.setGetCredentialResponse(
            result, getCredResponse
        )
        return result
    }

    private fun getAuthData(rpid:String, credentialRawId:ByteArray, keyPair: KeyPair ):String{
        val AAGUID = "00000000000000000000000000000000"
        check(AAGUID.length % 2 == 0) { "AAGUID Must have an even length" }

        val rpIdHash:ByteArray = MessageDigest.getInstance("SHA-256")
            .digest(rpid.toByteArray())

        val flags: ByteArray = byteArrayOf(0x5d.toByte())
        val signCount:ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        val aaguid = AAGUID.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
        val credentialIdLength:ByteArray = byteArrayOf(0x00, credentialRawId.size.toByte()) // = 20 bytes
        // val credentialId
        val credentialPublicKey:ByteArray =credentialRepository.getPublicKeyFromKeyPair(keyPair)

        val retVal = rpIdHash + flags + signCount + aaguid + credentialIdLength + credentialRawId + credentialPublicKey
        return WebAuthnUtils.b64Encode(retVal)
        //return "446A62A2EF738CC785FB325FE668786CFFBA2D895CB2985E9F6881332144EC73 5D 00000000 00000000000000000000000000000000 0014 E9D02198BBA75CDD9FD51C845CF4ED39C8DF5BCE A501020326200121582010FCE96113AFF9A663E985C8DB9C1BB638777C617071089EDB0F419CB9F99CE2225820E0E4F780F479955C2F1C1178C69A5AAB844A90F0C76DC6DBF81F5D72938391CD"
    }

}
