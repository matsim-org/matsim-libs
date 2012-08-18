package playground.ikaddoura.parkAndRide.analyze.plans;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import playground.ikaddoura.parkAndRide.pR.ParkAndRideFacility;

public class TextFileWriter {

	public void writeFile2(int personsPR, int personsHomeWork, String outputFile) {
		
		File file = new File(outputFile);
		   
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    
	    String zeile0 = "number of selected plans with a home and work activity: " + String.valueOf(personsHomeWork);
    	bw.write(zeile0);
        bw.newLine();
        
        String zeile00 = "number of selected plans with a Park'n'Ride activity: " + String.valueOf(personsPR);
    	bw.write(zeile00);
        bw.newLine();
        
        double prShare = (double) personsPR / (double) personsHomeWork;
        String prShareString = String.valueOf(prShare * 100);
        String zeile11 = "Park'n'Ride share: " + prShareString+"%";
    	bw.write(zeile11);
    	
        bw.newLine();
 
	    bw.flush();
	    bw.close();
	    
	    System.out.println("File "+file.toString()+" written");

	    } catch (IOException e) {}
	}

	public void writeFile3(Map<Id, Integer> prLinkId2prActs, Map<Id, ParkAndRideFacility> id2prFacilities, String outputFile) {
		
		File file = new File(outputFile);
		   
	    try {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    
        String zeile11 = "LinkId ; TransitStop ; Usage (pr Activities: 2 per user)";
    	bw.write(zeile11);
        bw.newLine();
      	
	    for (Id linkId : prLinkId2prActs.keySet()){
	    	
	    	String name = "unknown";
			for (ParkAndRideFacility pr : id2prFacilities.values()){
				if (pr.getPrLink3in().equals(linkId)){
					name = pr.getStopFacilityName();
				}
			}
	    	
	    	String zeile1 = linkId.toString() + " ; " + name + " ; " + prLinkId2prActs.get(linkId);
	
	    	bw.write(zeile1);
	        bw.newLine();
	    }
	    	
	    bw.flush();
	    bw.close();
	    System.out.println("File "+file.toString()+" written");
    
	    } catch (IOException e) {}
	}

	public void writeFile1(int equalPlanPersonIDs, int improvedPlanPersonIDs, int worsePlanPersonIDs, int improvedPRPlanPersonIDs, String outputFile) {
		
		File file = new File(outputFile);
		   
	    try {
	    	
		    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		    
		    String zeile0 = "number of selected plans with an equal score: " + String.valueOf(equalPlanPersonIDs);
	    	bw.write(zeile0);
	        bw.newLine();
	        String zeile1 = "number of selected plans with a higher score: " + String.valueOf(improvedPlanPersonIDs);
	    	bw.write(zeile1);
	        bw.newLine();
	        String zeile2 = "number of selected plans with a lower score: " + String.valueOf(worsePlanPersonIDs);
	    	bw.write(zeile2);
	        bw.newLine();
	        String zeile3 = "number of selected plans with Park'n'Ride and a higher score: " + String.valueOf(improvedPRPlanPersonIDs);
	    	bw.write(zeile3);
	        bw.newLine();
		
		    bw.flush();
		    bw.close();
		    
		    System.out.println("File "+file.toString()+" written");

	    } catch (IOException e) {}
	}
}
