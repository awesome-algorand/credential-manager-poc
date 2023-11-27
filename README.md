# Overview

- [Sequence Diagram](./SEQUENCE.md)
- [Decisions](./decisions/README.md)

Android 14 introduces `CredentialProviderService` and the ability for third-parties to manage passkey credentials.
This project is a reference implementation of a CredentialManager application.

## Prerequisites

- Google Account? (Maybe optional)
- Android 14 Device with Play Services and Screen Lock
- Google Chrome on Desktop


## Getting Started

Install NuAuth Credential Manager on the Android 14 device and ensure that the Google Account is active on the device. 
Also ensure the same account is used in the Google Chrome instance on the Desktop. 
Once the accounts are active, head to https://webauthn.io and register the device. 
During the registration process, it will look for registration of a phone via WebAuthn and Google caBLE. 

If no devices have been registered, it will say "Use a phone or tablet", otherwise it will be a list of devices with the option to "Use a different phone or tablet". 
Ensure at least one Android 14 device is registered to the Google Account. 
Once the device is registered, it can be used with Google's caBLE protocol for account registration/authentication (verified only on Linux at the time of writing).

## Compatibility Matrix

- GPM: Google Password Manager
- USK: USB Security Key

| Operating System           | Browser | Roaming Authenticator | NuAuth Status | Verified By |
|----------------------------|---------|-----------------------|---------------|-------------|
| Linux (Ubuntu 20.04.3 LTS) | Chrome  | Android 14 + NuAuth   | ✅             | @PhearZero  |
| Linux (Ubuntu 20.04.3 LTS) | Edge    | Android 14 + NuAuth   | ✅             | @PhearZero  |
| Linux (Ubuntu 20.04.3 LTS) | Brave   | Android 14 + NuAuth   | ✅             | @PhearZero  |
| Linux (Ubuntu 20.04.3 LTS) | Firefox | Only some USK         | ❌             | @PhearZero  |
| Windows 11                 | Chrome  | GPM & USK             | ❌             | @PhearZero  |



## Compatibility Tracking Links

- [Mozilla Passkey Support](https://connect.mozilla.org/t5/ideas/support-webauthn-passkeys/idi-p/14069)
