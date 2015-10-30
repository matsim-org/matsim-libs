package playground.gleich.analyzer.analysisRunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.vehicles.Vehicles;

import playground.vsp.analysis.modules.act2mode.ActivityToModeAnalysis;
import playground.vsp.analysis.modules.boardingAlightingCount.BoardingAlightingCountAnalyzer;
import playground.vsp.analysis.modules.bvgAna.delayAtStopHistogram.VehDelayAtStopHistogramAnalyzer;
import playground.vsp.analysis.modules.bvgAna.ptTripTravelTime.PtTripTravelTimeTransfersAnalyzer;
import playground.vsp.analysis.modules.carDistance.CarDistanceAnalyzer;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;
import playground.vsp.analysis.modules.legModeDistanceDistribution.LegModeDistanceDistribution;
import playground.vsp.analysis.modules.monetaryTransferPayments.MonetaryPaymentsAnalyzer;
import playground.vsp.analysis.modules.networkAnalysis.NetworkAnalyzer;
import playground.vsp.analysis.modules.plansSubset.GetPlansSubset;
import playground.vsp.analysis.modules.ptAccessibility.PtAccessibility;
import playground.vsp.analysis.modules.ptCircuityAnalyzer.PtCircuityAnalyzer;
import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdAnalyzer;
import playground.vsp.analysis.modules.ptLines2PaxAnalysis.PtLines2PaxAnalysis;
import playground.vsp.analysis.modules.ptOperator.PtOperatorAnalyzer;
import playground.vsp.analysis.modules.ptPaxVolumes.PtPaxVolumesAnalyzer;
import playground.vsp.analysis.modules.ptRoutes2paxAnalysis.PtRoutes2PaxAnalysis;
import playground.vsp.analysis.modules.ptTravelStats.TravelStatsAnalyzer;
import playground.vsp.analysis.modules.ptTripAnalysis.traveltime.TTtripAnalysis;
import playground.vsp.analysis.modules.stuckAgents.GetStuckEventsAndPlans;
import playground.vsp.analysis.modules.transitSchedule2Shp.TransitSchedule2Shp;
import playground.vsp.analysis.modules.transitScheduleAnalyser.TransitScheduleAnalyser;
import playground.vsp.analysis.modules.transitVehicleVolume.TransitVehicleVolumeAnalyzer;
import playground.vsp.analysis.modules.travelTime.TravelTimeAnalyzer;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsAnalyzer;
import playground.vsp.analysis.modules.waitingTimes.WaitingTimesAnalyzer;
import playground.vsp.analysis.modules.welfareAnalyzer.WelfareAnalyzer;

