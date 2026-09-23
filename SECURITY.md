# Security Policy

OclAx is a public project and treats privacy and data protection as first-class requirements.

## Reporting a vulnerability

Do **not** publish secrets, private data, working credentials or sensitive exploit details in a public issue.

Use GitHub private vulnerability reporting when it is enabled for this repository.

If private reporting is not available, do not disclose sensitive technical details publicly. A private contact method can be configured later without exposing personal information in the repository.

## Secrets

Never commit real:
- API keys;
- passwords;
- tokens;
- private keys;
- signing keys/keystores;
- credentials;
- personal documents;
- private user data.

If a secret is committed, assume it is compromised and rotate/revoke it. Deleting it from the latest commit is not sufficient.

See `docs/SECURITY.md` for the project security model.
