package playground.ikaddoura.busCorridor.version4;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

public class TextFileWriter {
	private final static Logger log = Logger.getLogger(TextFileWriter.class);

	public void writeFile(String outputExternalIterationDirPath, Map<Integer, Integer> iteration2numberOfBuses, Map<Integer, Double> iteration2operatorScore, Map<Integer, Double> iteration2userScore, Map<Integer, Integer> iteration2numberOfCarLegs, Map<Integer, Integer> iteration2numberOfPtLegs, Map<Integer, Integer> iteration2numberOfWalkLegs){
		File file = new File(outputExternalIterationDirPath+"/busNumberScoreStats.txt");
		   
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "ITERATION ; NumberOfBuses ; OperatorScore ; UserScore (avg. executed) ; CarLegs ; PtLegs ; WalkLegs";
	    bw.write(zeile1);
	    bw.newLine();
	
	    for (Integer iteration : iteration2numberOfBuses.keySet()){
	    	int numberOfBuses = iteration2numberOfBuses.get(iteration);
	    	double operatorScore = iteration2operatorScore.get(iteration);
	    	double userScore = iteration2userScore.get(iteration);
	    	int carLegs = iteration2numberOfCarLegs.get(iteration);
	    	int ptLegs = iteration2numberOfPtLegs.get(iteration);
	    	int walkLegs = iteration2numberOfWalkLegs.get(iteration);

	    	
	    	String zeile = iteration+ " ; "+numberOfBuses+" ; "+operatorScore+" ; "+userScore+" ; "+carLegs+" ; "+ptLegs+" ; "+walkLegs;
	
	    	bw.write(zeile);
	        bw.newLine();
	    }
	
	    bw.flush();
	    bw.close();
	    log.info("Analysis Textfile written to "+file.toString());
    
	    } catch (IOException e) {}
	}
}
