# NOTE: This is NOT a makefile to compile matsim.  Instead, it performs
# certain maintenance helper tasks.

QUICK=-Dmaven.test.skip -Dmaven.javadoc.skip -Dsource.skip -Dassembly.skipAssembly=true -DskipTests --offline

.PHONY: hs hybridsim

hs: hybridsim

hybridsim:
	cd matsim ; mvn clean install -DskipTests
	cd contribs/protobuf  ; mvn clean eclipse:clean eclipse:eclipse install
	cd contribs/hybridsim ; mvn clean eclipse:clean eclipse:eclipse install

release:
	mvn clean ; cd matsim ; mvn -f ~/git/matsim/pom.xml --projects playgrounds/kairuns/ --also-make install -DskipTests
	cd playgrounds/kairuns ; mvn clean ; mvn -Prelease -DskipTests=true

matsim-quick:
	cd matsim ; mvn clean install ${QUICK}

quick:
	mvn clean install ${QUICK}


#	cd matsim ; mvn clean ; mvn install -DskipTests=true 
##	cd contribs ; mvn clean ; mvn install --fail-at-end -DskipTests=true
#	cd contribs/analysis ; mvn clean ; mvn install --fail-at-end -DskipTests=true
#	cd contribs/roadpricing ; mvn clean ; mvn install --fail-at-end -DskipTests=true
#	cd contribs/noise ; mvn clean ; mvn install --fail-at-end -DskipTests=true
##	cd playgrounds; mvn clean ; mvn install --fail-at-end -DskipTests=true
#	cd playgrounds/kairun ; mvn clean ; mvn -Prelease -DskipTests=true
