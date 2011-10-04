package playground.ikaddoura.analysis.beeline;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.SortedMap;

public class TextFileWriter {

	public void writeFile(String inputFile, String outputFile, SortedMap<Double,Line> resultMap){
		File file = new File(outputFile);
		   
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "analyzed PlansFile: "+inputFile;
	    String zeile2 = "Distanzgruppe[m] ; CarLegs ; PtLegs ; BikeLegs ; WalkLegs ; RideLegs ; UndefinedLegs";
	    bw.write(zeile1);
	    bw.newLine();
	    bw.write(zeile2);
	    bw.newLine();
	
	    for (Double distanzGruppe : resultMap.keySet()){
	    	int carLegs = resultMap.get(distanzGruppe).getCarLegs();
	    	int ptLegs = resultMap.get(distanzGruppe).getPtLegs();
	    	int bikeLegs = resultMap.get(distanzGruppe).getBikeLegs();
	    	int walkLegs = resultMap.get(distanzGruppe).getWalkLegs();
	    	int rideLegs = resultMap.get(distanzGruppe).getRideLegs();
	    	int undefinedLegs = resultMap.get(distanzGruppe).getUndefinedLegs();
	    	
	    	String zeile = distanzGruppe+ " ; "+carLegs+" ; "+ptLegs+" ; "+bikeLegs+" ; "+walkLegs+" ; "+rideLegs+" ; "+undefinedLegs;
	
	    	bw.write(zeile);
	        bw.newLine();
	        }
	
	    bw.flush();
	    bw.close();
	    System.out.println("File "+file.toString()+" geschrieben");
    
	    } catch (IOException e) {}
	}
}
