package org.matsim.integration.daily.accessibility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.analysis.vsp.qgis.QGisConstants;
import org.matsim.contrib.analysis.vsp.qgis.QGisMapnikFileCreator;
import org.matsim.contrib.analysis.vsp.qgis.QGisWriter;
import org.matsim.contrib.analysis.vsp.qgis.RasterLayer;
import org.matsim.contrib.analysis.vsp.qgis.VectorLayer;
import org.matsim.contrib.analysis.vsp.qgis.layerTemplates.AccessibilityDensitiesRenderer;
import org.matsim.contrib.analysis.vsp.qgis.layerTemplates.AccessibilityRenderer;
import org.matsim.contrib.analysis.vsp.qgis.layerTemplates.AccessibilityXmlRenderer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ExeRunner;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.testcases.MatsimTestUtils;

public class AccessibilityRuns {
	public static final Logger log = Logger.getLogger( AccessibilityRuns.class ) ;

	public static final Boolean doPopulationWeightedPlot = null;

	public static final Boolean doNonPopulationWeightedPlot = null;
	
	private static final double cellSize = 1000.;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void doAccessibilityTest() {

		Config config = ConfigUtils.createConfig( new AccessibilityConfigGroup() ) ;
		
		AccessibilityConfigGroup acg = (AccessibilityConfigGroup) config.getModule( AccessibilityConfigGroup.GROUP_NAME ) ;
		
		config.network().setInputFile("../../matsimExamples/countries/za/nmbm/network/NMBM_Network_CleanV7.xml.gz");
		config.facilities().setInputFile("../../matsimExamples/countries/za/nmbm/facilities/20121010/facilities.xml.gz" );

		config.controler().setLastIteration(0);

		config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.ABORT );

		// some (otherwise irrelevant) settings to make the vsp check happy:
		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);
		config.vspExperimental().setRemovingUnneccessaryPlanAttributes(true);
		config.plans().setActivityDurationInterpretation( ActivityDurationInterpretation.tryEndTimeThenDuration );

		{
			StrategySettings stratSets = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) );
			stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString() );
			stratSets.setWeight(1.);
			config.strategy().addStrategySettings(stratSets);
		}

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		List<String> activityTypes = new ArrayList<String>() ;
		ActivityFacilities homes = FacilitiesUtils.createActivityFacilities("homes") ;
		for ( ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()  ) {
			for ( ActivityOption option : fac.getActivityOptions().values() ) {
				// figure out all activity types
				if ( !activityTypes.contains(option.getType()) ) {
					activityTypes.add( option.getType() ) ;
				}
				// figure out where the homes are
				if ( option.getType().equals("h") ) {
					homes.addActivityFacility(fac);
				}
			}
		}

		// new
		Map<String, ActivityFacilities> activityFacilitiesMap = new HashMap<String, ActivityFacilities>();
		Controler controler = new Controler(scenario) ;
		controler.setOverwriteFiles(true);
		// end new

		log.warn( "found activity types: " + activityTypes );
		// yyyy there is some problem with activity types: in some algorithms, only the first letter is interpreted, in some
		// other algorithms, the whole string.  BEWARE!  This is not good software design and should be changed.  kai, feb'14

		
		// two loops over activity types and modes to add one GridBasedAccessibilityControlerListenerV3 for each combination
		for ( String actType : activityTypes ) {
//			for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
				if ( !actType.equals("w") ) {
					log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
					continue ;
				}
//				if ( !mode.equals(Modes4Accessibility.freeSpeed) ) {
//					log.error("skipping everything except freespeed for debugging purposes; remove in production code. dz, nov'14") ;
//					continue ;
//				}

				config.controler().setOutputDirectory( utils.getOutputDirectory());
				ActivityFacilities opportunities = FacilitiesUtils.createActivityFacilities() ;
				for ( ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()  ) {
					for ( ActivityOption option : fac.getActivityOptions().values() ) {
						if ( option.getType().equals(actType) ) {
							opportunities.addActivityFacility(fac);
						}
					}
				}

				activityFacilitiesMap.put(actType, opportunities);

				GridBasedAccessibilityControlerListenerV3 listener = 
						new GridBasedAccessibilityControlerListenerV3(activityFacilitiesMap.get(actType), config, scenario.getNetwork());
				// define the mode that will be considered
				listener.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);
