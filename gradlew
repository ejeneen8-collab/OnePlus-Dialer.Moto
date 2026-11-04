#!/bin/sh

# Gradle start-up script for UN*X
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Fallback to direct gradle invocation or bootstrap if wrapper jar missing
if [ -f "$CLASSPATH" ]; then
    exec java -jar "$CLASSPATH" "$@"
else
    gradle "$@"
fi
