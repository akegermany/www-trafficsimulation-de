#!/bin/sh

targetDir=$HOME/public_html/MicroApplet
jarFile=www-trafficsimulation-de-5.0.jar
cp target/$jarFile ./trafficsimulation.jar

zip -r trafficApplet.zip trafficsimulation.html trafficsimulation.jar

cp -v trafficApplet.zip  $targetDir
cp -v trafficsimulation.jar $targetDir
cd $targetDir
echo "uploaded to the local public html"
echo "now uploading to the webserver ..."
uploadweblocal.sh trafficsimulation.jar trafficApplet.zip 





