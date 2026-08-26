#!/usr/bin/env bash
# Deploys the freshly built plugin into a provisioned Eclipse and launches it
# against a given workspace. Optionally seeds the per-workspace "Instance Icon"
# preferences so the effect (a distinct window/taskbar icon per workspace) is
# visible immediately on startup.
#
# The project is built by the Maven wrapper (./mvnw); this helper just deploys
# the resulting bundle jar into an Eclipse dropins/ folder and launches it, so
# the GUI plugin can be exercised interactively.
#
# Usage:
#   scripts/run-eclipse.sh <workspace-dir> [overlay-text]
#
# Examples:
#   scripts/run-eclipse.sh ~/ws-dev  DEV
#   scripts/run-eclipse.sh ~/ws-prod PROD
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INSTALL_ROOT="${ECLIPSE_INSTALL_ROOT:-${HOME}/tools}"
ECLIPSE_HOME="${INSTALL_ROOT}/eclipse"

WORKSPACE="${1:-${HOME}/eclipse-workspace}"
OVERLAY_TEXT="${2:-}"

# Bundle symbolic name (matches plugin/META-INF/MANIFEST.MF).
BUNDLE_ID="com.github.eclipse.instanceicon"

if [[ ! -x "${ECLIPSE_HOME}/eclipse" ]]; then
  echo "Eclipse not found at ${ECLIPSE_HOME}. Run scripts/provision-eclipse.sh first." >&2
  exit 1
fi

# Find the built bundle jar (built by ./mvnw into plugin/target). Build on demand.
find_jar() {
  ls -1 "${REPO_DIR}"/plugin/target/${BUNDLE_ID}-*.jar 2>/dev/null | grep -v -- '-sources' | head -n1 || true
}
PLUGIN_JAR="$(find_jar)"
if [[ -z "${PLUGIN_JAR}" ]]; then
  echo "Plugin jar not found; building with the Maven wrapper ..."
  (cd "${REPO_DIR}" && ./mvnw --batch-mode -pl plugin -am package)
  PLUGIN_JAR="$(find_jar)"
fi
if [[ -z "${PLUGIN_JAR}" ]]; then
  echo "Plugin jar still not found under plugin/target after build." >&2
  exit 1
fi

# Deploy into dropins (clear any previous copy first so re-runs stay clean).
mkdir -p "${ECLIPSE_HOME}/dropins"
rm -f "${ECLIPSE_HOME}"/dropins/${BUNDLE_ID}-*.jar
cp "${PLUGIN_JAR}" "${ECLIPSE_HOME}/dropins/"
echo "Deployed $(basename "${PLUGIN_JAR}") into dropins"

# Optionally seed this workspace's Instance Icon preferences (InstanceScope ->
# <workspace>/.metadata/.plugins/org.eclipse.core.runtime/.settings/<bundle>.prefs).
if [[ -n "${OVERLAY_TEXT}" ]]; then
  PREF_DIR="${WORKSPACE}/.metadata/.plugins/org.eclipse.core.runtime/.settings"
  mkdir -p "${PREF_DIR}"
  cat >"${PREF_DIR}/${BUNDLE_ID}.prefs" <<EOF
eclipse.preferences.version=1
instanceIconText=${OVERLAY_TEXT}
instanceColorPrimary=71,55,136
instanceColorSecondary=44,34,85
instanceColorAccent=247,148,30
instanceIconTextColor=0,0,0
instanceIconTextSize=40
EOF
  echo "Seeded overlay text '${OVERLAY_TEXT}' for workspace ${WORKSPACE}"
fi

echo "Launching Eclipse (workspace: ${WORKSPACE}) ..."
exec "${ECLIPSE_HOME}/eclipse" \
  -data "${WORKSPACE}" \
  -clean \
  -vmargs -Dorg.eclipse.swt.graphics.Resource.reportNonDisposed=false
