package playground.ikaddoura.parkAndRide.analyze.plans;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.ikaddoura.parkAndRide.pR.ParkAndRideFacility;

public class TextFileWriter {

	public void writeFile1(List<Id> idList, int numberOfAgents, String outputFile){
		File file = new File(outputFile);
		   
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile00 = "number of agents: " + numberOfAgents;
    	bw.write(zeile00);
        bw.newLine();
	    
	    String zeile = "PersonIDs:";
		
    	bw.write(zeile);
        bw.newLine();
	
	    for (Id id : idList){
	    	String personId = id.toString();
	    	
	    	String zeile1 = personId;
	
	    	bw.write(zeile1);
	        bw.newLine();
	        }
	
	    bw.flush();
	    bw.close();
	    System.out.println("File "+file.toString()+" geschrieben");
    
	    } catch (IOException e) {}
	}

	public void writeFile2(List<Person> personsPR, List<Person> personsHomeWork, String outputFile) {
		File file = new File(outputFile);
		   
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile0 = "number of selected plans with a home and work activity: " + personsHomeWork.size();
    	bw.write(zeile0);
        bw.newLine();
        String zeile00 = "number of selected plans with a Park'n'Ride activity: " + personsHomeWork.size();
    	bw.write(zeile00);
        bw.newLine();
        String zeile11 = "Park'n'Ride share: " + (double) personsPR.size() / (double) personsHomeWork.size()*100+"%";
    	bw.write(zeile11);
        bw.newLine();
      
        bw.newLine();
	    
	    String zeile = "Agents with selected Park'n'Ride plan:";
		
    	bw.write(zeile);
        bw.newLine();
	
	    for (Person person : personsPR){
	    	String personId = person.getId().toString();
	    	
	    	String zeile1 = personId;
	
	    	bw.write(zeile1);
	        bw.newLine();
	        }
	
	    bw.flush();
	    bw.close();
	    System.out.println("File "+file.toString()+" geschrieben");
    
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
	    System.out.println("File "+file.toString()+" geschrieben");
    
	    } catch (IOException e) {}
	}
}
