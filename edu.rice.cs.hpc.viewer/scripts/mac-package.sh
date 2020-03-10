#!/bin/bash
# Script to create packages of hpcviewer or hpctraceviewer from macOS platforms
# To run this script, you need to export hpcviewer and/or hpctraceviewer into a
# directory (called "export/hpcviewer" for instance). Eclipse will then create
# a set of directories named hpcviewer
# copy this script into export directory, and then run in inside export/hpcviewer 
# as follows:
#   ../mac-package hpcviewer
# (it's ugly, but it works)

# example of variable initialization
release=`date "+%Y.%m"`

platform=`uname`

if [ $platform != "Darwin" ]; then
  echo "This script only for Mac OS based platform"
  exit
fi

if [ -e hpcviewer/MacOS/hpcviewer ]; then
  viewer="hpcviewer"
elif [ -e hpctraceviewer/MacOS/hpctraceviewer ]; then
  viewer="hpctraceviewer"
else
  echo "Unknown RCP"
  exit
fi

cd $viewer
# check if we have separate Resources, MacOS and Info.plist
if [ -e MacOS/${viewer} ]; then
    mv MacOS/${viewer} ${viewer}.app/Contents/MacOS
    rmdir MacOS
fi
if [ -e Resources  ]; then
    mv Resources ${viewer}.app/Contents/
fi
   if [ -e Info.plist ]; then
       mv Info.plist ${viewer}.app/Contents/
   fi
cd ..
