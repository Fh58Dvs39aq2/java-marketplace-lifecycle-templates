#!/usr/bin/env sh
set -eu
classes="${TMPDIR:-/tmp}/marketplace-template-classes"
mkdir -p "$classes"
javac -d "$classes" src/main/java/marketplace/mail/*.java
java -cp "$classes" marketplace.mail.TemplateMigrationDemo "$@"
