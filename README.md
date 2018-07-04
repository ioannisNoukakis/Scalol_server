# Scalol_server
## The project
This projet is the result of a student projet for the HEIG-VD (http://www.heig-vd.ch/international).
The idea was to create a social media website which allows its users to upload and share funny images
related to the Scala programming language. Thoses images have a score voted by the community.
A small chat system was also implemented.
This repository contains the API rest of this website.
To see the frontend please go to https://github.com/AkessonHenrik/Scalol-frontend.

## Setup the environment
The following are required in order to run this application:
- Java JDK 1.8
- MariaDB 10.3
- Scala 2.11.7
- SBT 0.13.5

See conf/application.conf for to configure this application
for your mysql server.

## Tests
Run the runTest.sh file in order to perform tests.

## Deploy
Run deploy.sh.

## Documentation
The API rest documentation is available in the spec.yaml.
Copy paste its content into http://editor.swagger.io/#/