/**
 * Work Flow at version 12.03.2014:
 * 
 * <ul>
 * <li>1. AnalysisRunner.java loads scenario
 * <li>2. AnalysisRunner.java runs methods r"AnalyzerName"() to run all working 
 * analyzers, output is saved in outputDirectory/"AnalyzerName"
 * <li>3. AnalysisRunner.java creates an instance of ReportGenerator.java and runs it
 * <li>4. ReportGenerator.java copies RScripts and Latex files from a local 
 * directory (to be replaced by an svn path shared-svn...studies/gleich/MATSimAnalyzer) 
 * to a local working copy at outputDirectory/Latex and outputDirectory/RScripts
 * <li>5. ReportGenerator.java launches the RScript executable which runs analysis_main.R 
 * at outputDirectory/Rscripts
 * <li>5.1. RScript follows relative paths stated in analysis_main.R to the R Scripts 
 * for the VspAnalyzer output
 * <li>5.2. RScript executable creates plots as .pdf
 * <li>6. ReportGenerator.java launches pdflatex executable which runs 
 * VspAnalyzerAutomaticReport.tex
 * <li>6.1. pdflatex executable follows relative paths stated in 
 * VspAnalyzerAutomaticReport.tex to VspAnalyzerAutomaticReport_main.tex and further 
 * on to Latex files for the VspAnalyzer and Rscript output
 * <li>6.2. pdflatex creates a single report file including the results of all analyzers
 * outputDirectory/VspAnalyzerAutomaticReport.pdf
 * </ul>
 * 
 * There is for each working VspAnalyzer one latex file outputDirectory/Latex/"analyzerName".tex.
 * For some Analyzers there is one Rscript outputDirectory/RScripts/"analyzerName.R".
 * 
 * AnalysisRunner.java, ReportGenerator.java, all R and latex files and all paths 
 * work for the example scenario at gleich's computer at version 12.03.2014. However,
 * the current legModeDistanceDistribution.R needs LegModeDistanceDistribution to 
 * write its output file with the "#" in the header replaced by 'distance' 
 * (R interpretes '#' as comment line).<p>
 * The example scenario can be found in shared-svn.../studies/gleich/MATSimExampleScenario
 * <p>
 * 
 * MATsim output of the ExampleScenario runs and corresponding output of AnalysisRunner can
 * be found in shared-svn/studies/gleich/MATSimExampleScenario.
 * 
 * 
 * TODO 12.03.2014:
 * <ul> 
 * <li>Separate Run of VspAnalyzers from Starting R and Latex
 * <li>Separate methods to run the analyzers from this class into one class per analyzer
 * (in the package of the analysis module)
 * <li>Have the R scripts and latex files created by Java by one class per analyzer
 * (in the package of the analysis module)
 * <li>After the VspAnalyzers were run by one of the above mentioned classes or manually
 * somehow else, Java should look in the output data which the VspAnalyzers created and
 * check for output names which match with preconfigured standardized names for each
 * analyzer output. In case the analyzer was run, Java finds its output names and writes
 * R scripts and latex files corresponding to this analyzer. Otherwise this analyzer is 
 * not included in the final VspAnalyzerAutomaticReport.pdf
 * <li>Have the documentary comment of the analysis modules inserted automatically into
 * the latex files in order to have them updated to the most recent version in the 
 * resulting pdf report.
 * </ul>
 * Problems concerning analyzer modules:
 * <ul>
 * <li>Some analyzers crash during runtime (PtTravelStats, TransitVehicleVolume) due to
 * a conflict between Matsim class "Counts" which does not allow counts starting at 0 and
 * these analyzers which crash while trying to create Counts starting at 0.
 * <li>NetworkAnalyzer seems to have problems to retrieve the coordinate reference system
 * <li>PtTripAnalysis (commented out) and BvgAna are not included
 * </ul>
 * Problems concerning the exampleScenario
 * <ul>
 * <li>There are no transfers between different TransitLines, this might make the example
 * scenario unsuitable for some analyzers.
 * <li>Monetary Payments and Welfare analyzers deliver almost empty output
 * <li>ptSimpleTripAnalyzer needs a scenario without transitLines
 * </ul>
 * 
 * Some analysis modules only create .shp files which are not yet included in the report.
 * Due to a bug in Qgis, it cannot be used from command line on windows to create
 * graphics to be included in latex. According to ikaddoura and aneumann shp files 
 * (respectively graphics produced from shp files ) do not need to be inserted in the report.
 * <p>
 * 
 * End of TODO list and description of version 12.03.2014
 * -------
 * Begin normal documentary comment
 * -------
 * Runs most analyzers available in the vsp playground and invokes the
 * {@link ReportGenerator} to create a working copy of R and Latex scripts and
 * finally a <i>pdf-document which shows the results of some
 * analyzers</i>.
 * <p>
 * Some Methods may still need special configuration depending on the scenario
 * to be analysed: 
 * <ul>
 * <li> addition of distance and activity clusters at rPtAccesibility()
 * <li> setting time intervals to be analysed at rAct2Mode(), 
 * rAct2ModeWithPlanCoord(), rBoardingAlightingCountAnalyzer(),
 * rPtPaxVolumes(), rPtRoutes2PaxAnalysis(), 
 * rPtTravelStats(), rTransitVehicleVolume()
 * </ul>
 * 
 * Make sure to have all necessary R packages installed and accessible (see
 * {@link ReportGenerator#runRScripts()}). PtRoutes2Pax and PtRoutes2Lines use
 * packages requiring Perl, so R needs an valid path to an Perl interpreter.
 * However, apart from an error message, there seems to be no obvious
 * difference between running R with a Perl interpreter and without.
 *
 * @param <b>outputDirectory</b>: a path to a network drive may cause problems when
 * ReportGenerator invokes R and Latex
 * @param pathToScenario data: plans, network, vehicles, transitSchedule
 * @param <b>eventFile</b>: it has to correspond to the plan file (plans from
 *  iteration x and events from iteration x) otherwise some analyzers may produce
 *  unreliable results
 * @param pathToRScriptFiles
 * @param pathToLatexFiles
 * @param pathToRScriptExe
 * @param pathToPdfLatexExe
 * @param coordinateSystem (as String)
 * @param useConfigXml : Shall the config be loaded from an xml file to be found at
 * pathToScenarioData
 * 
 * @author gleich
 */
public class AnalysisRunner {
	
	private boolean useConfigXml;
	private String pathToScenarioData = "Z:/WinHome/ArbeitWorkspace/Analyzer/";
	private String eventFile = pathToScenarioData 
			+ "output/testOneBusManyIterations/ITERS/it.10/10.events.xml.gz";
	private String outputDirectory;
	private String pathToRScriptFiles = "Z:/WinHome/ArbeitWorkspace/Analyzer/output/Rscripts";
	private String pathToLatexFiles = "Z:/WinHome/ArbeitWorkspace/Analyzer/output/Latex";
	private String pathToRScriptExe = "C:/Program Files/R/R-2.14.2/bin/Rscript.exe";
	private String pathToPdfLatexExe = "pdflatex";
	private String coordinateSystem = "DHDN_GK4";//DHDN_GK4
	
	private Scenario scenario;
	
	public static void main(String[] args) throws IOException{
		AnalysisRunner analysisRun = new AnalysisRunner();
		analysisRun.runAllAnalyzersAndGenerateReport();
	}
	
