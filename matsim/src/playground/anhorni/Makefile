######################################################################
# run stuff with, i.e.
#  make MAINCLASS=matsim/network/NetworkTest CONFIG=myconfig.xml run
# write your own Makefile
#
# as an alternative, you can use a file Makefile.local where you can
# add or override variables and define phony tags


MAINCLASS = org/matsim/run/Controler
CONFIG = config.xml
DTD = config_v1.dtd
MEMORY = -Xms1450m -Xmx1800m

######################################################################
# do NOT change that just for normal runs!

LIBS := libs/colt.jar
LIBS := $(LIBS):libs/commons-collections-3.2.jar
LIBS := $(LIBS):libs/itext-2.0.4.jar
LIBS := $(LIBS):libs/jcommon-1.0.10.jar
LIBS := $(LIBS):libs/jfreechart-1.0.6.modified.0.jar
LIBS := $(LIBS):libs/jgap-3.1.jar
LIBS := $(LIBS):libs/jung-1.7.6.jar
LIBS := $(LIBS):libs/log4j-1.2.15.jar

LIBS := $(LIBS):libs/jaxb-2.1.6/lib/activation.jar
LIBS := $(LIBS):libs/jaxb-2.1.6/lib/jaxb-api.jar
LIBS := $(LIBS):libs/jaxb-2.1.6/lib/jaxb-impl.jar
LIBS := $(LIBS):libs/jaxb-2.1.6/lib/jaxb-xjc.jar
LIBS := $(LIBS):libs/jaxb-2.1.6/lib/jaxb1-impl.jar
LIBS := $(LIBS):libs/jaxb-2.1.6/lib/jsr173_1.0_api.jar

