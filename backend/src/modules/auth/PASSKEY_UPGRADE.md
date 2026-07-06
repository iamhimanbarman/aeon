# Aeon Passkey Upgrade Path

Passkeys are intentionally not implemented yet. The current auth schema is ready for them through `auth_identities.provider = 'passkey'`.

Production implementation should add:

- A WebAuthn dependency with Android credential-manager support.
- `passkey_credentials` table keyed by `user_id` and WebAuthn credential id.
- Challenge tables for register and login ceremonies.
- `POST /auth/passkey/register/start`
- `POST /auth/passkey/register/finish`
- `POST /auth/passkey/login/start`
- `POST /auth/passkey/login/finish`
- Security events for register, login, failed assertion, and credential removal.

Do not store raw challenge secrets or private key material. Store only public credential data required by WebAuthn verification.