	public AnalysisRunner(){
		/* Set outputDirectory to the users desktop ---- TODO: Test Linux, Mac----
		 * using the desktop as output directory appears to cause runTime
		 * exceptions of the cmd.exe on the workplace pc when ReportGenerator
		 * tries to start R and Latex :
		 * CMD.EXE wurde mit dem oben angegebenen Pfad als aktuellem Verzeichnis gestartet. UNC-Pfade werden nicht unterstützt.
		 * possible solutions proposed on the internet include modifying the registry */
		String operatingSystem = System.getProperty("os.name");
		if(operatingSystem.equals("Windows 7") || operatingSystem.equals("Windows Vista") || operatingSystem.equals("Windows XP")){
			outputDirectory = System.getProperty("user.home") + "/desktop/MATSimAnalyzer";
			(new File(System.getProperty("user.home") + "/desktop/MATSimAnalyzer")).mkdir();
		} else {
			outputDirectory = System.getProperty("user.home") + "/Desktop/MATSimAnalyzer";
			(new File(System.getProperty("user.home") + "/desktop/MATSimAnalyzer")).mkdir();
		}
		outputDirectory = "Z:/WinHome/MATSimAnalyzer4";
		(new File("Z:/WinHome/MATSimAnalyzer4")).mkdir();
		this.initialize();
	}
	
	public AnalysisRunner(String pathToExampleScenario, String eventFile,
			String outputDirectory, String pathToRScriptFiles, 
			String pathToLatexFiles, String pathToRScriptExe, 
			String pathToPdfLatexExe, String coordinateSystem, 
			boolean useConfigXml){
		this.pathToScenarioData = pathToExampleScenario;
		this.eventFile = eventFile;
		this.outputDirectory = outputDirectory;
		this.pathToRScriptFiles = pathToRScriptFiles;
		this.pathToLatexFiles = pathToLatexFiles;
		this.pathToRScriptExe = pathToRScriptExe;
		this.pathToPdfLatexExe = pathToPdfLatexExe;
		this.coordinateSystem = coordinateSystem;
		this.useConfigXml = useConfigXml;
		this.initialize();
	}
	
	private void initialize(){
		Config config;
		if(useConfigXml){
			config = ConfigUtils.loadConfig(pathToScenarioData);// + "input/config_exampleScenario.xml"
		} else {
			config = ConfigUtils.createConfig();
//			Config config = ConfigUtils.loadConfig(pathToExampleScenario + "input/ExampleScenario/config_exampleScenario.xml");
			config.network().setInputFile(pathToScenarioData + "input/network.xml");
			config.transit().setTransitScheduleFile(pathToScenarioData + "input/transitSchedule.xml");
			config.transit().setVehiclesFile(pathToScenarioData + "input/Vehicles.xml");
			config.transit().setUseTransit(true);
			config.scenario().setUseVehicles(true);
			
			/* Some analyzers read the result plan files.
			 * The Plan File should include the plans used for the iteration
			 * whose eventfile is to be analysed. */
			config.plans().setInputFile(pathToScenarioData + "output/testOneBusManyIterations/ITERS/it.10/10.plans.xml.gz");//pathToScenarioData + "output/ExampleScenario/1.plans.xml.gz"
		}
		scenario = ScenarioUtils.loadScenario(config);
	}
	
