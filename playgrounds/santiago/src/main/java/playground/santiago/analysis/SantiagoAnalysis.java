package playground.santiago.analysis;

import java.io.File;

import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.scenarios.munich.analysis.modular.legModeDistanceDistribution.RunLegModeDistanceDistribution;

/** 
 * 
 * Class to write ALL THE NECESSARY ANALYSIS FILES
 * 
 */

public class SantiagoAnalysis {
	
	private static final String CASE_NAME = "baseCase10pct";
	
	private static final String RUN_DIR = "../../../runs-svn/santiago/" + CASE_NAME + "/";
	private static final String OUTPUT_FOLDER = RUN_DIR + "outputOfStep0_24T0/";	
	private static final String ANALYSIS_DIR = OUTPUT_FOLDER + "analysis/";

//	private static final String PLANS = IT_FOLDER + IT_NUMBER +".plans.xml.gz";
//	private static final String CONFIG_FILE = RUN_DIR + "config_" + CASE_NAME + "_sim7.xml";
//	private static final String CONFIG_FILE = RUN_DIR + "configStepP1_1.xml";
	
	
	private static final UserGroup USER_GROUP = null;
	
	
	private static void createDir(File file) {
		file.mkdirs();	
	}
	
	
	public static void writeModalShare( String eventsFile , String outputFile ){
			
		File analysisDir = new File(ANALYSIS_DIR);
		if(!analysisDir.exists()) createDir(analysisDir);			
		ModalShareFromEvents msc = new ModalShareFromEvents(eventsFile);
		msc.run();
		msc.writeResults(outputFile);

			

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
	
	
	
	public static void main (String[]arg){
		
		int it = 0;		
		while (it<=300){
		int itAux = 300 + it;
		String itFolder = OUTPUT_FOLDER + "ITERS/it." + it + "/";		
		String events = itFolder + it +".events.xml.gz";	
		String outputDir = ANALYSIS_DIR +"modalSplit_It"+ itAux  + ".txt";		
		writeModalShare(events, outputDir);		
		it = it + 50;
		}
//		writeModeDistanceDistribution();
//		writeModalTravelTimes();

	}
	

}
