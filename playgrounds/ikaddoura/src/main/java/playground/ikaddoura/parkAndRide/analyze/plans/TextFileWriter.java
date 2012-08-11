package playground.ikaddoura.parkAndRide.analyze.plans;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class TextFileWriter {

	public void writeFile1(List<Id> idList, String outputFile){
		File file = new File(outputFile);
		   
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile0 = "Anzahl Agenten mit verbessertem Plan ohne Park'n'Ride: " + idList.size();
    	bw.write(zeile0);
        bw.newLine();
	    
	    String zeile = "PersonIDs mit verbessertem Plan ohne Park'n'Ride:";
		
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
	    String zeile0 = "Anzahl ausgewählter Pläne mit Home und Work Activity: " + personsHomeWork.size();
    	bw.write(zeile0);
        bw.newLine();
        String zeile00 = "Anzahl ausgewählter Pläne mit Park'n'Ride Activity: " + personsHomeWork.size();
    	bw.write(zeile00);
        bw.newLine();
        String zeile11 = "Park'n'Ride-Anteil: " + personsPR.size()/personsHomeWork.size()*100+"%";
    	bw.write(zeile11);
        bw.newLine();
      
        bw.newLine();
	    
	    String zeile = "Agenten mit Park'n'Ride im ausgewählten Plan:";
		
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

	public void writeFile3(Map<Id, Integer> prLinkId2prActs, String outputFile) {
		File file = new File(outputFile);
		   
	    try {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    
        String zeile11 = "LinkId ; Usage (pr Activities: 2 per user)";
    	bw.write(zeile11);
        bw.newLine();
      
        bw.newLine();
	
	    for (Id linkId : prLinkId2prActs.keySet()){
	    	
	    	String zeile1 = linkId.toString() + " ; " + prLinkId2prActs.get(linkId);
	
	    	bw.write(zeile1);
	        bw.newLine();
	        }
	
	    bw.flush();
	    bw.close();
	    System.out.println("File "+file.toString()+" geschrieben");
    
	    } catch (IOException e) {}
	}
}
