#!/bin/sh
cd conf
mysql -u root -p1234 < create_db.sql
cd ..
sbt run