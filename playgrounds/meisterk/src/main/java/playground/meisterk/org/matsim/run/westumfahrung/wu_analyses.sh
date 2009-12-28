#!/bin/bash

for i in `seq 1 12`;
        do
		# clean up input directory
		rm -f input/network_*
		rm -f input/plans_*
		rm -f input/events_*
		rm -f input/linkset_*
		rm -f input/name_*

		case $i in
		1)
			# ist vs wu	wt -> wu
			# configure before and after scenario
			ln -s /home/meisterk/workspace/MATSim/input/ist/*before* input
			ln -s /home/meisterk/workspace/MATSim/input/wu/*after* input

			# configure linksets
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westtangente.txt input/linkset_before.txt
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westumfahrung.txt input/linkset_after.txt
			;;
		2)
			# ist vs wu-fm	wt -> wu
			# configure before and after scenario
			ln -s /home/meisterk/workspace/MATSim/input/ist/*before* input
			ln -s /home/meisterk/workspace/MATSim/input/wu-fm/*after* input

			# configure linksets
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westtangente.txt input/linkset_before.txt
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westumfahrung.txt input/linkset_after.txt
			;;
		3)
			# ist vs wu-fm-gt	wt -> wu
			# configure before and after scenario
			ln -s /home/meisterk/workspace/MATSim/input/ist/*before* input
			ln -s /home/meisterk/workspace/MATSim/input/wu-fm-gt/*after* input

			# configure linksets
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westtangente.txt input/linkset_before.txt
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westumfahrung.txt input/linkset_after.txt
			;;
		4)
			# ist vs wu-fm-gt-nr	wt -> wu
			# configure before and after scenario
			ln -s /home/meisterk/workspace/MATSim/input/ist/*before* input
			ln -s /home/meisterk/workspace/MATSim/input/wu-fm-gt-nr/*after* input

			# configure linksets
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westtangente.txt input/linkset_before.txt
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westumfahrung.txt input/linkset_after.txt
			;;
		5)
			# ist vs wu-fm-gt-nr	wt -> wu || nr
			# configure before and after scenario
			ln -s /home/meisterk/workspace/MATSim/input/ist/*before* input
			ln -s /home/meisterk/workspace/MATSim/input/wu-fm-gt-nr/*after* input

			# configure linksets
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westtangente.txt input/linkset_before.txt
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westumfahrung-nordring.txt input/linkset_after.txt
			;;
		6)
			# ist vs wu-fm-gt-nouetli	wt -> wu\uetli 
			ln -s /home/meisterk/workspace/MATSim/input/ist/*before* input
			ln -s /home/meisterk/workspace/MATSim/input/wu-fm-gt-nouetli/*after* input

			# configure linksets
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westtangente.txt input/linkset_before.txt
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westumfahrung-nouetli.txt input/linkset_after.txt
			;;
		7)
			# wu vs wu-fm	wt -> wu 
			# configure before and after scenario
			ln -s /home/meisterk/workspace/MATSim/input/wu/*before* input
			ln -s /home/meisterk/workspace/MATSim/input/wu-fm/*after* input

			# configure linksets
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westtangente.txt input/linkset_before.txt
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westumfahrung.txt input/linkset_after.txt
			;;
		8)
			# wu-fm vs wu-fm-gt	wt(flama) -> wu 
			# configure before and after scenario
			ln -s /home/meisterk/workspace/MATSim/input/wu-fm/*before* input
			ln -s /home/meisterk/workspace/MATSim/input/wu-fm-gt/*after* input

			# configure linksets
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westtangente-flama.txt input/linkset_before.txt
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westumfahrung.txt input/linkset_after.txt
			;;
		9)
			# wu-fm-gt vs wu-fm-gt-nr	wt(flama) -> wu
			# configure before and after scenario
			ln -s /home/meisterk/workspace/MATSim/input/wu-fm-gt/*before* input
			ln -s /home/meisterk/workspace/MATSim/input/wu-fm-gt-nr/*after* input

			# configure linksets
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westtangente-flama.txt input/linkset_before.txt
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westumfahrung.txt input/linkset_after.txt
			;;
		10)
			# wu-fm-gt vs wu-fm-gt-nr	wt(flama) -> wu || nr
			# configure before and after scenario
			ln -s /home/meisterk/workspace/MATSim/input/wu-fm-gt/*before* input
			ln -s /home/meisterk/workspace/MATSim/input/wu-fm-gt-nr/*after* input

			# configure linksets
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westtangente-flama.txt input/linkset_before.txt
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westumfahrung-nordring.txt input/linkset_after.txt
			;;
		11)
			# wu-fm-gt vs wu-fm-gt-nouetli	uetli -> wt(flama)
			# configure before and after scenario
			ln -s /home/meisterk/workspace/MATSim/input/wu-fm-gt/*before* input
			ln -s /home/meisterk/workspace/MATSim/input/wu-fm-gt-nouetli/*after* input

			# configure linksets
			ln -s /home/meisterk/workspace/MATSim/input/linksets/uetliberg.txt input/linkset_before.txt
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westtangente-flama.txt input/linkset_after.txt
			;;
		12)	
			# wu-fm-gt vs wu-fm-gt-nouetli	wt(flama) -> wu\uetli
			# configure before and after scenario
			ln -s /home/meisterk/workspace/MATSim/input/wu-fm-gt/*before* input
			ln -s /home/meisterk/workspace/MATSim/input/wu-fm-gt-nouetli/*after* input

			# configure linksets
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westtangente-flama.txt input/linkset_before.txt
			ln -s /home/meisterk/workspace/MATSim/input/linksets/westumfahrung-nouetli.txt input/linkset_after.txt
			;;
		esac
		# run the analysis
		make MAINCLASS=playground/meisterk/org/matsim/run/westumfahrung/CompareScenarios CONFIG='input/name_before.txt input/network_before.xml input/plans_before.xml.gz input/events_before.dat input/linkset_before.txt input/name_after.txt input/network_after.xml input/plans_after.xml.gz input/events_after.dat input/linkset_after.txt' DTD='' MEMORY='-Xms512M -Xmx1024M' run
        done 

