package playground.dhosse.gap.scenario;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.jbischoff.commuterDemand.CommuterDataReader;

/**
 * 
 * This class is based on playground.jbischoff....
 * The read method has been modified in such way that
 * <ul>
 * <li>You can specify whether the file read contains data about commuters or reverse commuters. The home and work locations need to be set accordingly-</li>
 * <li>It is possible to add a spatial filter. This can e.g. be used to create only intenal commuter relations (origin and destination inside the planning area).</li>
 * </ul>
 *
 *@author dhosse
 *
 */
public class CommuterFileReader {
	
	private static final Logger log = Logger.getLogger(CommuterDataReader.class);
	private List<String> filteredMunicipalities;
	private String spatialFilter;
	private List<CommuterDataElement> commuterRelations;
	private int currentAdminLevel;
	private int previousAdminLevel = -1;
	
	private List<String> relationHashes = new ArrayList<>();

	public CommuterFileReader(){
		this.commuterRelations = new ArrayList<CommuterDataElement>();
		this.filteredMunicipalities = new LinkedList<String>();
		this.relationHashes = new ArrayList<>();
		this.spatialFilter = "";
	}
	
	public void addFilterRange(int comm){
		log.info("Adding municipalities starting with " + comm);
		
		for (int i = 0;i<1000;i++){
			Integer community = comm + i; 
			this.filteredMunicipalities.add(community.toString());
		}
			
		
	}
	
	public void addFilter(String comm){
		this.filteredMunicipalities.add(comm);
	}
	
	public void setSpatialFilder(String s){
		this.spatialFilter = s;
	}
		
	public void printMunicipalities(){
		for (CommuterDataElement cde : this.commuterRelations){
			System.out.println(cde);
		}
	}
	
	public void read(String filename, boolean reverse){
	
		log.info("Reading commuter da from " + filename);
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		
		String line = null;

		String homeId = null;
		String homeName = null;
		String workId = null;
		String workName = null;
		
		try {
			
			while((line = br.readLine()) != null){
				
				String[] tokens = line.split(";");
				
				if(!tokens[0].equals("")){
					
					if(reverse){
						
						homeId = tokens[0];
						homeName = tokens[1];
						
					} else{
						
						workId = tokens[0];
						workName = tokens[1];
						
					}
					
					continue;
					
				} else{

					if((tokens[2].equals("") || tokens[3].equals(""))&&!tokens[2].equalsIgnoreCase("ZZ")){

						log.warn(line);
						log.info("Possible data error. Will skip this line...");
						continue;
						
					} else {
						
						if(reverse){
							
							workId = tokens[2];
							workName = tokens[3];
							
						} else{
							
							homeId = tokens[2];
							homeName = tokens[3];
							
						}
						
						try{
							this.currentAdminLevel = tokens[2].length();
							
							if(this.previousAdminLevel > 0){
								
								if(this.previousAdminLevel > this.currentAdminLevel){
	
									if(!tokens[3].contains("Übrige")){
										this.previousAdminLevel = this.currentAdminLevel;
										continue;
									}
									
								}
								
							}
						} catch(NumberFormatException e){
							log.info("Reached the end of this block. Continue...");
							continue;
						}
						
						if((this.filteredMunicipalities.contains(homeId) || this.filteredMunicipalities.contains(workId)) &&
								!this.relationHashes.contains(homeId + "_" + workId) && (homeId.startsWith(spatialFilter)&& workId.startsWith(spatialFilter))){
							
							try{
								
								this.relationHashes.add(homeId + "_" + workId);
								int commuters = Integer.parseInt(tokens[4]);
								double share = Double.parseDouble(tokens[5])/commuters;
								CommuterDataElement current = new CommuterDataElement(homeId, homeName, workId, workName, commuters, share);
						    	this.commuterRelations.add(current);
								
							} catch(NumberFormatException e){
								log.info("Invalid number format. Will skip this line...");
								log.warn(line);
							}
							
						}
						
					}
					
				}
			
				this.previousAdminLevel = this.currentAdminLevel;
				
			}
			
			br.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
		log.info("read " + this.commuterRelations.size() + " commuter relations");
		
	}
	
	public List<CommuterDataElement> getCommuterRelations() {
		return commuterRelations;
	}

}
