package foundation.algorand.nuauth

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderCreateCredentialRequest
import foundation.algorand.nuauth.webauthn.AuthenticatorAttestationResponse
import androidx.credentials.webauthn.FidoPublicKeyCredential
import androidx.credentials.webauthn.PublicKeyCredentialCreationOptions
import androidx.lifecycle.*
import foundation.algorand.nuauth.credential.CredentialRepository
import foundation.algorand.nuauth.webauthn.WebAuthnUtils
import foundation.algorand.nuauth.credential.db.Credential
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECPoint

class CreatePasskeyViewModel(): ViewModel() {
    private val credentialRepository = CredentialRepository()
    companion object {
        const val TAG = "CreatePasskeyViewModel"
    }

    private val _origin = MutableLiveData<String>().apply {
        value = ""
    }
    val origin: LiveData<String> = _origin

    private fun setOrigin(o: String){
        _origin.value = o
    }
    private val _requestJson = MutableLiveData<String>().apply {
        value = ""
    }
    val requestJson: LiveData<String> = _requestJson

    private fun setRequestJson(jsonStr: String){
        _requestJson.value = JSONObject(jsonStr).toString(2)
    }

    private val _callingApp = MutableLiveData<String>().apply {
        value = ""
    }

    val callingApp: LiveData<String> = _callingApp
    private fun setCallingAppPackage(name: String){
        _callingApp.value = name
    }

    /**
     * Handle state and dispatch handlers
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun processCreatePasskey(context: Context, request: ProviderCreateCredentialRequest): Intent{
        val publicKeyRequest: CreatePublicKeyCredentialRequest =
            request.callingRequest as CreatePublicKeyCredentialRequest

        setOrigin(request.callingAppInfo.origin!!)
        setCallingAppPackage(request.callingAppInfo.packageName)
        setRequestJson(publicKeyRequest.requestJson)

        return handleCreatePasskey(context, request)
    }


    private fun populateEasyAccessorFields(json: String, rpid:String , keyPair: KeyPair, credentialId: ByteArray):String{
        Log.d("MyCredMan","=== populateEasyAccessorFields BEFORE === "+ json)
        val response = Json.decodeFromString<CreatePublicKeyCredentialResponseJson>(json)
        response.response.publicKeyAlgorithm = -7 // ES256
        response.response.publicKey = WebAuthnUtils.b64Encode(keyPair.public.encoded)
        response.response.authenticatorData = getAuthData(rpid, credentialId, keyPair)

        Log.d("MyCredMan","=== populateEasyAccessorFields AFTER === "+ Json.encodeToString(response))
        return Json.encodeToString(response)

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
    fun validateRpId(info: androidx.credentials.provider.CallingAppInfo, rpid:String): String{
        var origin = WebAuthnUtils.appInfoToOrigin(info)
        val rpIdForRexEx = rpid.replace(".","""\.""")
        if (Regex("""^https://([A-Za-z0-9\-.]*\.)?"""+rpIdForRexEx+"""/.?""").matches(origin)){
            origin = rpid
        }
        return origin
    }
    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun handleCreatePasskey(context: Context, request: ProviderCreateCredentialRequest): Intent {
        Log.d(TAG, "handleCreatePasskey($context, $request)")
        val publicKeyRequest: CreatePublicKeyCredentialRequest =
            request.callingRequest as CreatePublicKeyCredentialRequest
        val requestOptions = PublicKeyCredentialCreationOptions(publicKeyRequest.requestJson)

        // Generate a credentialId
//        val credentialId = credentialRepository.generateCredentialId()
        // Generate a credential key pair
//        val keyPair = credentialRepository.getKeyPair(context, credentialId)
        val credentialId = ByteArray(32)
        SecureRandom().nextBytes(credentialId)

        // Generate a credential key pair
        val spec = ECGenParameterSpec("secp256r1")
        val keyPairGen = KeyPairGenerator.getInstance("EC")
        keyPairGen.initialize(spec)
        val keyPair = keyPairGen.genKeyPair()

        val name = JSONObject(publicKeyRequest.requestJson).getJSONObject("user").get("name").toString()
        val rpid = validateRpId(request.callingAppInfo, requestOptions.rp.id)
        // Save passkey in your database as per your own implementation
        viewModelScope.launch {
            credentialRepository.saveCredential(
                context,
                Credential(
                    credentialId = WebAuthnUtils.b64Encode(credentialId),
                    userHandle = name,
                    origin = request.callingAppInfo.origin!!,
                    publicKey = WebAuthnUtils.b64Encode(keyPair.public.encoded),
                    privateKey = WebAuthnUtils.b64Encode(keyPair.private.encoded),
                    count = 0,
                )
            )
        }

        // Create AuthenticatorAttestationResponse object to pass to
        // FidoPublicKeyCredential

        val response = AuthenticatorAttestationResponse(
            requestOptions = requestOptions,
            credentialId = credentialId,
            credentialPublicKey = credentialRepository.getPublicKeyFromKeyPair(keyPair),
            origin = WebAuthnUtils.appInfoToOrigin(request.callingAppInfo),
            up = true,
            uv = false,
            be = false,
            bs = false,
            packageName = request.callingAppInfo.packageName,
//            clientDataHash = publicKeyRequest.clientDataHash
        )

        val credential = FidoPublicKeyCredential(
            rawId = credentialId, response = response, authenticatorAttachment = "platform"
        )
//        val credentialJson = populateEasyAccessorFields(credential.json(),rpid, keyPair,credentialId)
        val result = Intent()

        val createPublicKeyCredResponse =
            CreatePublicKeyCredentialResponse(credential.json())

               // Set the CreateCredentialResponse as the result of the Activity
        PendingIntentHandler.setCreateCredentialResponse(
            result, createPublicKeyCredResponse
        )
        return result
    }
}

@Serializable
private data class CreatePublicKeyCredentialResponseJson(
    //RegistrationResponseJSON
    val id:String,
    val rawId: String,
    val response: Response,
    val authenticatorAttachment: String?,
    val clientExtensionResults: EmptyClass = EmptyClass(),
    val type: String,
) {
    @Serializable
    data class Response(
        //AuthenticatorAttestationResponseJSON
        val clientDataJSON: String? = null,
        var authenticatorData: String? = null,
        val transports: List<String>? = arrayOf("internal").toList(),
        var publicKey: String? = null, // easy accessors fields
        var publicKeyAlgorithm: Long? =null, // easy accessors fields
        val attestationObject: String? // easy accessors fields
    )
    @Serializable
    class EmptyClass
}

@Serializable
data class GetPublicKeyCredentialRequestJson(
    val allowCredentials:Array<AllowCredential>? = null,
    val allowList:Array<AllowCredential>? = null,
    val challenge:String,
    val rpId:String,
    val userVerification: String,
    val timeout: Int? = null
) {
    @Serializable
    data class AllowCredential(
        val id: String,
        val transports:Array<String>,
        val type: String
    )
}
