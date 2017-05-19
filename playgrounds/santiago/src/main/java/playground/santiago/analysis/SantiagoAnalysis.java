package playground.santiago.analysis;

import java.io.File;
import java.util.Locale;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;

import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.scenarios.munich.analysis.modular.legModeDistanceDistribution.RunLegModeDistanceDistribution;

/** 
 * 
 * Class to write ALL THE NECESSARY ANALYSIS FILES
 * 
 */

public class SantiagoAnalysis {
	//Fields related to the scenario and its steps - they must be changed depending on the step.
	private static final String CASE_NAME = "baseCase10pct";
	private static final String STEP_NAME = "Step1xB";
	private static final int FIRST_IT = 600;
	private static final int LAST_IT = 800;
	private static final int LENGTH = LAST_IT - FIRST_IT;
	
	//Fields related to the outputDir - Do not change these.
	private static final String RUN_DIR = "../../../runs-svn/santiago/" + CASE_NAME + "/";	
	private static final String OUTPUT_FOLDER = RUN_DIR + "outputOf" + STEP_NAME + "/";	
	private static final String ANALYSIS_DIR = OUTPUT_FOLDER + "analysis/";

	//Fields related to the inputFiles	
	private static final String NET_FILE = OUTPUT_FOLDER + "output_network.xml.gz";
	private static final String COUNTS_FILE = OUTPUT_FOLDER + "output_counts.xml.gz";
	

//	private static final String PLANS = IT_FOLDER + IT_NUMBER +".plans.xml.gz";
//	private static final String CONFIG_FILE = RUN_DIR + "config_" + CASE_NAME + "_sim7.xml";
//	private static final String CONFIG_FILE = RUN_DIR + "configStepP1_1.xml";
	
	
	private static final UserGroup USER_GROUP = null;
	
	
	public static void main (String[]arg){
		
		int it = 0;	
		
		while (it<=LENGTH){
		int itAux = FIRST_IT;
		String itFolder = OUTPUT_FOLDER + "ITERS/it." + it + "/";		
		String events = itFolder + it +".events.xml.gz";	
		String modalShareOutputDir = ANALYSIS_DIR +"modalSplit_It"+ itAux  + ".txt";
		String countsCompareOutputDir = ANALYSIS_DIR + itAux + ".countscompare.txt";		
		writeModalShare(events, modalShareOutputDir);
		writeCountsCompare(events, countsCompareOutputDir);
		it = it + 50;
		}




//		writeModeDistanceDistribution();
//		writeModalTravelTimes();

	}
	
	private static void createDir(File file) {
		file.mkdirs();	
	}
	
	
	public static void writeModalShare(String eventsFile , String outputFile){
			
		File analysisDir = new File(ANALYSIS_DIR);
		if(!analysisDir.exists()) createDir(analysisDir);			
		ModalShareFromEvents msc = new ModalShareFromEvents(eventsFile);
		msc.run();
		msc.writeResults(outputFile);

			

	}
	
	private static void writeCountsCompare (String eventsFile, String outputFile){
		Network network = readNetwork( NET_FILE );
		Counts counts = readCounts( COUNTS_FILE );
		VolumesAnalyzer volumes = readVolumes( network , eventsFile );
		double scaleFactor;
		if (CASE_NAME.substring(8,10).equals("10")){
			scaleFactor = 10;
		}else{
			scaleFactor = 1;
		}


	
	final CountsComparisonAlgorithm cca =
			new CountsComparisonAlgorithm(
					volumes,
					counts,
					network,
					scaleFactor );

		cca.run();
		
		try {
			final CountSimComparisonTableWriter ctw=
				new CountSimComparisonTableWriter(
						cca.getComparison(),
						Locale.ENGLISH);
			ctw.writeFile( outputFile );
		}
		catch ( Exception e ) {

		}
	}
	
	private static VolumesAnalyzer readVolumes ( Network network, String eventsFile ) {
		final VolumesAnalyzer volumes = new VolumesAnalyzer( 3600 , 24 * 3600 - 1 , network );
		final EventsManager events = EventsUtils.createEventsManager();
		events.addHandler( volumes );
		new MatsimEventsReader( events ).readFile( eventsFile );
		return volumes;
	}
	
	private static Counts readCounts(final String countsFile) {
		final Counts counts = new Counts();
		new MatsimCountsReader( counts ).readFile( countsFile );
		return counts;
	}

	private static Network readNetwork(final String netFile) {
		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader(sc.getNetwork()).readFile( netFile );
		return sc.getNetwork();
	}


	
	
//	public static void writeModalTravelTimes(){
//		
//			String eventsFile = EVENTS;			
//			ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(eventsFile);		
//			mtta.run();
//			String outputFile = ANALYSIS_DIR +"modalTravelTimes_It"+ IT_NUMBER  + ".txt";
//			mtta.writeResults(outputFile);
//		}
//	
//	public static void writeModeDistanceDistribution(){
//	RunLegModeDistanceDistribution rlmdd = new RunLegModeDistanceDistribution(OUTPUT_FOLDER, CONFIG_FILE, IT_NUMBER, USER_GROUP);	
//	rlmdd.run();
//	}
	
	

	

}
