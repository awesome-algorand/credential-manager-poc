package foundation.algorand.nuauth

import android.content.Intent
import androidx.credentials.provider.PendingIntentHandler
import androidx.lifecycle.ViewModel
import kotlin.io.encoding.ExperimentalEncodingApi


class CreatePasskeyViewModel: ViewModel() {
    private fun validatePasskey(requestJson: String, origin: Any, packageName: String, uid: Any, username: Any, credId: Any, privateKey: Any) {

    }
    @OptIn(ExperimentalEncodingApi::class)
    fun handleGetCredentialIntent(intent: Intent){

       val request =
           PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
//        if (request != null && request.credentialOptions is GetPublicKeyCredentialOption) {
//
//        }
//
//        val publicKeyRequest =
//           getRequest.credentialOption as GetPublicKeyCredentialOption
//
//       val requestInfo = intent.getBundleExtra("CREDENTIAL_DATA")
//       val credIdEnc = requestInfo!!.getString("credId")
//
//// Get the saved passkey from your database based on the credential ID
//// from the publickeyRequest
//       val passkey = <your database>.getPasskey(credIdEnc)
//
//// Decode the credential ID, private key and user ID
//       val credId = Base64.encode(credIdEnc)
//       val privateKey = Base64.encode(passkey.credPrivateKey)
//       val uid = Base64.encode(passkey.uid)
//
//       val origin = appInfoToOrigin(getRequest.callingAppInfo)
//       val packageName = getRequest.callingAppInfo.packageName
//
//       validatePasskey(
//           publicKeyRequest.requestJson,
//           origin,
//           packageName,
//           uid,
//           passkey.username,
//           credId,
//           privateKey
//       )
   }
}
