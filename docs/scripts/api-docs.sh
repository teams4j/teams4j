#!/usr/bin/env bash
# Unpacks every module's javadoc jar into docs/public/api/<module>/, where VitePress serves it as
# static files. The jars come from a git ref, by default the latest release tag, so the API pages
# describe the last release even when the guides on main are ahead. Pass `.` to use the working tree.
set -euo pipefail

root=$(git rev-parse --show-toplevel)
ref=${1:-$(git -C "$root" describe --tags --abbrev=0 --match 'v*')}
out=$root/docs/public/api

if [ "$ref" = . ]; then
  src=$root
else
  src=$(mktemp -d)
  git -C "$root" worktree add --detach -q "$src" "$ref"
  trap 'git -C "$root" worktree remove --force "$src"' EXIT
fi

(cd "$src" && ./gradlew -q javadocJar)

rm -rf "$out"
mkdir -p "$out"
for jar in "$src"/teams4j-*/build/libs/*-javadoc.jar; do
  module=$(basename "$(dirname "$(dirname "$(dirname "$jar")")")")
  mkdir -p "$out/$module"
  unzip -q -o "$jar" -d "$out/$module"
  rm -rf "$out/$module/META-INF"
  # An empty jar (the Kotlin modules before Dokka, 0.1.0) would otherwise be a 404.
  if [ ! -f "$out/$module/index.html" ]; then
    cat > "$out/$module/index.html" <<HTML
<!doctype html><meta charset="utf-8"><title>$module</title>
<p>No API documentation was generated for <code>$module</code> in this release.
The <a href="https://central.sonatype.com/artifact/io.github.teams4j/$module">sources jar</a> is the reference.</p>
HTML
  fi
done
echo "$ref" > "$out/VERSION"
echo "API docs for $ref in $out"
