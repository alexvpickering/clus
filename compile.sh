#!/bin/sh
export WEKA_DIR=$HOME/NoCsBack/weka-3-4
export CLUS_DIR=$HOME/Clus
javac -d "$CLUS_DIR/bin" -cp "$CLUS_DIR/.:$WEKA_DIR/weka.jar:$CLUS_DIR/commons-math-1.0.jar" clus/Clus.java
