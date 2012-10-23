#!/bin/bash

export DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

$DIR/gradle-1.2-rc-1/bin/gradle $@


