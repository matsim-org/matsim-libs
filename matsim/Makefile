######################################################################
# run stuff with, i.e.
#  make MAINCLASS=matsim/network/NetworkTest CONFIG=myconfig.xml run
# write your own Makefile
#
# as an alternative, you can use a file Makefile.local where you can
# add or override variables and define phony tags


MAINCLASS = org/matsim/demandmodeling/Tests
CONFIG = config.xml
DTD = config_v1.dtd

######################################################################
# do NOT change that!

LIBS = libs/log4j-1.2.15.jar:libs/jgap-3.1.jar:libs/colt.jar:libs/jung-1.7.6.jar:libs/commons-collections-3.2.jar:libs/concurrent.jar:libs/itext-2.0.4.jar:libs/jcommon-1.0.10.jar:libs/jfreechart-1.0.6.modified.0.jar
CLASSPATH = classes
SOURCEPATH = src
JAVA = java -classpath $(LIBS):$(CLASSPATH)
JAVAC = javac -source 1.5 -sourcepath $(SOURCEPATH) -classpath $(LIBS):$(CLASSPATH)
PROFILER =
MEMORY = -Xms1450m -Xmx1800m
JAVAX = $(MEMORY) -enableassertions $(PROFILER)

######################################################################
# to use the profiler, place the following line in your Makefile.local
#
# PROFILER = -Xrunhprof:cpu=samples

######################################################################
# with Makefile.local you can add or override vars or even define your
# own phony tags
ifeq (./Makefile.local,$(wildcard ./Makefile.local))
	include ./Makefile.local
endif

######################################################################

.PHONY: compile compile_strategies run tags clean

compile:
	@echo "------------------------------------------------------------"
	-mkdir $(CLASSPATH)
	cp $(SOURCEPATH)/log4j.properties $(CLASSPATH)/log4j.properties
	$(JAVAC) $(SOURCEPATH)/$(MAINCLASS).java -d $(CLASSPATH)
	@echo "------------------------------------------------------------"

# strategy modules are loaded dynamically (classname given in config-file)
# thus they are not referenced anywhere in the sourcecode and will not be compiled
# together with the MainClass. So, let's compile them manually if needed.
compile_strategies:
	@echo "------------------------------------------------------------"
	-mkdir $(CLASSPATH)
	$(JAVAC) $(SOURCEPATH)/org/matsim/demandmodeling/plans/strategies/*.java -d $(CLASSPATH)
	@echo "------------------------------------------------------------"

run: compile
	@echo "------------------------------------------------------------"
	$(JAVA) $(JAVAX) $(MAINCLASS) $(CONFIG) $(DTD)
	@echo "------------------------------------------------------------"

tags:
	etags src/org/matsim/*.java
	etags --append src/org/matsim/*/*.java

clean:
	@echo "------------------------------------------------------------"
	-rm -r $(CLASSPATH)
	@echo "------------------------------------------------------------"

######################################################################
# example runs for published versions

triangle:
	${MAKE} MAINCLASS=org/matsim/demandmodeling/TriangleTest CONFIG=config_triangle.xml run

validation:
	${MAKE} MAINCLASS=org/matsim/demandmodeling/ValidationTest CONFIG=config_output-validation.xml run
