#!/bin/bash

browser=firefox
echo "running java code in trafficsimulation.jar by calling trafficsimulation.html"
echo "WATCH: must close ALL firefox-sessions to prevent cachning"
echo "firefox trafficsimulation.html"
$browser $PWD/trafficsimulation.html