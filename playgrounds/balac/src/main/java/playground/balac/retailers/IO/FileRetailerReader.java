package playground.balac.retailers.IO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.facilities.ActivityFacilityImpl;

import playground.balac.retailers.data.Retailer;
import playground.balac.retailers.data.Retailers;



public class FileRetailerReader {

	private final static Logger log = Logger.getLogger(FileRetailerReader.class);
	private Map<Id<ActivityFacility>, ? extends ActivityFacility> controlerFacilities;
	private String facilityIdFile;
	private Retailers retailers = new Retailers();
	private ArrayList<Id<Link>> retailersLinks = new ArrayList<>();
	private ArrayList<Id<ActivityFacility>> retailersFacilities = new ArrayList<>();
	
	public FileRetailerReader(Map<Id<ActivityFacility>, ? extends ActivityFacility> controlerFacilities, String facilityIdFile) {
		this.controlerFacilities = controlerFacilities;
		this.facilityIdFile = facilityIdFile;
	}
	
	public FileRetailerReader(String facilityIdFile) {
		this.facilityIdFile = facilityIdFile;
	}
	
	public Retailers readRetailers(Controler controler) {
		try { 
			FileReader fr = new FileReader(this.facilityIdFile);
			BufferedReader br = new BufferedReader(fr);
			
			// Skip header
			String curr_line = br.readLine();
			int notFoundFacilities = 0;
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// header: r_id  f_id  strategy linkId capacity
				// index:     0     1      2	   3	  4
				Id<Retailer> rId = Id.create(entries[0], Retailer.class);
				Id<ActivityFacility> fId = Id.create(entries[1], ActivityFacility.class);
								
					if (controlerFacilities.get(fId) != null) {
					if (this.retailers.getRetailers().containsKey(rId)) { // retailer exists already
						
						ActivityFacilityImpl f = (ActivityFacilityImpl)controlerFacilities.get(fId);
						this.retailers.getRetailers().get(rId).addFacility(f);
						retailersLinks.add(f.getLinkId());

					}	
					else { // retailer does not exists yet
						
						Retailer r = new Retailer(rId, null);
						r.addStrategy(controler, entries[2]);
						ActivityFacilityImpl f = (ActivityFacilityImpl)controlerFacilities.get(fId);
						r.addFacility(f);
						retailersLinks.add(f.getLinkId());
						this.retailers.addRetailer(r);
					}
				}
				else {
					notFoundFacilities = notFoundFacilities+1;
					log.warn("The facility " + fId + " has not been found" );
				}
			}
			br.close();
			log.warn(notFoundFacilities + " facilities have not been found");
		} 
		catch (IOException e) {
		}
		return this.retailers;
	}
	
	public ArrayList<Id<ActivityFacility>> readRetailersFacilities() {
		try { 
			FileReader fr = new FileReader(this.facilityIdFile);
			BufferedReader br = new BufferedReader(fr);
			
			// Skip header
			String curr_line = br.readLine();
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// header: r_id  f_id  strategy linkId capacity
				// index:     0     1      2	   3	  4
				Id<ActivityFacility> fId = Id.create (entries[1], ActivityFacility.class);
				this.retailersFacilities.add(fId);		
						
			}
			br.close();
		} 
		catch (IOException e) {
		}
		return this.retailersFacilities;
	}
}

	

