package org.matsim.integration.daily;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PlanStrategyRegistrar;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.FacilitiesUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ExeRunner;
import org.matsim.testcases.MatsimTestUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class AccessibilityRuns {
	private static final Logger log = Logger.getLogger( AccessibilityRuns.class ) ; 

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	private Boolean doPopulationWeightedPlot = true;
	private Boolean doNonPopulationWeightedPlot = false;		

//	@SuppressWarnings("static-method")
//	@Test
//	public void doTest() {
//		System.out.println("available ram: " + (Runtime.getRuntime().maxMemory() / 1024/1024));
//
//		final String FN = "matsimExamples/tutorial/lesson-3/network.xml" ;
//
//		Config config = ConfigUtils.createConfig();
//		Scenario sc = ScenarioUtils.createScenario( config ) ;
//
//		try {
//			new MatsimNetworkReader(sc).readFile(FN);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		try {
//			new MatsimNetworkReader(sc).readFile("../" + FN);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		try {
//			new MatsimNetworkReader(sc).readFile("../../" + FN);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		try {
//			new MatsimNetworkReader(sc).readFile("../../../" + FN);
//			// this is the one that works locally
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//
//		Assert.assertTrue(true);
//	}

	@Test
	public void doAccessibilityTest() {
		
		Config config = ConfigUtils.createConfig() ;
		
		// config.network().setInputFile("../../../matsimExamples/countries/za/nmbm/network/network.xml.gz" );
		// config.facilities().setInputFile("../../../matsimExamples/countries/za/nmbm/facilities/facilities.xml.gz" );
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
			stratSets.setStrategyName( PlanStrategyRegistrar.Selector.ChangeExpBeta.toString() );
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
		
		for ( String actType : activityTypes ) {
			for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
//				if ( !actType.equals("w") ) {
//					log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
//					continue ;
//				}
				if ( !mode.equals(Modes4Accessibility.freeSpeed) ) {
					log.error("skipping everything except freespeed for debugging purposes; remove in production code. dz, nov'14") ;
					continue ;
				}
				
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
						
	//			Controler controler = new Controler(scenario) ;
	//			// yy it is a bit annoying to have to run the controler separately for every act type, or even to run it at all.
	//			// But the computation uses too much infrastructure which is plugged together in the controler.
	//			// (Might be able to get away with ONE controler run if we manage to write the accessibility results
	//			// in subdirectories of the controler output directory.) kai, feb'14
	//			
	//			controler.setOverwriteFiles(true);
			
				GridBasedAccessibilityControlerListenerV3 listener = 
					new GridBasedAccessibilityControlerListenerV3(activityFacilitiesMap.get(actType), config, scenario.getNetwork());
				// define the mode that will be considered
				// listener.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);
				listener.setComputingAccessibilityForMode(mode, true);
				listener.addAdditionalFacilityData(homes) ;
				listener.generateGridsAndMeasuringPointsByNetwork(1000.);
				
				listener.writeToSubdirectoryWithName(actType + "/" + mode);
								
				controler.addControlerListener(listener);
			}
		}
					
		controler.run();

		
//		// two loops over activity types and modes to produce one gnuplot script for each combination
//		for (String actType : activityTypes) {
//			for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
////				if ( !actType.equals("w") ) {
////					log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
////					continue ;
////				}
//				if ( !mode.equals(Modes4Accessibility.freeSpeed) ) {
//					log.error("skipping everything except freespeed for debugging purposes; remove in production code. dz, nov'14") ;
//					continue ;
//				}
//				
//				
//				Boolean doPopulationWeightedPlot = this.doPopulationWeightedPlot;
//				Boolean doNonPopulationWeightedPlot = this.doNonPopulationWeightedPlot;	
//
//				
//				if (doPopulationWeightedPlot == false && doNonPopulationWeightedPlot == false) {
//					throw new RuntimeException("At least one plot (pop-weighted or non-pop-weighted) needs to be activated.");
//				}
//				
//				// produce gnuplot scripts
//				try {
//					BufferedWriter writer = IOUtils.getBufferedWriter( config.controler().getOutputDirectory() + "/" + actType 
//							+ "/" + mode + "/t.gpl" ) ;
//					// pm3d is an splot style for drawing palette-mapped 3d and 4d data as color/gray maps and surfaces (docu p.134)
//					// To use pm3d coloring to generate a two-dimensional plot rather than a 3D surface, use set view map
//					// or set pm3d map (p.135)
//					writer.write("set pm3d map\n");
//					
//					// flush { begin | center | end }
//					writer.write("set pm3d flush begin\n") ;
//					
//					// corners2color { mean|geomean|median|min|max|c1|c2|c3|c4 }
//					writer.write("set pm3d corners2color c1\n");
//					
//					// The set style data command changes the default plotting style for data plots (docu p.148)
//					// There are many plotting styles available in gnuplot. They are listed alphabetically below. The commands
//					// set style data and set style function change the default plotting style for subsequent plot and splot
//					// commands. (docu p.42)
//					writer.write("set style data pm3d\n");
//					
//					// Palette is a color storage for use by pm3d, filled color contours or polygons, color histograms, color gradient
//					// background, and whatever it is or it will be implemented (docu p.137)
//					// Gray-to-rgb mapping can be manually set by use of palette defined: A color gradient is defined and used
//					// to give the rgb values. Such a gradient is a piecewise linear mapping from gray values in [0,1] to the RGB
//					// space [0,1]x[0,1]x[0,1]. You must specify the gray values and the corresponding RGB values between which
//					// linear interpolation will be done (docu p.139)
//					writer.write("set palette defined ( 0. '#ff0000', 0.82 '#ff0000', 0.86 '#00ff00', 0.9 '#0000ff', 1.0 '#0000ff' )\n");
//					
//					// The set zrange command sets the range that will be displayed on the z axis. The zrange is used only by
//					// splot and is ignored by plot (docu p.166)
//					writer.write("#set zrange [-40:10]\n");
//					
//					// The set cbrange command sets the range of values which are colored using the current palette by styles
//					// with pm3d, with image and with palette. Values outside of the color range use color of the nearest
//					// extreme (docu p.167)
//					writer.write("#set cbrange [-0:10]\n");
//					
//					// gnuplot supports many different graphics devices. Use set terminal to tell gnuplot what kind of output
//					// to generate (docu p.152)
//					// This terminal produces files in the Adobe Portable Document Format (PDF), useable for printing or display
//					// with tools like Acrobat Reader (docu p.206)
//					//writer.write("set term pdf size 25cm,20cm\n");
//					writer.write("set term pdf font 'Helvetica,6' size 25cm,20cm\n");
//					
//					// The set view command sets the viewing angle for splots. It controls how the 3D coordinates of the plot are
//					// mapped into the 2D screen space. It provides controls for both rotation and scaling of the plotted data, but
//					// supports orthographic projections only (docu p.156)
//					writer.write("#set view 45,30;\n");
//					writer.write("\n") ;
//					
//					// see docu p.137
//					writer.write("# set palette model HSV functions gray, 1, 1\n");
//					writer.write("\n");
//									
//					// New user-defined variables and functions of one through twelve variables may be declared and used anywhere,
//					// including on the plot command itself (docu p.30)
//					// define minimum and maximum functions
//					writer.write("min(a,b) = (a < b) ? a : b\n");
//					writer.write("max(a,b) = (a < b) ? b : a\n");
//					
//					// define two variables
//					writer.write("accmin=3 ; # accessibilities below this are red\n");
//					writer.write("accmax=9 ; # accessibilities above this are blue. max is around 12\n");
//					
//					// define a function to determine the shade of gray. gray(acc) will have a value from 0 through 1
//					// Acc values equal to or below accmin lead to 0; acc values equal to or above accmax lead to 1
//					writer.write("gray(acc) = 2.*min( 1, max(0 , (acc-accmin)/(accmax-accmin) ) ) ;\n") ;
//					writer.write("# I have no idea why this needs to be multiplied by 2. kai, feb'14\n") ;
//					writer.write("\n") ;
//					
//					// consider population density
//					// define two variables
//					writer.write("densmax=1000 ; # 2726 is the maximum value in NMB\n") ;
//					writer.write("maxwhite = 240 ; # 255 means that we go all the way to full white\n") ;
//					
//					while (doPopulationWeightedPlot == true || doNonPopulationWeightedPlot == true) {
//						// define a function that gets the higher the smaller the population density
//						// maxwhite*1 for pop dens = 0; maxwhite*0 for pop dens higher than densmax
//						if (doPopulationWeightedPlot == true) {
//							writer.write("val(dens) = max(0,maxwhite*(densmax-dens)/densmax) ;\n") ;
//							
//							// By default, screens are displayed to the standard output. The set output command redirects the display
//							// to the specified file or device (docu p.132)
//							writer.write("set out 'accessibility-pop-weighted.pdf'\n");
//							
//							// The set title command produces a plot title that is centered at the top of the plot. set title is a special
//							// case of set label (docu p.155)
//							//writer.write("set title 'accessibility to " + actType + " by " + mode + " (population-weighted)'\n") ;
//							
//							doPopulationWeightedPlot = false;
//							
//						} else if (doNonPopulationWeightedPlot == true) {
//							writer.write("val(dens) = 0. ; # unset this comment to get the non-pop-weighted version (for paper)\n");
//							writer.write("set out 'accessibility.pdf'\n");
//							//writer.write("set title 'accessibility to " + actType + " by " + mode + " (non-population-weighted)'\n") ;
//							
//							doNonPopulationWeightedPlot = false;
//						}
//						writer.write("\n") ;
//						
//						// define three color functions
//						// so far not clear to me where the functions come from, dz mai14
//						writer.write("blue(acc,dens) = min(255,  val(dens)+255*max(0,1.-2.*gray(acc))  ) ;\n") ;
//						writer.write("green(acc,dens) = min(255,  val(dens)+255*max(0,min(2.*gray(acc),2.-2.*gray(acc)))  ) ;\n") ;
//						writer.write("red(acc,dens) = min(255,  val(dens)+255*max(0,2.*gray(acc)-1)  ) ;\n") ;
//						writer.write("\n") ;
//						
//						// define color function based on accessibility and population density
//						// int(x) = integer part of x, truncated toward zero (docu p.26)
//						// Example: rgb(r,g,b) = 65536 * int(r) + 256 * int(g) + int(b) (docu p.35)
//						// Why are blue and red swapped here???
//						writer.write("rgb(acc,dens) = 256*256*int(blue(acc,dens))+256*int(green(acc,dens))+int(red(acc,dens)) ;\n") ;
//						writer.write("\n") ;
//						writer.write("unset colorbox ; # useless with lc rgb variable\n") ;
//						writer.write("\n") ;  // end new
//						
//						// plot csv file based on three values; first two are coordinates; third takes into account accessibility and 
//						// population density and is calculated based on above-defined rgb formula
//						// The "lc rgbcolor variable" tells the program to read RGB color information for each line in the data file.
//						// This requires a corresponding additional column in the using specifier. The extra column is interpreted as a
//						// 24-bit packed RGB triple (docu p.35)
//						// writer.write("splot \"<awk '!/access/{print $0}' doAccessibilityTest/w/accessibilities.csv\" u 1:2:(rgb($3,$8)) lc rgb variable\n") ;
//						writer.write("splot \"<awk '!/access/{print $0}' accessibilities.csv\" u 1:2:(rgb($3,$8)) lc rgb variable\n");
//						// writer.write("splot \"accessibilities.csv\" u 1:2:(rgb($3,$8)) lc rgb variable\n");
//						// writer.write("splot \"accessibilities.csv\" u 1:2:(rgb($3,$8)) notitle lc rgb variable\n");
//					}
//						
//					writer.close();
//				} catch (Exception ee ) {
//					ee.printStackTrace(); 
//					throw new RuntimeException( "writing t.gpl did not work") ;
//				}
//				
//				
//				// start running gnuplot with above-created script
//				
//				// If working on a Windows system, it is important that the environment variable PATH includes the gnuplot folder
//				// so that gnuplot can be run from the folder where the data is stored (otherwise the relative paths won't work)
//				String cmd = "gnuplot t.gpl";
//							
//				// Doesn't work if root of directory is not passed
//				String stdoutFileName = config.controler().getOutputDirectory() + actType + "/" + mode + "/gnuplot.log";
//				// String stdoutFileName = config.controler().getOutputDirectory() + "gnuplot.log";
//				int timeout = 99999;
//				
//				// 4th argument = workingDirectory. Since we are working with relative paths, workingDirectory needs to be passed.
//				ExeRunner.run(cmd, stdoutFileName, timeout, config.controler().getOutputDirectory() + actType + "/" + mode);
//			}
//		}

		
		String osmMapnikFile = config.controler().getOutputDirectory() + "osm_mapnik.xml";
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter( osmMapnikFile ) ;
		
			writer.write("<GDAL_WMS>\n");
			writer.write("\t<Service name=\"TMS\">\n");
			writer.write("\t\t<ServerUrl>http://tile.openstreetmap.org/${z}/${x}/${y}.png</ServerUrl>\n");
			writer.write("\t</Service>\n");
			writer.write("\t<DataWindow>\n");
			//TODO make coordinates adjustable, ideally with an automatic coordinate system conversion so that they only have to be set once (dz, dez'14)
			writer.write("\t\t<UpperLeftX>-20037508.34</UpperLeftX>\n");
			writer.write("\t\t<UpperLeftY>20037508.34</UpperLeftY>\n");
			writer.write("\t\t<LowerRightX>20037508.34</LowerRightX>\n");
			writer.write("\t\t<LowerRightY>-20037508.34</LowerRightY>\n");
			writer.write("\t\t<TileLevel>18</TileLevel>\n");
			writer.write("\t\t<TileCountX>1</TileCountX>\n");
			writer.write("\t\t<TileCountY>1</TileCountY>\n");
			writer.write("\t\t<YOrigin>top</YOrigin>\n");
			writer.write("\t</DataWindow>\n");
			writer.write("\t<Projection>EPSG:3857</Projection>\n");
			writer.write("\t<BlockSizeX>256</BlockSizeX>\n");
			writer.write("\t<BlockSizeY>256</BlockSizeY>\n");
			writer.write("\t<BandsCount>3</BandsCount>\n");
			writer.write("\t<Cache>\n");
			writer.write("\t\t<Path>/tmp/cache_osm_mapnik</Path>\n");
			writer.write("\t</Cache>\t\n");
			writer.write("</GDAL_WMS>\n");
			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException( "writing Mapnik file did not work") ;
		}
		
				
		// two loops over activity types and modes to produce one QGIs project file for each combination
		for (String actType : activityTypes) {
			for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
//				if ( !actType.equals("w") ) {
//					log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
//					continue ;
//				}
				if ( !mode.equals(Modes4Accessibility.freeSpeed) ) {
					log.error("skipping everything except freespeed for debugging purposes; remove in production code. dz, nov'14") ;
					continue ;
				}
				
				String version = "<qgis projectname=\"\" version=\"2.6.1-Brighton\">\n";
								
				QGisProjectFileWriter qgisWriter = new QGisProjectFileWriter();
				
				try {
					BufferedWriter writer = IOUtils.getBufferedWriter( config.controler().getOutputDirectory() + "/" + actType 
							+ "/" + mode + "/QGisProjectFile.qgs" ) ;
//					openFile(this.outputFolder+filename);
					
					qgisWriter.writeQGisHead(writer);
					writer.write(version);
//					handler.startLegend(writer);
//					handler.writeLegendLayer(writer,key);
//					handler.writeProjectLayer(writer, key, geometry, claz, type);
					
					//TODO Should be split up into modules later, so far just a big collection of everything
					qgisWriter.writeEverything(writer, osmMapnikFile);

//					handler.endProjectLayers(writer);
//					handler.writeProperties(writer);
					qgisWriter.endDocument(writer);
					
					writer.flush();
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException( "writing QGis project file did not work") ;
				}
				
				
				// now creating a snapshot based on above-created project file
				// Remember to set the PATH varibales correctly to be able to call "qgis.bat" on the command line
				String cmd = "qgis.bat " + config.controler().getOutputDirectory() + actType + "/" + mode + "/QGisProjectFile.qgs" +
						" --snapshot " + config.controler().getOutputDirectory() + actType + "/" + mode + "/snapshot.png";
				
				String stdoutFileName = config.controler().getOutputDirectory() + actType + "/" + mode + "/snapshot.log";
				int timeout = 99999;
		
				ExeRunner.run(cmd, stdoutFileName, timeout);
			}	
		}		
	}
}
