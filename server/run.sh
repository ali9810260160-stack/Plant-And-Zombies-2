#!/usr/bin/env bash
# Run the PvZ2 server (build first with build.sh).
# NOTE: on Windows the JVM uses ';' as the classpath separator even under bash.
cd "$(dirname "$0")"
[ -d out ] || ./build.sh
SEP=":"
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) SEP=";" ;;
esac
java -cp "out${SEP}lib/gson-2.13.1.jar" com.pvz2.server.Main "$@"
