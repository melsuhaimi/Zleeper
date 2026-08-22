# Zleeper living-world recovery snapshot

This directory preserves the current durable implementation progress for `feat/living-world-progression` while GitHub Actions is intentionally disabled on the branch.

The active workflow `.github/workflows/android.yml` was removed in the same commit and preserved verbatim as `.github/workflows/android.yml.bak`. Do not restore the `.yml` extension until explicit CI approval is given.

## Snapshot contents

The recovery archive contains 44 source/content/test files from the current reconstructed implementation state. The complete path list is in `files.txt`.

The archive is stored as four Base64 text chunks to avoid binary-transfer limitations:

- `archive-00.b64`
- `archive-01.b64`
- `archive-02.b64`
- `archive-03.b64`

Reconstruct it with:

```sh
cat archive-00.b64 archive-01.b64 archive-02.b64 archive-03.b64 > zleeper_recovery_progress.tgz.b64
base64 -d zleeper_recovery_progress.tgz.b64 > zleeper_recovery_progress.tgz
sha256sum zleeper_recovery_progress.tgz
```

Expected SHA-256:

`9044493b16cd34417ad272e31bfab5c0698094d75fbd3facc2b66a7c3e3e2781`

Then extract:

```sh
tar -xzf zleeper_recovery_progress.tgz
```

This backup is a durability checkpoint, not a CI-validated release candidate. The authoritative Android/Compose/KSP verification remains gated behind explicit approval.
