#!/bin/sh

javac -d ../bin Classifier.java
java ../bin/Classifier $1 $2 unknown