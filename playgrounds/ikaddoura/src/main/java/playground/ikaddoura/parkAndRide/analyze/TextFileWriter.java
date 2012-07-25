package playground.ikaddoura.parkAndRide.analyze;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.matsim.api.core.v01.Id;

public class TextFileWriter {

	public void writeFile(List<Id> idList, String outputFile){
		File file = new File(outputFile);
		   
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    bw.newLine();
	
	    for (Id id : idList){
	    	String personId = id.toString();
	    	
	    	String zeile = personId;
	
	    	bw.write(zeile);
	        bw.newLine();
	        }
	
	    bw.flush();
	    bw.close();
	    System.out.println("File "+file.toString()+" geschrieben");
    
	    } catch (IOException e) {}
	}
}
