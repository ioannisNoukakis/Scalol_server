# Scalol_server
## The project
This a student project for the HEIG-VD (http://www.heig-vd.ch/international). In the end this will be the rest API for an 9gag like app.

## Setup the environment
You should have sbt installed and a mariadb/mysql server running.
See conf/application.conf for to configure this application
for your mysql server.

## Run the tests
By the restriction of scala play and it's testing framework you must
run the runTest.sh file in order to perform tests.

## Deploy
Simply run deploy.sh 
Then go to your web browser at http://localhost:9000