	private void runAllAnalyzersAndGenerateReport() throws IOException{
		rAct2Mode();
		rAct2ModeWithPlanCoord();
		rBoardingAlightingCountAnalyzer();
		rBvgAna();// mehrere Analyzer
		rCarDistanceAnalyzer();
		/*rEmissionsAnalyzer(); */ //not included: needs an emission events file created with the package emissionsWriter which needs settings made with VspExperimentalConfigGroup which is not intended for public use
		rLegModeDistanceDistribution(); // reads the plans in the scenario -> scenario should include the most recent plans (set in config)
		rMonetaryPaymentsAnalyzer();

		rNetworkAnalyzer();
		/*
		While retrieving the coordinate reference system, the inserted 
		"DHDN_GK4" is looked up in map MGC.transformations where this key 
		exists, however somewhere on the way to geotool's CRSUtils this 
		information on the coordinate reference system gets lost or is
		ignored or is invalid and CRSUtils adds "0" to the fixed String "EPSG:"
		leading to the problem that the coordinate reference system looked for
		is "EPSG:0" which apparently cannot be retrieved. 
		
		2014-02-18 11:17:03,639  INFO MatsimXmlParser:218 Trying to load http://matsim.org/files/dtd/network_v1.dtd. In some cases (e.g. network interface up but no connection), this may take a bit.
2014-02-18 11:17:03,686  INFO NetworkImpl:488 building QuadTree for nodes: xrange(999.0,4001.0); yrange(999.0,4001.0)
2014-02-18 11:17:03,780  INFO NetworkImpl:497 Building QuadTree took 0.094 seconds.
2014-02-18 11:17:03,858  INFO NetworkAnalyzer:208   checking 19 nodes and 64 links for dead-ends...
2014-02-18 11:17:03,951  INFO NetworkAnalyzer:220 size of biggest cluster equals network size... continuing with routable network...
2014-02-18 11:17:03,951  INFO NetworkAnalyzer:279 analysing network nodes...
2014-02-18 11:17:03,951  INFO NetworkAnalyzer:333 found 0 exit road nodes, 0 dead end nodes and 0 redundant nodes...
2014-02-18 11:17:03,951  INFO NetworkAnalyzer:334 ...done
2014-02-18 11:17:03,951  INFO NetworkAnalyzer:346 analyzing network links...
2014-02-18 11:17:03,951  INFO NetworkAnalyzer:371 0 warnings about storage capacity written...
2014-02-18 11:17:03,951  INFO NetworkAnalyzer:372 ...done
2014-02-18 11:17:04,060  WARN MyBoundingBox:47 Setting bounding box from network! For large networks this may lead to memory issues depending on available memory and/or grid resolution. In this case define a custom bounding box.
2014-02-18 11:17:04,060  INFO MyBoundingBox:51 ... done!
|--------------------------------------------------------------------------------------------------|
||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

Feb 18, 2014 11:17:04 AM org.geotools.referencing.factory.epsg.ThreadedEpsgFactory <init>
Information: Setting the EPSG factory org.geotools.referencing.factory.epsg.DefaultFactory to a 1800000ms timeout
Feb 18, 2014 11:17:04 AM org.geotools.referencing.factory.epsg.ThreadedEpsgFactory <init>
Information: Setting the EPSG factory org.geotools.referencing.factory.epsg.ThreadedHsqlEpsgFactory to a 1800000ms timeout
Feb 18, 2014 11:17:04 AM org.geotools.referencing.factory.epsg.ThreadedHsqlEpsgFactory createDataSource
Information: Building new data source for org.geotools.referencing.factory.epsg.ThreadedHsqlEpsgFactory
Feb 18, 2014 11:17:18 AM org.geotools.referencing.factory.epsg.ThreadedHsqlEpsgFactory createBackingStore
Information: Building backing store for org.geotools.referencing.factory.epsg.ThreadedHsqlEpsgFactory
2014-02-18 11:25:31,715  WARN CRSUtils:73 No code "EPSG:0" from authority "European Petroleum Survey Group" found for object of type "CoordinateReferenceSystem".
|--------------------------------------------------------------------------------------------------|
2014-02-18 11:40:39,812  INFO NetworkImpl:519 building LinkQuadTree for nodes: xrange(999.0,4001.0); yrange(999.0,4001.0)
2014-02-18 11:40:40,093  INFO NetworkImpl:525 Building LinkQuadTree took 0.281 seconds.
||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

2014-02-18 11:40:55,787  INFO NetworkAnalyzer:151 total length of all network links: 68420.0 m
2014-02-18 11:40:55,787  INFO NetworkAnalyzer:152 total geometric length of all network links: 58092.457716446464 m
2014-02-18 11:41:12,184  INFO ShapeFileWriter:53 Writing shapefile to Z:/WinHome/MATSimAnalyzer/NetworkAnalyzer/envelope.shp
	*/
		rPlansSubset();
		rPtAccesibility();
		rPtCircuityAnalysis();
		rPtDriverPrefix();
		rPtLines2PaxAnalysis();
		rPtOperator();
		rPtPaxVolumes();
		rPtRoutes2PaxAnalysis();
		/*
		rPtTravelStats();
		Exception in thread "main" java.lang.RuntimeException: counts start at 1, not at 0.  If you have a use case where you need to go below one, let us know and we think about it, but so far we had numerous debugging sessions because someone inserted counts at 0.
	at org.matsim.counts.Count.createVolume(Count.java:53)
	at playground.vsp.analysis.modules.ptTravelStats.TravelStatsHandler.handleEvent(TravelStatsHandler.java:149)
	at org.matsim.core.events.EventsManagerImpl.callHandlerFast(EventsManagerImpl.java:293)
	at org.matsim.core.events.EventsManagerImpl.computeEvent(EventsManagerImpl.java:222)
	at org.matsim.core.events.EventsManagerImpl.processEvent(EventsManagerImpl.java:136)
	at org.matsim.core.events.EventsReaderXMLv1.startEvent(EventsReaderXMLv1.java:92)
	at org.matsim.core.events.EventsReaderXMLv1.startTag(EventsReaderXMLv1.java:67)
	at org.matsim.core.events.MatsimEventsReader$XmlEventsReader.startTag(MatsimEventsReader.java:84)
	at org.matsim.core.utils.io.MatsimXmlParser.startElement(MatsimXmlParser.java:290)
	at org.apache.xerces.parsers.AbstractSAXParser.startElement(Unknown Source)
	at org.apache.xerces.parsers.AbstractXMLDocumentParser.emptyElement(Unknown Source)
	at org.apache.xerces.impl.XMLNSDocumentScannerImpl.scanStartElement(Unknown Source)
	at org.apache.xerces.impl.XMLDocumentFragmentScannerImpl$FragmentContentDispatcher.dispatch(Unknown Source)
	at org.apache.xerces.impl.XMLDocumentFragmentScannerImpl.scanDocument(Unknown Source)
	at org.apache.xerces.parsers.XML11Configuration.parse(Unknown Source)
	at org.apache.xerces.parsers.XML11Configuration.parse(Unknown Source)
	at org.apache.xerces.parsers.XMLParser.parse(Unknown Source)
	at org.apache.xerces.parsers.AbstractSAXParser.parse(Unknown Source)
	at org.apache.xerces.jaxp.SAXParserImpl$JAXPSAXParser.parse(Unknown Source)
	at javax.xml.parsers.SAXParser.parse(Unknown Source)
	at org.matsim.core.utils.io.MatsimXmlParser.parse(MatsimXmlParser.java:176)
	at org.matsim.core.utils.io.MatsimXmlParser.parse(MatsimXmlParser.java:146)
	at org.matsim.core.events.MatsimEventsReader$XmlEventsReader.readFile(MatsimEventsReader.java:105)
	at org.matsim.core.events.MatsimEventsReader.readFile(MatsimEventsReader.java:64)
	at analyzerExampleScenario.RunAnalyzer.rPtTravelStats(RunAnalyzer.java:436)
	at analyzerExampleScenario.RunAnalyzer.main(RunAnalyzer.java:155)
	*/
		/*rPtTripAnalysis(); */ // not working and not included
		rStuckAgents();
		rTransitSchedule2Shp();
		rTransitScheduleAnalyser();
		/*
		rTransitVehicleVolume();
		Exception in thread "main" java.lang.RuntimeException: counts start at 1, not at 0.  If you have a use case where you need to go below one, let us know and we think about it, but so far we had numerous debugging sessions because someone inserted counts at 0.
	at org.matsim.counts.Count.createVolume(Count.java:53)
	at playground.vsp.analysis.modules.transitVehicleVolume.TransitVehicleVolumeHandler.handleEvent(TransitVehicleVolumeHandler.java:77)
	at org.matsim.core.events.EventsManagerImpl.callHandlerFast(EventsManagerImpl.java:293)
	at org.matsim.core.events.EventsManagerImpl.computeEvent(EventsManagerImpl.java:222)
	at org.matsim.core.events.EventsManagerImpl.processEvent(EventsManagerImpl.java:136)
	at org.matsim.core.events.EventsReaderXMLv1.startEvent(EventsReaderXMLv1.java:92)
	at org.matsim.core.events.EventsReaderXMLv1.startTag(EventsReaderXMLv1.java:67)
	at org.matsim.core.events.MatsimEventsReader$XmlEventsReader.startTag(MatsimEventsReader.java:84)
	at org.matsim.core.utils.io.MatsimXmlParser.startElement(MatsimXmlParser.java:290)
	at org.apache.xerces.parsers.AbstractSAXParser.startElement(Unknown Source)
	at org.apache.xerces.parsers.AbstractXMLDocumentParser.emptyElement(Unknown Source)
	at org.apache.xerces.impl.XMLNSDocumentScannerImpl.scanStartElement(Unknown Source)
	at org.apache.xerces.impl.XMLDocumentFragmentScannerImpl$FragmentContentDispatcher.dispatch(Unknown Source)
	at org.apache.xerces.impl.XMLDocumentFragmentScannerImpl.scanDocument(Unknown Source)
	at org.apache.xerces.parsers.XML11Configuration.parse(Unknown Source)
	at org.apache.xerces.parsers.XML11Configuration.parse(Unknown Source)
	at org.apache.xerces.parsers.XMLParser.parse(Unknown Source)
	at org.apache.xerces.parsers.AbstractSAXParser.parse(Unknown Source)
	at org.apache.xerces.jaxp.SAXParserImpl$JAXPSAXParser.parse(Unknown Source)
	at javax.xml.parsers.SAXParser.parse(Unknown Source)
	at org.matsim.core.utils.io.MatsimXmlParser.parse(MatsimXmlParser.java:176)
	at org.matsim.core.utils.io.MatsimXmlParser.parse(MatsimXmlParser.java:146)
	at org.matsim.core.events.MatsimEventsReader$XmlEventsReader.readFile(MatsimEventsReader.java:105)
	at org.matsim.core.events.MatsimEventsReader.readFile(MatsimEventsReader.java:64)
	at analyzerExampleScenario.RunAnalyzer.rTransitVehicleVolume(RunAnalyzer.java:488)
	at analyzerExampleScenario.RunAnalyzer.main(RunAnalyzer.java:159)
	*/
		rTravelTimeAnalyzer();
		rUserBenefits();
		rWaitingTimes();
		rWelfareAnalyzer();
		
		ReportGenerator reporter = new ReportGenerator(pathToRScriptFiles,
				pathToLatexFiles, pathToRScriptExe, pathToPdfLatexExe, 
				outputDirectory);
		reporter.generateReport();
	}
	
