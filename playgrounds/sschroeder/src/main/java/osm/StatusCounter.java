package osm;

import org.apache.log4j.Logger;

public class StatusCounter {
	
	private static Logger logger = Logger.getLogger(StatusCounter.class);
	
	private Integer lastPrint;
	
	private int printCounter;
	
	public void printStatus(){
		if(lastPrint == null){
			lastPrint = 1;
		}
		printCounter++;
		if(lastPrint == printCounter){
			logger.info(printCounter + " items");
			lastPrint=lastPrint*2;
		}
	}
}
