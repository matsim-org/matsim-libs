package playground.jbischoff.commuterDemand;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;



public class CommuterDataReader {
	private static final Logger log = Logger.getLogger(CommuterDataReader.class);
	private List<String> filteredMunicipalities;
	private List<CommuterDataElement> CommuterRelations;

public CommuterDataReader(){
	this.CommuterRelations = new ArrayList<CommuterDataElement>();
	this.filteredMunicipalities = new LinkedList<String>();
}

public void fillFilter(int comm){
	log.info("Adding municipalities starting with " + comm);
	
	for (int i = 0;i<1000;i++){
		Integer community = comm + i; 
		this.filteredMunicipalities.add(community.toString());
	}
		
	
}

public void addFilter(String comm){
	this.filteredMunicipalities.add(comm);
}
	
public void printMunicipalities(){
	for (CommuterDataElement cde : this.CommuterRelations){
		System.out.println(cde);
	}
}
	
public void readFile(String filename){
	log.info("Reading commuter files from" + filename);		
	FileReader fr;
	try {
		
		
		if (this.filteredMunicipalities.get(0) == null) throw new Exception("No Municipality filter set");
		fr = new FileReader(new File (filename));
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		String currentFrom = null;
		while ((line = br.readLine()) != null) {
			    String[] result = line.split(";");
			    if (!result[0].equals("")) {
			    	currentFrom = result[0];
			    	log.info("Handling From Municipality "+currentFrom);
			    	continue;
			    }
			    else {
			    	String currentTo = result[2];
			    	if (currentTo.equals("")) {
			    		log.error("possible data error, will skip this line: \n "+ line);
			    		continue;
			    	}
			    	int commuters = Integer.parseInt(result[4]);
			    	CommuterDataElement current = new CommuterDataElement(currentFrom,currentTo, commuters);
			    	this.CommuterRelations.add(current);
			    }
	         
	
		
 }
		
		
	
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	
	}
	
}
