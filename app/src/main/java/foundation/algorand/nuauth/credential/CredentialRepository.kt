package foundation.algorand.nuauth.credential

import android.content.Context
import android.security.keystore.KeyProperties
import android.util.Log
import foundation.algorand.nuauth.credential.db.Credential
import foundation.algorand.nuauth.credential.db.CredentialDatabase
import foundation.algorand.nuauth.webauthn.WebAuthnUtils
import java.math.BigInteger
import java.security.*
import java.security.interfaces.ECPublicKey
import java.security.spec.*


interface CredentialRepository {
    val keyStore: KeyStore
    var db: CredentialDatabase
    suspend fun saveCredential(context: Context, credential: Credential)
    fun getDatabase(context: Context): CredentialDatabase
    fun generateCredentialId(): ByteArray
    fun getKeyPair(context: Context): KeyPair
    fun getKeyPair(context: Context, credentialId: ByteArray): KeyPair

    fun getPublicKeyFromKeyPair(keyPair: KeyPair?): ByteArray
}
fun CredentialRepository(): CredentialRepository = Repository()
class Repository(): CredentialRepository {
    override var keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
    private var generator: KeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)
    override lateinit var db: CredentialDatabase
    init {
        keyStore.load(null)
    }
    companion object {
        const val TAG = "CredentialRepository"
    }
    override suspend fun saveCredential(context: Context, credential: Credential) {
        Log.d(TAG, "saveCredential($credential)")
        getDatabase(context)
        db.credentialDao().insertAll(credential)
    }
    override fun getDatabase(context: Context): CredentialDatabase {
        Log.d(TAG, "getDatabase($context)")
        if(!::db.isInitialized) {
            db = CredentialDatabase.getInstance(context)
        }
        return db
    }
    override fun generateCredentialId(): ByteArray {
        Log.d(TAG, "generateCredentialId()")
        val credentialId = ByteArray(32)
        SecureRandom().nextBytes(credentialId)
        return credentialId
    }
    fun getKeyPairFromDatabase(context: Context, credentialId: ByteArray): KeyPair? {
        Log.d(TAG, "getKeyPairFromDatabase()")
        getDatabase(context)
        val credential = db.credentialDao().findById(WebAuthnUtils.b64Encode(credentialId))
        if (credential != null) {
            val publicKeyBytes = WebAuthnUtils.b64Decode(credential.publicKey)
            val privateKeyBytes = WebAuthnUtils.b64Decode(credential.privateKey)
            val factory = KeyFactory.getInstance("EC")
            val publicKey = factory.generatePublic(X509EncodedKeySpec(publicKeyBytes))
            val privateKey = factory.generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
            return KeyPair(publicKey, privateKey)
        }
        return null
    }
    override fun getKeyPair(context: Context): KeyPair{
        return getKeyPair(context, generateCredentialId())
    }
    override fun getKeyPair(context:Context, credentialId: ByteArray): KeyPair {
        Log.d(TAG, "getKeyPair($context, $credentialId)")
        val savedKeyPair = getKeyPairFromDatabase(context, credentialId)
        if (savedKeyPair != null) {
            return savedKeyPair
        }
        generator.initialize(ECGenParameterSpec("secp256r1"))
        return generator.generateKeyPair()
    }


    /**
     * Thank you https://developers.kddi.com/blog/2esxXGTcSBSaGLTJO0dC67
     */
    override fun getPublicKeyFromKeyPair(keyPair: KeyPair?): ByteArray {
        // credentialPublicKey CBOR
        if (keyPair==null) return ByteArray(0)
        if (keyPair.public !is ECPublicKey) return ByteArray(0)

        val ecPubKey = keyPair.public as ECPublicKey
        val ecPoint: ECPoint = ecPubKey.w

        // for now, only covers ES256
        if (ecPoint.affineX.bitLength() > 256 || ecPoint.affineY.bitLength() > 256) return ByteArray(0)

        val byteX = bigIntToByteArray32(ecPoint.affineX)
        val byteY = bigIntToByteArray32(ecPoint.affineY)

        // refer to RFC9052 Section 7 for details
        return "A5010203262001215820".chunked(2).map { it.toInt(16).toByte() }.toByteArray() + byteX+ "225820".chunked(2).map { it.toInt(16).toByte() }.toByteArray() + byteY

    }
    private fun bigIntToByteArray32(bigInteger: BigInteger):ByteArray{
        var ba = bigInteger.toByteArray()

        if(ba.size < 32) {
            // append zeros in front
            ba = ByteArray(32) + ba
        }
        // get the last 32 bytes as bigint conversion sometimes put extra zeros at front
        return ba.copyOfRange(ba.size - 32, ba.size)

    }
}
