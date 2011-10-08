package playground.ikaddoura.busCorridor.version4;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class TextFileWriter {

	public void writeFile(String outputExternalIterationDirPath, Map<Integer, Integer> iteration2numberOfBuses, Map<Integer, Double> iteration2providerScore, Map<Integer, Double> iteration2userScore, Map<Integer, Integer> iteration2numberOfCarLegs, Map<Integer, Integer> iteration2numberOfPtLegs, Map<Integer, Integer> iteration2numberOfWalkLegs){
		File file = new File(outputExternalIterationDirPath+"/busNumberScoreStats.txt");
		   
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "ITERATION ; NumberOfBuses ; ProviderScore ; UserScore (avg. executed) ; CarLegs ; PtLegs ; WalkLegs";
	    bw.write(zeile1);
	    bw.newLine();
	
	    for (Integer iteration : iteration2numberOfBuses.keySet()){
	    	int numberOfBuses = iteration2numberOfBuses.get(iteration);
	    	double providerScore = iteration2providerScore.get(iteration);
	    	double userScore = iteration2userScore.get(iteration);
	    	int carLegs = iteration2numberOfCarLegs.get(iteration);
	    	int ptLegs = iteration2numberOfPtLegs.get(iteration);
	    	int walkLegs = iteration2numberOfWalkLegs.get(iteration);

	    	
	    	String zeile = iteration+ " ; "+numberOfBuses+" ; "+providerScore+" ; "+userScore+" ; "+carLegs+" ; "+ptLegs+" ; "+walkLegs;
	
	    	bw.write(zeile);
	        bw.newLine();
	    }
	
	    bw.flush();
	    bw.close();
	    System.out.println("File "+file.toString()+" geschrieben");
    
	    } catch (IOException e) {}
	}
}
