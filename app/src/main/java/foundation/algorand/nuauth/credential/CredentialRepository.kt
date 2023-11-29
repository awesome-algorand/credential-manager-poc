package foundation.algorand.nuauth.credential

import android.content.Context
import android.util.Log
import androidx.credentials.provider.CallingAppInfo
import androidx.fragment.app.Fragment
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

interface CredentialRepository {
    fun getKeyPair(): KeyPair
    fun getKeyPair(context: Context): KeyPair
    fun getKeyPair(fragment: Fragment): KeyPair
    fun getKeyPair(publicKey: String, privateKey: String): KeyPair
    fun appInfoToOrigin(info: CallingAppInfo): String
}
fun CredentialRepository(): CredentialRepository = Repository()
class Repository: CredentialRepository {
    companion object {
        const val TAG = "CredentialRepository"
        const val SHARED_PREFERENCE = "credential"
        const val CREDENTIAL_PREFERENCE_PUBLICKEY = "public"
        const val CREDENTIAL_PREFERENCE_PRIVATEKEY = "private"
        const val KEY_ALGO = "EC"
    }

    private var generator: KeyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGO)

    override fun getKeyPair(): KeyPair {
        Log.d(TAG, "getKeyPair()")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        return generator.generateKeyPair()
    }
    override fun getKeyPair(fragment: Fragment): KeyPair {
        Log.d(TAG, "getKeyPair($fragment)")
        return getKeyPair(fragment.requireContext())
    }
    @OptIn(ExperimentalEncodingApi::class)
    override fun getKeyPair(context: Context): KeyPair {
        Log.d(TAG, "getKeyPair($context)")
        val sharedPref = context.getSharedPreferences(SHARED_PREFERENCE, Context.MODE_PRIVATE)
        val publicKeyPref = sharedPref.getString(CREDENTIAL_PREFERENCE_PUBLICKEY, null)
        val privateKeyPref = sharedPref.getString(CREDENTIAL_PREFERENCE_PRIVATEKEY, null)


        return if(publicKeyPref === null || privateKeyPref === null){
            val keyPair = getKeyPair()
            // TODO: Find a more secure storage
            // Save to preferences, this is very insecure!!
            with (sharedPref.edit()){
                putString(CREDENTIAL_PREFERENCE_PUBLICKEY, Base64.encode(keyPair.public.encoded))
                putString(CREDENTIAL_PREFERENCE_PRIVATEKEY, Base64.encode(keyPair.private.encoded))
                apply()
            }
            keyPair
        } else {
            getKeyPair(publicKeyPref, privateKeyPref)
        }
    }
    @OptIn(ExperimentalEncodingApi::class)
    override fun getKeyPair(publicKey: String, privateKey: String): KeyPair {
        Log.d(TAG, "getKeyPair($publicKey, $privateKey)")
        val publicKeyBytes = Base64.decode(publicKey)
        val privateKeyBytes = Base64.decode(privateKey)
        val publicKeySpec = KeyFactory.getInstance(KEY_ALGO).generatePublic(X509EncodedKeySpec(publicKeyBytes))
        val privateKeySpec = KeyFactory.getInstance(KEY_ALGO).generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
        return KeyPair(publicKeySpec, privateKeySpec)
    }
    @OptIn(ExperimentalEncodingApi::class)
    override fun appInfoToOrigin(info: CallingAppInfo): String {
        val cert = info.signingInfo.apkContentsSigners[0].toByteArray()
        val md = MessageDigest.getInstance("SHA-256");
        val certHash = md.digest(cert)
        // This is the format for origin
        return "android:apk-key-hash:${Base64.encode(certHash)}"
    }
}