	private void rAct2Mode() {
		//works without facilities, although stated to be necessary in ActivityToModeAnalysis
		Set<Id> personsOfInterest = new TreeSet<Id>();

		for(Id id: scenario.getPopulation().getPersons().keySet()){
			personsOfInterest.add(id);
		}
		ActivityToModeAnalysis analysis = new ActivityToModeAnalysis(scenario, personsOfInterest, 30*60, coordinateSystem);
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = analysis.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		analysis.postProcessData();
		(new File(outputDirectory + "/Act2Mode")).mkdir(); 
		analysis.writeResults(outputDirectory + "/Act2Mode/");
	}

	private void rAct2ModeWithPlanCoord() {
		Set<Id> personsOfInterest = new TreeSet<Id>();
		//personsOfInterest.add(Id.create("car_11_to_12_Nr100"));
		
		for(Id id: scenario.getPopulation().getPersons().keySet()){
			personsOfInterest.add(id);
		}
		
		ActivityToModeAnalysis analysis = new ActivityToModeAnalysis(scenario, personsOfInterest, 30*60, coordinateSystem);
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = analysis.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		analysis.postProcessData();
		(new File(outputDirectory + "/Act2ModeWithPlanCoord")).mkdir(); 
		analysis.writeResultsPlanCoords(outputDirectory + "/Act2ModeWithPlanCoord/");
	}

