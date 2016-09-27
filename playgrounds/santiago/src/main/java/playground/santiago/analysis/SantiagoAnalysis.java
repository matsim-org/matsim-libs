package playground.santiago.analysis;

import java.io.File;

import org.matsim.api.core.v01.Scenario;

import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.modalShare.ModalShareFromPlans;
import playground.agarwalamit.analysis.travelTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.scenarios.munich.analysis.modular.legModeDistanceDistribution.RunLegModeDistanceDistribution;

public class SantiagoAnalysis {
	
	private static final String RUN_DIR = "../../../runs-svn/santiago/baseCase10pct/";
	private static final String OUTPUT_FOLDER = RUN_DIR + "output/";	
	private static final String ANALYSIS_DIR = OUTPUT_FOLDER + "analysis/";
	
	

//	private static final String OUTPUT_TEMP = "../../../runs-svn/santiago/baseCase10pct/OLD/outputSim1/";
//	private static final String ANALYSIS_DIR_TEMP = OUTPUT_TEMP + "analysis/";
	
	private static final String IT_NUMBER = "100";
	private static final String IT_FOLDER = OUTPUT_FOLDER + "ITERS/it." + IT_NUMBER + "/";	
	
//	private static final String IT_FOLDER_TEMP = OUTPUT_TEMP + "ITERS/it." + IT_NUMBER + "/";
	
	
	
	private static final String EVENTS = IT_FOLDER + IT_NUMBER +".events.xml.gz";
	
//	private static final String EVENTS_TEMP = IT_FOLDER_TEMP + IT_NUMBER +".events.xml.gz";
	
//	private static final String PLANS = IT_FOLDER + IT_NUMBER +".plans.xml.gz";
//	private static final String CONFIG_FILE = RUN_DIR + "config_baseCase1pct.xml";
//	private static final UserGroup USER_GROUP = null;
	
	
	private static void createDir(File file) {
		file.mkdirs();	
	}
	
	
	public static void writeModalShare(){
			
		File analysisDir = new File(ANALYSIS_DIR);
		
//		File analysisDir = new File(ANALYSIS_DIR_TEMP);
		
		
		String eventsFile = EVENTS;	
//		String eventsFile = EVENTS_TEMP;
		
		
//		String plansFile = PLANS;
		if(!analysisDir.exists()) createDir(analysisDir);			
			String outputFile = ANALYSIS_DIR +"modalSplit_It"+ IT_NUMBER  + ".txt";
			
//			String outputFile = ANALYSIS_DIR_TEMP +"modalSplit_It"+ IT_NUMBER  + ".txt";
			
			ModalShareFromEvents msc = new ModalShareFromEvents(eventsFile);
			msc.run();
			msc.writeResults(outputFile);
			
//			Scenario sc = LoadMyScenarios.loadScenarioFromPlans(plansFile);
//			ModalShareFromPlans msg = new ModalShareFromPlans(sc.getPopulation());
//			msg.run();
//			msg.writeResults(outputFile);
			
			

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
//	
	
	
	public static void main (String[]arg){
		writeModalShare();
//		writeModalTravelTimes();
//		writeModeDistanceDistribution();
	}
	

}
