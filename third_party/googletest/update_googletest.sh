#!/bin/sh

REVISION='10ade8473b698a8fe14ddb518c2abd228669657a'
REPO='https://github.com/google/googletest.git'
PREFIX='third_party/googletest/googletest'

cd `git rev-parse --show-toplevel`
echo "Updating googletest to ${REVISION}"
git subtree pull --prefix="$PREFIX" "$REPO" "$REVISION" --squash