	private void rBvgAna(){
		VehDelayAtStopHistogramAnalyzer cda = new VehDelayAtStopHistogramAnalyzer(100);
		cda.init((MutableScenario) scenario);
		cda.preProcessData();
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = cda.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		cda.postProcessData();
		(new File(outputDirectory + "/BvgAna")).mkdir(); 
		cda.writeResults(outputDirectory + "/BvgAna/");
		
		PtTripTravelTimeTransfersAnalyzer transfer = new PtTripTravelTimeTransfersAnalyzer();
		transfer.init((MutableScenario) scenario);
		transfer.preProcessData();
		EventsManager events2 = EventsUtils.createEventsManager();
		List<EventHandler> handler2 = transfer.getEventHandler();
		for(EventHandler eh : handler2){
			events2.addHandler(eh);
		}
		MatsimEventsReader reader2 = new MatsimEventsReader(events2);
		reader2.readFile(eventFile);
		transfer.postProcessData();
		transfer.writeResults(outputDirectory + "/BvgAna/");
		//pTripTransfers.txt empty due to no transfers in the example Scenario
	}

	private void rBoardingAlightingCountAnalyzer(){
		BoardingAlightingCountAnalyzer ba = new BoardingAlightingCountAnalyzer(
				scenario, 30*60, coordinateSystem);
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = ba.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		ba.postProcessData();
		(new File(outputDirectory + "/BoardingAlightingCountAnalyzer")).mkdir(); 
		ba.writeResults(outputDirectory + "/BoardingAlightingCountAnalyzer/");
	}
	
	private void rCarDistanceAnalyzer() {
		CarDistanceAnalyzer cda = new CarDistanceAnalyzer();
		cda.init((MutableScenario) scenario);
		cda.preProcessData();
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = cda.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		cda.postProcessData();
		(new File(outputDirectory + "/CarDistanceAnalyzer")).mkdir(); 
		cda.writeResults(outputDirectory + "/CarDistanceAnalyzer/");
	}
	
	private void rEmissionsAnalyzer() {
		 //insert emissions event file
		EmissionsAnalyzer ema = new EmissionsAnalyzer("Z:/WinHome/ArbeitWorkspace/Analyzer/output/test1/ITERS/it.10/10.events.xml.gz");
		ema.init((MutableScenario) scenario);
		ema.preProcessData();
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = ema.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		ema.postProcessData();
		(new File(outputDirectory + "/EmissionsAnalyzer")).mkdir(); 
		ema.writeResults(outputDirectory + "/EmissionsAnalyzer/");
	}
	
	private void rLegModeDistanceDistribution(){
		LegModeDistanceDistribution lmdd = new LegModeDistanceDistribution();
		lmdd.init(scenario);
		lmdd.postProcessData();
		(new File(outputDirectory + "/LegModeDistanceDistribution")).mkdir(); 
		lmdd.writeResults(outputDirectory + "/LegModeDistanceDistribution/");
	}
	
	private void rMonetaryPaymentsAnalyzer(){
		MonetaryPaymentsAnalyzer mpa = new MonetaryPaymentsAnalyzer();
		mpa.init((MutableScenario) scenario);
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = mpa.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		mpa.postProcessData();
		(new File(outputDirectory + "/MonetaryPaymentsAnalyzer")).mkdir(); 
		mpa.writeResults(outputDirectory + "/MonetaryPaymentsAnalyzer/");
	}
	
	private void rNetworkAnalyzer(){//TODO: Change Analyzer to use the scenario as input instead of path to network file
		NetworkAnalyzer nea = new NetworkAnalyzer(pathToScenarioData + "input/network.xml", coordinateSystem);
		//no event handler
		nea.postProcessData();
		(new File(outputDirectory + "/NetworkAnalyzer")).mkdir(); 
		nea.writeResults(outputDirectory + "/NetworkAnalyzer/");
	}
	
	private void rPlansSubset(){
		Set<Id<Person>> selection = new TreeSet<>();
		selection.add(Id.create(2, Person.class));
		selection.add(Id.create(256, Person.class));
		GetPlansSubset gps = new GetPlansSubset(scenario, selection, true);
		//gps.preProcessData(); //unused, no EventHandler
		gps.postProcessData();
		(new File(outputDirectory + "/PlansSubset")).mkdir(); 
		gps.writeResults(outputDirectory + "/PlansSubset/");
	}
	
