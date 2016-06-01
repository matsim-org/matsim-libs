# NOTE: This is NOT a makefile to compile matsim.  Instead, it performs
# certain maintenance helper tasks.

.PHONY: hs hybridsim

hs: hybridsim

hybridsim:
	cd matsim ; mvn clean install -DskipTests
	cd contribs/protobuf  ; mvn clean eclipse:clean eclipse:eclipse install
	cd contribs/hybridsim ; mvn clean eclipse:clean eclipse:eclipse install
