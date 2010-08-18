package playground.wrashid.PSF.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.matsim.api.core.v01.network.Link;

import playground.wrashid.PHEV.parking.data.Facility;
import playground.wrashid.PHEV.parking.data.StreetParkingData;
import playground.wrashid.PSF.ParametersPSF;

/**
 * File format (for example see test case data).
 * 
 * Numbering of Hubs starts with zero. 
 * 
 * the links belonging to the first hub (number zero) are in the first column.
 * 
 * At the end of the file, if some hubs have more links than others, a minus -1 is used to fill the columns. * 
 * 
 * Note: This only accepts link ids which are integer => change that later (restriction only, because format given)
 * 
 * 
 * @author rashid_waraich
 *
 */
public class HubLinkMapping {
	
	// key: linkId, value: hub number
	HashMap<String,Integer> linkHubMapping=new HashMap<String,Integer>();
	private int numberOfHubs;

	private void handleUnmappedLinksStart(){
		if (ParametersPSF.getMainInitUnmappedLinks()!=null && ParametersPSF.getMainInitUnmappedLinks()){
			this.numberOfHubs--;
		}
	}
	
	private void handleUnmappedLinksEnd(){
		if (ParametersPSF.getMainInitUnmappedLinks()!=null && ParametersPSF.getMainInitUnmappedLinks()){
			// add unmapped links in "last column"
			for (Link link:ParametersPSF.getMatsimControler().getNetwork().getLinks().values()){
				String linkStringId=link.getId().toString();
				if (!linkHubMapping.containsKey(linkStringId)){
					linkHubMapping.put(linkStringId, this.numberOfHubs);
				}
			}
			this.numberOfHubs++;
		}
	}
	

	/**
	 * reads the mappings from the file. The file has columns (first column for first hub and all the links corresponding to that hub below it) 
	 * @param fileName
	 */
	public HubLinkMapping(String fileName, int numberOfHubs){
		this.numberOfHubs = numberOfHubs;
		
		handleUnmappedLinksStart();
		
		try {
		
		FileReader fr = new FileReader(fileName);
		
		BufferedReader br = new BufferedReader(fr);
		String line;
		StringTokenizer tokenizer;
		String token;
		int linkId;
		line = br.readLine();
		while (line != null) {
			tokenizer = new StringTokenizer(line);
			
			for (int i=0;i<this.numberOfHubs;i++){
				token = tokenizer.nextToken();
				linkId= (int) Double.parseDouble(token);
				linkHubMapping.put(Integer.toString(linkId), i);
			}
			
			if (tokenizer.hasMoreTokens()){
				// if there are more columns than expected, throw an exception
				
				throw new RuntimeException("the number of hubs is wrong");
			}
			
			line = br.readLine();
		}
		
		} catch (RuntimeException e){
			// just forward the runtime exception
			throw e;
		} catch (Exception e){
			throw new RuntimeException("Error reading the hub link mapping file");
		}
		
		// remove link id with number -1
		linkHubMapping.remove("-1");
		
		handleUnmappedLinksEnd();
	}


	public int getNumberOfHubs() {
		return numberOfHubs;
	}
	
	/*
	 * The number of links, which have been read.
	 */
	public int getNumberOfLinks() {
		return linkHubMapping.size();
	}


	public int getHubNumber(String linkId) {
		// just for debugging
		if (linkHubMapping.get(linkId)==null){
			System.out.println(linkId);
		}
		
		return linkHubMapping.get(linkId).intValue();
	}
	
	public static void main(String[] args) {
		new HubLinkMapping("A:/data/matsim/input/runRW1003/hubLinkMapping.txt",819);
	}
	
}     
 