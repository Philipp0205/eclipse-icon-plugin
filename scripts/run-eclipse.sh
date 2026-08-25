#!/usr/bin/env bash
# Deploys the freshly built plugin into the provisioned Eclipse and launches it
# against a given workspace. Optionally seeds the per-workspace "Instance Icon"
# preferences so the effect (a distinct window/taskbar icon per workspace) is
# visible immediately on startup.
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

if [[ ! -x "${ECLIPSE_HOME}/eclipse" ]]; then
  echo "Eclipse not found at ${ECLIPSE_HOME}. Run scripts/provision-eclipse.sh first." >&2
  exit 1
fi

# Find the built bundle jar.
PLUGIN_JAR="$(ls -1 "${REPO_DIR}"/target/de.kurrle.eclipse.instanceicon-*.jar 2>/dev/null | head -n1 || true)"
if [[ -z "${PLUGIN_JAR}" ]]; then
  echo "Plugin jar not found in target/. Build it first: mvn -B clean package" >&2
  exit 1
fi

# Deploy into dropins (clear any previous copy first so re-runs stay clean).
mkdir -p "${ECLIPSE_HOME}/dropins"
rm -f "${ECLIPSE_HOME}"/dropins/de.kurrle.eclipse.instanceicon-*.jar
cp "${PLUGIN_JAR}" "${ECLIPSE_HOME}/dropins/"
echo "Deployed $(basename "${PLUGIN_JAR}") into dropins"

# Optionally seed this workspace's Instance Icon preferences.
if [[ -n "${OVERLAY_TEXT}" ]]; then
  PREF_DIR="${WORKSPACE}/.metadata/.plugins/org.eclipse.core.runtime/.settings"
  mkdir -p "${PREF_DIR}"
  cat >"${PREF_DIR}/de.kurrle.eclipse.instanceicon.prefs" <<EOF
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