LIBS := $(LIBS):libs/geotools/ant-optional-1.5.1.jar
LIBS := $(LIBS):libs/geotools/batik-awt-util-1.6.jar
LIBS := $(LIBS):libs/geotools/batik-bridge-1.6.jar
LIBS := $(LIBS):libs/geotools/batik-css-1.6.jar
LIBS := $(LIBS):libs/geotools/batik-dom-1.6.jar
LIBS := $(LIBS):libs/geotools/batik-ext-1.6.jar
LIBS := $(LIBS):libs/geotools/batik-gvt-1.6.jar
LIBS := $(LIBS):libs/geotools/batik-parser-1.6.jar
LIBS := $(LIBS):libs/geotools/batik-script-1.6.jar
LIBS := $(LIBS):libs/geotools/batik-svg-dom-1.6.jar
LIBS := $(LIBS):libs/geotools/batik-svggen-1.6.jar
LIBS := $(LIBS):libs/geotools/batik-transcoder-1.6.jar
LIBS := $(LIBS):libs/geotools/batik-util-1.6.jar
LIBS := $(LIBS):libs/geotools/batik-xml-1.6.jar
LIBS := $(LIBS):libs/geotools/common-2.1.0.jar
LIBS := $(LIBS):libs/geotools/commons-beanutils-1.4.jar
LIBS := $(LIBS):libs/geotools/commons-cli-2.0-gt2-pre1.jar
LIBS := $(LIBS):libs/geotools/commons-io-1.2.jar
LIBS := $(LIBS):libs/geotools/commons-jxpath-1.2.jar
LIBS := $(LIBS):libs/geotools/commons-lang-2.1.jar
LIBS := $(LIBS):libs/geotools/commons-logging-1.0.4.jar
LIBS := $(LIBS):libs/geotools/commons-pool-1.3.jar
LIBS := $(LIBS):libs/geotools/concurrent-1.3.4.jar
LIBS := $(LIBS):libs/geotools/db2jcc_dummy-8.2.1.jar
LIBS := $(LIBS):libs/geotools/ecore-2.1.0.jar
LIBS := $(LIBS):libs/geotools/geoapi-nogenerics-2.1-M2.jar
LIBS := $(LIBS):libs/geotools/geowidgets-1.0-M1.jar
LIBS := $(LIBS):libs/geotools/gt2-api-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-arcgrid-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-arcsde-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-brewer-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-coverage-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-coveragetools-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-cql-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-db2-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-demo-data-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-demo-mappane-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-demo-property-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-directory-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-epsg-access-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-epsg-hsql-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-epsg-postgresql-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-epsg-wkt-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-geomedia-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-geotiff-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-gml-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-go-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-graph-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-gtopo30-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-hsql-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-image-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-imagemosaic-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-imagepyramid-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-indexed-shapefile-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-main-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-mappane-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-mif-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-mysql-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-openoffice-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-oracle-spatial-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-postgis-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-referencing-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-referencing3D-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-render-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-sample-data-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-sde-dummy-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-shapefile-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-shapefile-renderer-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-svgsupport-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-tiger-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-validation-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-vpf-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-wfs-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-widgets-swing-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-wms-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-xml-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-xml-filter-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-xml-gml2-2.3.3.jar
LIBS := $(LIBS):libs/geotools/gt2-xml-gml3-2.3.3.jar
LIBS := $(LIBS):libs/geotools/hsqldb-1.8.0.1.jar
LIBS := $(LIBS):libs/geotools/imageioext-asciigrid-1.0-rc1.jar
LIBS := $(LIBS):libs/geotools/imageioext-customstreams-1.0-rc1.jar
LIBS := $(LIBS):libs/geotools/jaxb-api-1.3.jar
LIBS := $(LIBS):libs/geotools/jaxb-impl-1.3.jar
LIBS := $(LIBS):libs/geotools/jaxb-libs-1.3.jar
LIBS := $(LIBS):libs/geotools/jdbc-stdext-2.0.jar
LIBS := $(LIBS):libs/geotools/jdom-1.0.jar
LIBS := $(LIBS):libs/geotools/jdom-b9.jar
LIBS := $(LIBS):libs/geotools/jlfgr-1.0.jar
LIBS := $(LIBS):libs/geotools/jsr108-0.01.jar
LIBS := $(LIBS):libs/geotools/jta-1.0.1B.jar
LIBS := $(LIBS):libs/geotools/jts-1.7.1.jar
LIBS := $(LIBS):libs/geotools/mysql-connector-java-3.0.10.jar
LIBS := $(LIBS):libs/geotools/picocontainer-1.2.jar
LIBS := $(LIBS):libs/geotools/postgis-driver-1.0.jar
LIBS := $(LIBS):libs/geotools/postgresql-8.1-407.jdbc3.jar
LIBS := $(LIBS):libs/geotools/relaxngDatatype-X.jar
LIBS := $(LIBS):libs/geotools/spatialdb-0.1.jar
LIBS := $(LIBS):libs/geotools/vecmath-1.3.1.jar
LIBS := $(LIBS):libs/geotools/velocity-1.4.jar
LIBS := $(LIBS):libs/geotools/velocity-dep-1.4.jar
LIBS := $(LIBS):libs/geotools/xercesImpl-2.7.1.jar
LIBS := $(LIBS):libs/geotools/xml-apis-1.0.b2.jar
LIBS := $(LIBS):libs/geotools/xml-apis-xerces-2.7.1.jar
LIBS := $(LIBS):libs/geotools/xsd-2.1.1.jar
LIBS := $(LIBS):libs/geotools/xsdlib-20050614.jar

# libs required for OTFVis:
LIBS := $(LIBS):libs/TimingFramework-0.53.jar
LIBS := $(LIBS):libs/jogl-1.1.0/gluegen-rt.jar
LIBS := $(LIBS):libs/jogl-1.1.0/jogl.jar

CLASSPATH = classes
SOURCEPATH = src
JAVA = java -classpath $(LIBS):$(CLASSPATH)
JAVAC = javac -source 1.5 -sourcepath $(SOURCEPATH) -classpath $(LIBS):$(CLASSPATH)
PROFILER =
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

.PHONY: compile run tags clean

compile:
	@echo "------------------------------------------------------------"
	-mkdir $(CLASSPATH)
	cp $(SOURCEPATH)/log4j.xml $(CLASSPATH)/log4j.xml
	$(JAVAC) $(SOURCEPATH)/$(MAINCLASS).java -d $(CLASSPATH)
	@echo "------------------------------------------------------------"

run: compile
	@echo "------------------------------------------------------------"
	$(JAVA) $(JAVAX) $(MAINCLASS) $(ARG)
	@echo "------------------------------------------------------------"

tags:
	etags src/org/matsim/*.java
	etags --append src/org/matsim/*/*.java

clean:
	@echo "------------------------------------------------------------"
	-rm -r $(CLASSPATH)
	@echo "------------------------------------------------------------"

