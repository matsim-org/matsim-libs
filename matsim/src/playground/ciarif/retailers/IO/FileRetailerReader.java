package playground.ciarif.retailers.IO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;

import playground.ciarif.retailers.data.Retailer;
import playground.ciarif.retailers.data.Retailers;

public class FileRetailerReader {

	private final static Logger log = Logger.getLogger(FileRetailerReader.class);
	private Map<Id, ? extends ActivityFacility> controlerFacilities;
	private String facilityIdFile;
	private Retailers retailers = new Retailers();
	private ArrayList<Id> retailersLinks = new ArrayList<Id>();
	
	public FileRetailerReader(Map<Id, ? extends ActivityFacility> controlerFacilities, String facilityIdFile) {
		this.controlerFacilities = controlerFacilities;
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
				Id rId = new IdImpl(entries[0]);
				Id fId = new IdImpl (entries[1]);
								
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
			log.warn(notFoundFacilities + " facilities have not been found");
		} 
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
		return this.retailers;
	}
}

	

