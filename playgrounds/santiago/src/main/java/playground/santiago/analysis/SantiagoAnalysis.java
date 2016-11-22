package playground.santiago.analysis;

import java.io.File;

import org.matsim.api.core.v01.Scenario;

import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.modalShare.ModalShareFromPlans;
import playground.agarwalamit.analysis.travelTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.utils.LoadMyScenarios;
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
	private static final String OUTPUT_FOLDER = RUN_DIR + "output_storage20pct/";	
	private static final String ANALYSIS_DIR = OUTPUT_FOLDER + "analysis/";
	
	private static final String IT_NUMBER = "100";
	private static final String IT_FOLDER = OUTPUT_FOLDER + "ITERS/it." + IT_NUMBER + "/";	
	
	private static final String EVENTS = IT_FOLDER + IT_NUMBER +".events.xml.gz";	
	private static final String PLANS = IT_FOLDER + IT_NUMBER +".plans.xml.gz";
	private static final String CONFIG_FILE = RUN_DIR + "config_" + CASE_NAME + "_sim7.xml";
	
	private static final UserGroup USER_GROUP = null;
	
	
	private static void createDir(File file) {
		file.mkdirs();	
	}
	
	
	public static void writeModalShare(){
			
		File analysisDir = new File(ANALYSIS_DIR);		
		String eventsFile = EVENTS;	

		if(!analysisDir.exists()) createDir(analysisDir);	
		
		String outputFile = ANALYSIS_DIR +"modalSplit_It"+ IT_NUMBER  + ".txt";

			
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
	public static void writeModeDistanceDistribution(){
	RunLegModeDistanceDistribution rlmdd = new RunLegModeDistanceDistribution(OUTPUT_FOLDER, CONFIG_FILE, IT_NUMBER, USER_GROUP);	
	rlmdd.run();
	}
	
	
	
	public static void main (String[]arg){
		writeModalShare();
//		writeModeDistanceDistribution();
//		writeModalTravelTimes();

	}
	

}