	private void rPtAccesibility(){
		List<Integer> distanceCluster = new ArrayList<Integer>();
		distanceCluster.add(200);
		distanceCluster.add(1000);
//		scenario.getConfig().planCalcScore().getActivityTypes()
		SortedMap<String, List<String>> activityCluster = new TreeMap<String, List<String>>();
		scenario.getConfig().planCalcScore().getActivityParams();
		List<String> home = new ArrayList<String>();
		home.add("h");
		List<String> work = new ArrayList<String>();
		work.add("w");
		List<String> shop = new ArrayList<String>();
		shop.add("s");//unused in scenario
		activityCluster.put("h", home);
		activityCluster.put("w", work);
		activityCluster.put("s", shop);
		PtAccessibility pta = new PtAccessibility(scenario, distanceCluster, 16, activityCluster, coordinateSystem, 10);
		pta.preProcessData();
		pta.postProcessData();
		(new File(outputDirectory + "/PtAccessibility")).mkdir(); 
		pta.writeResults(outputDirectory + "/PtAccessibility/");
	}
	
	private void rPtCircuityAnalysis(){
		MutableScenario sc = (MutableScenario) scenario;
		Vehicles vehicles = sc.getTransitVehicles();
		PtCircuityAnalyzer analysis = new PtCircuityAnalyzer(scenario, vehicles);
		analysis.preProcessData();
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = analysis.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		analysis.postProcessData();
		(new File(outputDirectory + "/PtCircuityAnalysis")).mkdir(); 
		analysis.writeResults(outputDirectory + "/PtCircuityAnalysis/");
	}
	
	private void rPtDriverPrefix(){
		PtDriverIdAnalyzer pda = new PtDriverIdAnalyzer();
		pda.init((MutableScenario)scenario);
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = pda.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		(new File(outputDirectory + "/PtDriverIdAnalyzer")).mkdir(); 
		pda.writeResults(outputDirectory + "/PtDriverIdAnalyzer/");//no output file, only console
	}
	