//				listener.setComputingAccessibilityForMode(mode, true);
				listener.addAdditionalFacilityData(homes) ;
				listener.generateGridsAndMeasuringPointsByNetwork(cellSize);

//				listener.writeToSubdirectoryWithName(actType + "/" + mode);
				listener.writeToSubdirectoryWithName(actType);
				
				listener.setUrbansimMode(false); // avoid writing some (eventually: all) files that related to matsim4urbansim

				controler.addControlerListener(listener);
//			}
		}

		controler.run();

		

		// ############################################################################################################
		// creating visual output
		// ############################################################################################################
		
		// GnuplotScriptWriter.createGnuplotScript(config, activityTypes);
		
		String workingDirectory =  config.controler().getOutputDirectory();

		// create Mapnik file that is needed to have OSM layer in QGis project
		QGisMapnikFileCreator.writeMapnikFile(workingDirectory + "osm_mapnik.xml");
				
		
		// Write QGis project file
		QGisWriter writer = new QGisWriter(TransformationFactory.WGS84_SA_Albers, workingDirectory);
		
//		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_SA_Albers, TransformationFactory.WGS84);
//		
//		Coord lowerLeftCoordinateSAALbers = new CoordImpl(100000,-3720000);
//		Coord upperRightCoordinateSAALbers = new CoordImpl(180000,-3675000);
//		Coord lowerLeftCoordinateWGS84 = transformation.transform(lowerLeftCoordinateSAALbers);
//		Coord upperRightCoordinateWGS84 = transformation.transform(upperRightCoordinateSAALbers);
//		
//		System.out.println("###########################################################################");
//		System.out.println("lowerLeftCoordinateWGS84: " + lowerLeftCoordinateWGS84);
//		System.out.println("upperRightCoordinateWGS84: " + upperRightCoordinateWGS84);
//		System.out.println("###########################################################################");
//		
//		//extent double array of the minx, miny, maxx and maxy coordinates of the starting view
		
		
		double[] extent = {100000,-3720000,180000,-3675000};
		writer.setExtent(extent);
		
		//raster layer
		RasterLayer mapnikLayer = new RasterLayer("osm_mapnik_xml", workingDirectory + "/osm_mapnik.xml");
		new AccessibilityXmlRenderer(mapnikLayer);
		mapnikLayer.setSrs("WGS84_Pseudo_Mercator");
		writer.addLayer(0,mapnikLayer);
		
		

		// loop over activity types to produce one QGIs project file for each combination
		for (String actType : activityTypes) {
//			for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
				if ( !actType.equals("w") ) {
					log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
					continue ;
				}
//				if ( !mode.equals(Modes4Accessibility.freeSpeed) ) {
//					log.error("skipping everything except freespeed for debugging purposes; remove in production code. dz, nov'14") ;
//					continue ;
//				}
				
				workingDirectory =  config.controler().getOutputDirectory() + actType + "/";
				writer.changeWorkingDirectory(workingDirectory);
				
				String qGisProjectFile = "/QGisProjectFile.qgs";
				
				VectorLayer densityLayer = new VectorLayer("density", workingDirectory + "accessibilities.csv", QGisConstants.geometryType.Point);
				densityLayer.setXField(1);
				densityLayer.setYField(2);
				AccessibilityDensitiesRenderer dRenderer = new AccessibilityDensitiesRenderer(densityLayer);
				dRenderer.setRenderingAttribute(8);
				writer.addLayer(densityLayer);
				
				
				VectorLayer accessibilityLayer = new VectorLayer("accessibility", workingDirectory + "accessibilities.csv", QGisConstants.geometryType.Point);
				//there are two ways to set x and y fields for csv geometry files
				//1) if there is a header, you can set the members xField and yField to the name of the column headers
				//2) if there is no header, you can write the column index into the member (e.g. field_1, field_2,...), but works also if there is a header
				accessibilityLayer.setXField(1);
				accessibilityLayer.setYField(2);
				AccessibilityRenderer renderer = new AccessibilityRenderer(accessibilityLayer);
				renderer.setRenderingAttribute(3); // choose column/header to visualize
				writer.addLayer(accessibilityLayer);
				
				writer.write(qGisProjectFile);
				
				
				// old version, not dependent on analysis.vsp
//				String version = "<qgis projectname=\"\" version=\"2.6.1-Brighton\">\n";
//
//				QGisProjectFileWriter qgisWriter = new QGisProjectFileWriter();
//
//				try {
//					BufferedWriter writer = IOUtils.getBufferedWriter( config.controler().getOutputDirectory() + "/" + actType 
//							+ "/" + mode + "/QGisProjectFile.qgs" ) ;
//					//					openFile(this.outputFolder+filename);
//
//					qgisWriter.writeQGisHead(writer);
//					writer.write(version);
//					//					handler.startLegend(writer);
//					//					handler.writeLegendLayer(writer,key);
//					//					handler.writeProjectLayer(writer, key, geometry, claz, type);
//
//					//TODO Should be split up into (customizable) modules later, so far just a big collection of everything
//					if (actType.equals("w")) {
//						qgisWriter.writeEverythingWork(writer, "../../"+mapnikFileName);
//					} else if (actType.equals("e")){
//						qgisWriter.writeEverythingEdu(writer, "../../"+mapnikFileName);						
//					} else {
//						qgisWriter.writeEverythingOther(writer, "../../"+mapnikFileName);
//					}
//
//					//					handler.endProjectLayers(writer);
//					//					handler.writeProperties(writer);
//					qgisWriter.endDocument(writer);
//
//					writer.flush();
//					writer.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//					throw new RuntimeException( "writing QGis project file did not work") ;
//				}
				// end old version
				

			// now creating a snapshot based on above-created project file
			for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
				if ( !mode.equals(Modes4Accessibility.freeSpeed) ) {
					log.error("skipping everything except freespeed for debugging purposes; remove in production code. dz, nov'14") ;
					continue ;
				}
					
					
				// if OS is Windows
				// example (daniel r) // os.arch=amd64 // os.name=Windows 7 // os.version=6.1
				if ( System.getProperty("os.name").contains("Win") || System.getProperty("os.name").contains("win")) {
					// On Windows, the PATH variables need to be set correctly to be able to call "qgis.bat" on the command line
					// This needs to be done manually. It does not seem to be set automatically when installing QGis
					String cmd = "qgis.bat " + workingDirectory + "QGisProjectFile.qgs" +
							" --snapshot " + workingDirectory + "snapshot.png";

					String stdoutFileName = workingDirectory + "snapshot.log";
					int timeout = 99999;

					ExeRunner.run(cmd, stdoutFileName, timeout);
				
					
				// if OS is Macintosh
				// example (dominik) // os.arch=x86_64 // os.name=Mac OS X // os.version=10.10.2
				//} else if ( System.getProperty("os.arch").contains("mac")) {
				} else if ( System.getProperty("os.name").contains("Mac") || System.getProperty("os.name").contains("mac") ) {
					
					String cmd = "/Applications/QGIS.app/Contents/MacOS/QGIS " + workingDirectory + "QGisProjectFile.qgs" +
					" --snapshot " + workingDirectory + "snapshot.png";

					String stdoutFileName = workingDirectory + "snapshot.log";
					
					int timeout = 99999;

					ExeRunner.run(cmd, stdoutFileName, timeout);
				
					
				// if OS is Linux
				// example (benjamin) // os.arch=amd64 // os.name=Linux	// os.version=3.13.0-45-generic
				//} else if ( System.getProperty("os.name").contains("Lin") || System.getProperty("os.name").contains("lin") ) {
					// TODO for linux
					
				// if OS is other
				} else {
					log.warn("generating png files not implemented for os.arch=" + System.getProperty("os.arch") );
				}
			}	
		}		
	}
}
