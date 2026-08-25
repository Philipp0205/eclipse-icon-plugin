#!/usr/bin/env bash
# Downloads and extracts an Eclipse IDE (RCP/RAP package, which bundles the
# Plug-in Development Environment) so the plugin can actually be run and
# developed. This is a convenience for the Cloud Agent development environment:
# it gives a full Eclipse that already contains org.eclipse.ui / swt / jface /
# core.resources, i.e. everything the bundle requires at runtime.
#
# Idempotent: if the target Eclipse already exists it is left untouched.
set -euo pipefail

ECLIPSE_RELEASE="2024-12"
ECLIPSE_PACKAGE="rcp"
ARCHIVE="eclipse-${ECLIPSE_PACKAGE}-${ECLIPSE_RELEASE}-R-linux-gtk-x86_64.tar.gz"
URL="https://download.eclipse.org/technology/epp/downloads/release/${ECLIPSE_RELEASE}/R/${ARCHIVE}"

INSTALL_ROOT="${ECLIPSE_INSTALL_ROOT:-${HOME}/tools}"
ECLIPSE_HOME="${INSTALL_ROOT}/eclipse"

if [[ -x "${ECLIPSE_HOME}/eclipse" ]]; then
  echo "Eclipse already present at ${ECLIPSE_HOME}"
  exit 0
fi

mkdir -p "${INSTALL_ROOT}"
tmp="$(mktemp -d)"
trap 'rm -rf "${tmp}"' EXIT

echo "Downloading Eclipse ${ECLIPSE_PACKAGE} ${ECLIPSE_RELEASE} ..."
curl -fsSL --retry 4 --retry-delay 3 -o "${tmp}/${ARCHIVE}" "${URL}"

echo "Extracting to ${INSTALL_ROOT} ..."
tar -xzf "${tmp}/${ARCHIVE}" -C "${INSTALL_ROOT}"

echo "Eclipse installed at ${ECLIPSE_HOME}"
