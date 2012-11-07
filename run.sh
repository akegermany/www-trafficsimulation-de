#!/bin/bash

browser=firefox
file=trafficsimulation-actual-target.html
echo "running java code in trafficsimulation.jar by calling trafficsimulation.html"
echo "WATCH: must close ALL firefox-sessions to prevent cachning"
echo "firefox $file"
$browser $PWD/$file
