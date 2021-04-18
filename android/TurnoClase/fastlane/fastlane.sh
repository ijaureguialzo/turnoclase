#!/bin/bash

export JAVA_HOME=$(/usr/libexec/java_home -v 11)
bundle exec fastlane
