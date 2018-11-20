#!/bin/sh
export CLUS_DIR=$HOME/clus
java -Xmx20g -cp "$CLUS_DIR/bin:$CLUS_DIR/jars/commons-math-1.0.jar:$CLUS_DIR/jars/jgap.jar:$CLUS_DIR/jars/cloning-1.9.9.jar:$CLUS_DIR/jars/objenesis-tck-3.0.1.jar" -Djava.library.path=$CLUS_DIR/jars clus.Clus $*
