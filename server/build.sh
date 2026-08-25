#!/usr/bin/env bash
# Compile the PvZ2 server with plain javac (no gradle).
# Compiles shared/ protocol + server/ sources; gson from lib/.
set -e
cd "$(dirname "$0")"
mkdir -p out
find src ../shared/src -name '*.java' > sources.txt
echo "Compiling..."
javac -encoding UTF-8 -d out -cp "lib/gson-2.13.1.jar" @sources.txt
rm -f sources.txt
echo "BUILD OK (classes in server/out)"
