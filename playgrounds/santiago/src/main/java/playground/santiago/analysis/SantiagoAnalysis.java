package playground.santiago.analysis;

import java.io.File;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;

public class SantiagoAnalysis {
	
	private static final String RUN_DIR = "../../../runs-svn/santiago/BASE1/output/";
	private static final String ANALYSIS_DIR = RUN_DIR + "analysis";
	
	private static final String IT = "ITERS/it.100";	
	private static final String EVENTS = "100.events.xml.gz";
	private static final String PLANS = "100.plans.xml.gz";
	
	
	
	private void createDir(File file) {
		file.mkdirs();	
	}
	
	
	public void writeModalShare(){
			
		File analysisDir = new File(ANALYSIS_DIR);
			
		
		String eventsFile = RUN_DIR+IT+EVENTS;
			
		
		if(!analysisDir.exists()) createDir(analysisDir);
			
//			String outputFile = RUN_DIR+"/analysis/modalSplit_"+rc+".txt";
			ModalShareFromEvents msc = new ModalShareFromEvents(eventsFile);
			msc.run();
//			msc.writeResults(outputFile);

	}

}
