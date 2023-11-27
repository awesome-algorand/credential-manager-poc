# Overview

Android 14 introduces `CredentialProviderService` and the ability for third-parties to manage passkey
credentials. This project is a reference for implementing a CredentialManager application.

## Prerequisites

- Google Account? (May be optional)
- Android 14 Device with Play Services and Screen Lock
- Google Chrome on Desktop


## Getting Started

Install NuAuth Credential Manager on the Android 14 device and ensure that the Google Account is active on the device. 
Also ensure the same account is used in the Google Chrome instance on the Desktop. 
Once the accounts are active, head to https://webauthn.io and register the device. 
During registration process it will look for registration of a phone via WebAuthn and Google caBLE. 

If no devices have been registered it will say "Use a phone or tablet", otherwise it will be a list of devices with the option to "Use a different phone or tablet". 
Ensure at least one Android 14 device is registered to the Google Account. 
Once the device is registered, it can be used with Google's caBLE protocol for account registration/authentication (verified only on Linux at the time of writing).

## Useful Links

- [Chrome Security Keys settings flag](chrome://settings/securityKeys/phones)
- [Credential Manager Service](https://developer.android.com/training/sign-in/credential-provider)
- [Migrating from FIDO2 to CredentialManager](https://developer.android.com/training/sign-in/fido2-migration)
- [Privileged Client](https://developer.android.com/training/sign-in/privileged-apps)
- [Well Known Url Draft](https://github.com/ms-id-standards/MSIdentityStandardsExplainers/blob/main/PasskeyEndpointsWellKnownUrl/explainer.md#example-1)