#!/bin/bash

dataDir=`pwd`
cd /media/data/2_Workspaces/matsim/playgrounds/benjamin/src/main/r
#cd /home/me/MATSim/workspace/playgrounds/benjamin/src/main/r

R --slave $dataDir $dataDir < plotA.R
echo "created plot A in directory " $dataDir
R --slave $dataDir $dataDir < plotB.R
echo "created plot B in directory " $dataDir
R --slave $dataDir $dataDir < plotC.R
echo "created plot C in directory " $dataDir
R --slave $dataDir $dataDir < boxplotCompareThreeCasesWithOutline.R
echo "created boxplot with outline in directory " $dataDir
R --slave $dataDir $dataDir < boxplotCompareThreeCasesNoOutline.R
echo "created boxplot without outline in directory " $dataDir
R --slave $dataDir $dataDir < plotD.R
echo "created plot D in directory " $dataDir
R --slave $dataDir $dataDir < plotE.R
echo "created plot E in directory " $dataDir
R --slave $dataDir $dataDir < plotF.R
echo "created plot F in directory " $dataDir
R --slave $dataDir $dataDir < modalsplit.R
echo "created plot of the modal split in directory " $dataDir
R --slave $dataDir $dataDir < modalsplitbygroups.R
echo "created plot of the modal split in directory " $dataDir
R --slave $dataDir $dataDir < compareDistance.R
echo "created plot of individual distance differences in directory " $dataDir
