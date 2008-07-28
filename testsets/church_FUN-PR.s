
% *RUN*
% clus %f.s
% cp ../data/church_FUN/ipol_pr.pl .
% perl ../data/church_FUN/prcurves.pl %f.out
% mdiff AUC_average/church_FUN-PR.areas orig/church_FUN-PR.areas.orig

% *CLEAN*
% rm -rf %f.out %f.model hierarchy.txt ipol_pr.pl AUC_average


[Data]
File = ../data/church_FUN/church_FUN.trainvalid.arff.zip
TestSet = ../data/church_FUN/church_FUN.test.arff.zip

[Tree]
ConvertToRules = No
FTest = 0.1

[Model]
MinimalWeight = 5.0

[Attributes]
ReduceMemoryNominalAttrs = yes

[Hierarchical]
Type = TREE
ClassificationTreshold = [0.0,2.0,4.0,6.0,8.0,10.0,12.0,14.0,16.0,18.0,20.0,22.0,24.0,26.0,28.0,30.0,32.0,34.0,36.0,38.0,40.0,42.0,44.0,46.0,48.0,50.0,52.0,54.0,56.0,58.0,60.0,62.0,64.0,66.0,68.0,70.0,72.0,74.0,76.0,78.0,80.0,82.0,84.0,86.0,88.0,90.0,92.0,94.0,96.0,98.0,100.0]
WType = ExpAvgParentWeight
HSeparator = /