	private void rPtLines2PaxAnalysis(){
		
		Map<Id<TransitLine>, TransitLine> lines = scenario.getTransitSchedule().getTransitLines();
		//scenario.getScenarioElement(vehicles)
		MutableScenario sc = (MutableScenario) scenario;

		Vehicles vehicles = sc.getTransitVehicles();
		//(Map<Id, TransitLine> lines, Vehicles vehicles, double interval, int maxSlices)
		PtLines2PaxAnalysis ppa = new PtLines2PaxAnalysis(lines, vehicles, 60*60, 24*(60/60));
		
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = ppa.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		(new File(outputDirectory + "/PtLines2PaxAnalysis")).mkdir(); 
		ppa.writeResults(outputDirectory + "/PtLines2PaxAnalysis/");
		
		//Latex code to insert one example plot, implemented here because file names are known here
		BufferedWriter w = IOUtils.getBufferedWriter(outputDirectory + "/PtLines2PaxAnalysis/includeExamplePlot.tex");
		if(lines.size() > 0){
			Id exampleLine = lines.values().iterator().next().getId();
			Id exampleRoute = lines.get(exampleLine).getRoutes().values().iterator().next().getId();
			try {
				w.write("\\includegraphics[width=0.99\\textwidth, page=1]{" +
						"PtLines2PaxAnalysis/" + exampleLine.toString() +
						"--" + exampleRoute.toString() + ".pdf}\n" + 
						exampleLine.toString() + "\n");
				w.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try{
				w.write("No transit line found, therefore no plot to be shown.");
				w.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void rPtOperator(){
		PtOperatorAnalyzer poa = new PtOperatorAnalyzer();
		poa.init((MutableScenario)scenario);
		poa.preProcessData();
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = poa.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		poa.postProcessData();
		(new File(outputDirectory + "/PtOperator")).mkdir(); 
		poa.writeResults(outputDirectory + "/PtOperator/");
	}
	
	private void rPtPaxVolumes(){
		PtPaxVolumesAnalyzer ppv = new PtPaxVolumesAnalyzer(scenario, 30.0*60.0, coordinateSystem);
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = ppv.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		ppv.postProcessData();
		(new File(outputDirectory + "/PtPaxVolumes")).mkdir(); 
		ppv.writeResults(outputDirectory + "/PtPaxVolumes/");
	}
	
	private void rPtRoutes2PaxAnalysis(){
		
		Map<Id<TransitLine>, TransitLine> lines = scenario.getTransitSchedule().getTransitLines();
		//scenario.getScenarioElement(vehicles)
		MutableScenario sc = (MutableScenario) scenario;

		Vehicles vehicles = sc.getTransitVehicles();
		//(Map<Id, TransitLine> lines, Vehicles vehicles, double interval in seconds, int maxSlices)
		PtRoutes2PaxAnalysis ppa = new PtRoutes2PaxAnalysis(lines, vehicles, 60*60, 24*(60/60));
		
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = ppa.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		(new File(outputDirectory + "/PtRoutes2PaxAnalysis")).mkdir(); 
		ppa.writeResults(outputDirectory + "/PtRoutes2PaxAnalysis/");
		
		//Latex code to insert one example plot, implemented here because file names are known here
		BufferedWriter w = IOUtils.getBufferedWriter(outputDirectory + "/PtRoutes2PaxAnalysis/includeExamplePlot.tex");
		if(lines.size() > 0){
			Id<TransitLine> exampleLine = lines.values().iterator().next().getId();
			Id exampleRoute = lines.get(exampleLine).getRoutes().values().iterator().next().getId();
			try {
				w.write("\\includegraphics[width=0.99\\textwidth, page=1]{" +
						"PtRoutes2PaxAnalysis/" + exampleLine.toString() +
						"--" + exampleRoute.toString() + ".pdf}\n" + 
						exampleLine.toString() + " " + 
						exampleRoute.toString() + "\n");
				w.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try{
				w.write("No transit line found, therefore no plot to be shown.");
				w.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void rPtTravelStats(){
		TravelStatsAnalyzer tsa = new TravelStatsAnalyzer(scenario, 30.0*60.0);
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = tsa.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		(new File(outputDirectory + "/PtTravelStats")).mkdir(); 
		tsa.writeResults(outputDirectory + "/PtTravelStats/");
	}
	
	private void rPtTripAnalysis(){
//		List<String> ptModes = new LinkedList<String>();
		Set<String> ptModes = scenario.getConfig().transit().getTransitModes();
		System.out.println("ptModes: " + ptModes);
//		ptModes.add("bus");
//		ptModes.add("train");
		Collection<String> networkModes = scenario.getConfig().plansCalcRoute().getNetworkModes();
		System.out.println("networkModes: " + networkModes);
//		List<String> networkModes = new LinkedList<String>();
//		networkModes.add("train");
//		networkModes.add("car");
//		networkModes.add("bus");
		TTtripAnalysis analysis = new TTtripAnalysis(ptModes, networkModes, scenario.getPopulation());
		(new File(outputDirectory + "/PtTripAnalysis")).mkdir(); 
		analysis.writeResults(outputDirectory + "/PtTripAnalysis/");
	}
	
	private void rStuckAgents(){
		GetStuckEventsAndPlans stuck = new GetStuckEventsAndPlans(scenario);
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = stuck.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		stuck.postProcessData();
		(new File(outputDirectory + "/StuckAgents")).mkdir(); 
		stuck.writeResults(outputDirectory + "/StuckAgents/");
		//empty due to no stuck events to be written
	}
	
	private void rTransitSchedule2Shp(){
		TransitSchedule2Shp tshp = new TransitSchedule2Shp(scenario, coordinateSystem);
		(new File(outputDirectory + "/TransitSchedule2Shp")).mkdir(); 
		tshp.writeResults(outputDirectory + "/TransitSchedule2Shp/");
	}
	
	private void rTransitScheduleAnalyser(){
		TransitScheduleAnalyser tsa = new TransitScheduleAnalyser(scenario);
		(new File(outputDirectory + "/TransitScheduleAnalyser")).mkdir(); 
		tsa.writeResults(outputDirectory + "/TransitScheduleAnalyser/");
	}
	
	private void rTransitVehicleVolume(){
		TransitVehicleVolumeAnalyzer tsa = new TransitVehicleVolumeAnalyzer(scenario, 30.0*60.0, coordinateSystem);
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = tsa.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		tsa.postProcessData();
		(new File(outputDirectory + "/TransitVehicleVolume")).mkdir(); 
		tsa.writeResults(outputDirectory + "/TransitVehicleVolume/");
	}
		
	private void rTravelTimeAnalyzer() {
		TravelTimeAnalyzer tt = new TravelTimeAnalyzer();
		tt.init((MutableScenario) scenario);
		tt.preProcessData();
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = tt.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		tt.postProcessData();
		(new File(outputDirectory + "/TravelTimeAnalyzer")).mkdir(); 
		tt.writeResults(outputDirectory + "/TravelTimeAnalyzer/");
	}

	private void rUserBenefits() {
		UserBenefitsAnalyzer uba = new UserBenefitsAnalyzer();
		uba.init((MutableScenario) scenario);
		uba.preProcessData();
		(new File(outputDirectory + "/UserBenefits")).mkdir(); 
		uba.writeResults(outputDirectory + "/UserBenefits/");
		
	}
	
	private void rWaitingTimes(){
		WaitingTimesAnalyzer tt = new WaitingTimesAnalyzer();
		tt.init((MutableScenario) scenario);
		tt.preProcessData();
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = tt.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		tt.postProcessData();
		(new File(outputDirectory + "/WaitingTimes")).mkdir(); 
		tt.writeResults(outputDirectory + "/WaitingTimes/");
	}
	
	private void rWelfareAnalyzer(){
		WelfareAnalyzer tt = new WelfareAnalyzer();
		tt.init((MutableScenario) scenario);
		tt.preProcessData();
		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = tt.getEventHandler();
		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		tt.postProcessData();
		(new File(outputDirectory + "/WelfareAnalyzer")).mkdir(); 
		tt.writeResults(outputDirectory + "/WelfareAnalyzer/");
	}
}
