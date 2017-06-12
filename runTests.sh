#!/bin/sh
sbt "test:testOnly *ApplicationEndpointTest"
sbt "test:testOnly *CommentEndpointTest"
sbt "test:testOnly *MessageEndpointTest"
sbt "test:testOnly *PostEndpointTest"
sbt "test:testOnly *UserEndpointTest"

