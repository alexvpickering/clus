#!/bin/sh
export CLUS_DIR=$HOME/Work/Software/clus-SVN-201710
java -Xmx56000m -cp "$CLUS_DIR/bin:$CLUS_DIR/jars/commons-math-1.0.jar:$CLUS_DIR/jars/jgap.jar" -Djava.library.path=$CLUS_DIR/jars clus.Clus $*
