#!/usr/bin/env bash
# Downloads the third-party libraries that are referenced on the bundle's
# Bundle-ClassPath (MANIFEST.MF) and .classpath but are intentionally kept out
# of version control (see .gitignore: *.jar). These jars must be present on
# disk before the plugin can be compiled or packaged.
#
# The script is idempotent: an existing, non-empty jar of the expected name is
# left untouched, so it is safe to re-run during environment setup.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)/lib"
MAVEN_CENTRAL="https://repo1.maven.org/maven2"

mkdir -p "${LIB_DIR}"

# Each entry: "<group-path> <artifact> <version>"
# The resulting jar is named "<artifact>-<version>.jar" to match .classpath /
# MANIFEST.MF Bundle-ClassPath entries.
BATIK_VERSION="1.17"
declare -a ARTIFACTS=(
  "org/apache/xmlgraphics batik-anim ${BATIK_VERSION}"
  "org/apache/xmlgraphics batik-awt-util ${BATIK_VERSION}"
  "org/apache/xmlgraphics batik-bridge ${BATIK_VERSION}"
  "org/apache/xmlgraphics batik-codec ${BATIK_VERSION}"
  "org/apache/xmlgraphics batik-constants ${BATIK_VERSION}"
  "org/apache/xmlgraphics batik-css ${BATIK_VERSION}"
  "org/apache/xmlgraphics batik-dom ${BATIK_VERSION}"
  "org/apache/xmlgraphics batik-ext ${BATIK_VERSION}"
  "org/apache/xmlgraphics batik-gvt ${BATIK_VERSION}"
  "org/apache/xmlgraphics batik-i18n ${BATIK_VERSION}"
  "org/apache/xmlgraphics batik-parser ${BATIK_VERSION}"
  "org/apache/xmlgraphics batik-script ${BATIK_VERSION}"
  "org/apache/xmlgraphics batik-shared-resources ${BATIK_VERSION}"
  "org/apache/xmlgraphics batik-svg-dom ${BATIK_VERSION}"
  "org/apache/xmlgraphics batik-transcoder ${BATIK_VERSION}"
  "org/apache/xmlgraphics batik-util ${BATIK_VERSION}"
  "org/apache/xmlgraphics batik-xml ${BATIK_VERSION}"
  "org/apache/xmlgraphics xmlgraphics-commons 2.9"
  "xml-apis xml-apis 1.4.01"
  "xml-apis xml-apis-ext 1.3.04"
)

echo "Downloading plugin libraries into ${LIB_DIR}"
for entry in "${ARTIFACTS[@]}"; do
  read -r group artifact version <<<"${entry}"
  jar_name="${artifact}-${version}.jar"
  dest="${LIB_DIR}/${jar_name}"
  if [[ -s "${dest}" ]]; then
    echo "  [skip] ${jar_name} (already present)"
    continue
  fi
  url="${MAVEN_CENTRAL}/${group}/${artifact}/${version}/${jar_name}"
  echo "  [get ] ${jar_name}"
  curl -fsSL --retry 4 --retry-delay 2 -o "${dest}.tmp" "${url}"
  mv "${dest}.tmp" "${dest}"
done

echo "All libraries present:"
ls -1 "${LIB_DIR}"
