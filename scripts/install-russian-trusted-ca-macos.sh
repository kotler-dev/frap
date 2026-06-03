#!/usr/bin/env bash
# Install Russian Trusted CA (NUC MinTsifry) on macOS.
# Official bundle: https://gu-st.ru/content/Other/doc/russiantrustedca.pem
# Instructions: https://www.gosuslugi.ru/crt , https://developers.sber.ru/help/certificates/how-to
set -euo pipefail

CERT_URL="https://gu-st.ru/content/Other/doc/russiantrustedca.pem"
WORKDIR="${TMPDIR:-/tmp}/frap-russian-trusted-ca"
BUNDLE="${WORKDIR}/russiantrustedca.pem"
LOGIN_KC="${HOME}/Library/Keychains/login.keychain-db"

mkdir -p "${WORKDIR}"
echo "[nuc] downloading ${CERT_URL}"
curl -fsSL -o "${BUNDLE}" "${CERT_URL}"

awk 'BEGIN{n=0} /BEGIN CERT/{n++; f=sprintf("'"${WORKDIR}"'/cert-%02d.pem",n)} {print > f} /END CERT/{close(f)}' "${BUNDLE}"

echo "[nuc] importing to login keychain (user)"
for f in "${WORKDIR}"/cert-02.pem "${WORKDIR}"/cert-01.pem; do
  security add-trusted-cert -d -r trustRoot -k "${LOGIN_KC}" "${f}" 2>/dev/null || \
    security import "${f}" -k "${LOGIN_KC}" -T /usr/bin/security -T /usr/bin/curl 2>/dev/null || true
done

echo "[nuc] opening bundle in Keychain Access — set SSL trust to Always Trust if prompted"
open "${BUNDLE}"

echo ""
echo "[nuc] optional: install for all users (requires admin password):"
echo "  sudo security add-trusted-cert -d -r trustRoot -p ssl -p basic -k /Library/Keychains/System.keychain ${WORKDIR}/cert-02.pem"
echo "  sudo security add-trusted-cert -d -r trustAsRoot -p ssl -p basic -k /Library/Keychains/System.keychain ${WORKDIR}/cert-01.pem"
echo ""
echo "[nuc] verify in Keychain Access: Certificates → Russian Trusted Root CA / Sub CA"
echo "[nuc] verify site: open https://www.sberbank.ru in Chrome or Yandex"
echo "[nuc] C013 explore: FRAP uses Yandex/Chrome — bundled Playwright Chromium needs separate NSS import"
