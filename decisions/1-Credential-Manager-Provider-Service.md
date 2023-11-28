# Overview

Leverage the Android 14 `Credential Provider Service` and `Credential Manager`. Use only PublicKeyCredentials from valid FIDO2/WebAuthn enabled services

## Decisions

- Migrate from FIDO2 Client to Credential Manager
- Implement a Credential Provider Service
- Document working configurations

## Implementation

Credential Provider Service should act as a `roaming authenticator` and handle `create` and `get` operations for WebAuthn clients.

```mermaid
sequenceDiagram
    participant User
    participant WebAuthnWebsite
    participant FIDO2Server
    participant CredentialProviderService
    Note over User, CredentialProviderService: Create Credential
    User->>WebAuthnWebsite: Click Register
    WebAuthnWebsite->>FIDO2Server: GET /attestation/options
    FIDO2Server-->>FIDO2Server: Create Challenge
    FIDO2Server-->>WebAuthnWebsite: Return Create Credential Options
    WebAuthnWebsite->>CredentialProviderService: caBLE via navigator.credential.create(options)
    CredentialProviderService-->>CredentialProviderService: Display Create Credential Intent
    User->>CredentialProviderService: Approve with Biometrics
    CredentialProviderService-->>CredentialProviderService: Sign Challenge
    CredentialProviderService-->>WebAuthnWebsite: Return Create Credential Result
    WebAuthnWebsite->>FIDO2Server: POST /attestation/result
    FIDO2Server-->>FIDO2Server: Validate challenge
    FIDO2Server-->>WebAuthnWebsite: Ok Response
```


## Links

- [FIDO2 to Credential Manager Migration](https://developer.android.com/training/sign-in/fido2-migration)
- [Integrate with your provider](https://developer.android.com/training/sign-in/credential-provider)
- [Well Known Url Draft](https://github.com/ms-id-standards/MSIdentityStandardsExplainers/blob/main/PasskeyEndpointsWellKnownUrl/explainer.md#example-1)
- [Privileged Client](https://developer.android.com/training/sign-in/privileged-apps)

