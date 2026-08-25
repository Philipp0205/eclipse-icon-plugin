#!/usr/bin/env bash
# Idempotent Cloud Agent bootstrap for the Per-Workspace Eclipse Icon Plugin.
#
# 1. Downloads the git-ignored third-party libraries (Batik) onto the bundle
#    classpath.
# 2. Builds the plugin headlessly with the Tycho/Maven build.
# 3. Provisions a full Eclipse (RCP/RAP package with PDE) so the plugin can be
#    run and developed, and deploys the freshly built jar into its dropins.
#
# Safe to re-run: downloads are skipped when already present and the Maven build
# is deterministic.
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${REPO_DIR}"

# Tycho 4.x requires Maven >= 3.9. The base image may ship an older Maven (or
# none), so ensure a suitable one is available without requiring root.
ensure_maven() {
  local need_install=1
  if command -v mvn >/dev/null 2>&1; then
    local ver
    ver="$(mvn -v 2>/dev/null | sed -n 's/^Apache Maven \([0-9.]*\).*/\1/p' | head -n1)"
    local major minor
    major="${ver%%.*}"
    minor="$(printf '%s' "${ver}" | cut -d. -f2)"
    if [[ -n "${major}" ]] && { [[ "${major}" -gt 3 ]] || { [[ "${major}" -eq 3 ]] && [[ "${minor:-0}" -ge 9 ]]; }; }; then
      need_install=0
    fi
  fi
  if [[ "${need_install}" -eq 0 ]]; then
    echo "Using $(mvn -v | head -n1)"
    return
  fi
  local mvn_version="3.9.9"
  local mvn_home="${HOME}/tools/apache-maven-${mvn_version}"
  if [[ ! -x "${mvn_home}/bin/mvn" ]]; then
    echo "Installing Apache Maven ${mvn_version} into ${mvn_home}"
    mkdir -p "${HOME}/tools"
    local tmp
    tmp="$(mktemp -d)"
    curl -fsSL --retry 4 --retry-delay 3 \
      -o "${tmp}/maven.tar.gz" \
      "https://archive.apache.org/dist/maven/maven-3/${mvn_version}/binaries/apache-maven-${mvn_version}-bin.tar.gz"
    tar -xzf "${tmp}/maven.tar.gz" -C "${HOME}/tools"
    rm -rf "${tmp}"
  fi
  export PATH="${mvn_home}/bin:${PATH}"
  echo "Using $(mvn -v | head -n1)"
}

echo "==> [1/3] Downloading plugin libraries"
./scripts/download-libs.sh

echo "==> [2/3] Building plugin (Tycho)"
ensure_maven
mvn -B clean package

echo "==> [3/3] Provisioning Eclipse and deploying plugin"
./scripts/provision-eclipse.sh

INSTALL_ROOT="${ECLIPSE_INSTALL_ROOT:-${HOME}/tools}"
ECLIPSE_HOME="${INSTALL_ROOT}/eclipse"
PLUGIN_JAR="$(ls -1 "${REPO_DIR}"/target/de.kurrle.eclipse.instanceicon-*.jar | head -n1)"
mkdir -p "${ECLIPSE_HOME}/dropins"
rm -f "${ECLIPSE_HOME}"/dropins/de.kurrle.eclipse.instanceicon-*.jar
cp "${PLUGIN_JAR}" "${ECLIPSE_HOME}/dropins/"

echo "==> Done. Built $(basename "${PLUGIN_JAR}") and deployed into ${ECLIPSE_HOME}/dropins"
