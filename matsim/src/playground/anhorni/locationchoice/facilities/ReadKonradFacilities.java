package playground.anhorni.locationchoice.facilities;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Facility;

public class ReadKonradFacilities {
	
	private final static Logger log = Logger.getLogger(ReadKonradFacilities.class);
	
	public List<ZHFacilityComposed> readFacilities(String file) {
		
		List<ZHFacilityComposed> zhfacilities = new Vector<ZHFacilityComposed>();
		
		Facilities facilities=(Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new FacilitiesReaderMatsimV1(facilities).readFile(file);
		
		Iterator<? extends Facility> facilities_it = facilities.getFacilities().values().iterator();
		while (facilities_it.hasNext()) {
			Facility facility = facilities_it.next();
		
			String [] entries = facility.getId().toString().trim().split("_", -1);
			String retailerCategory = entries[0].trim();
			String desc = entries[1].trim();
			String PLZ = entries[4].trim();
			String city = entries[5].trim();
			String streetAndNumber = entries[6].trim();
					
			String [] addressParts = streetAndNumber.split(" ", -1);
			
			String HNR = "-1";
			String lastElement = addressParts[addressParts.length -1];
			
			log.info(lastElement);
			
			String street = "";
			if (lastElement.matches("\\d{1,7}"))  {
				HNR = lastElement;
				for (int i = 0; i < addressParts.length-1; i++) {
					street +=  addressParts[i].charAt(0) + 
						addressParts[i].substring(1, addressParts[i].length()).toLowerCase() + " ";
				}
			}
			else {
				street = streetAndNumber;
			}
			
			ZHFacilityComposed zhfacility = new ZHFacilityComposed(
				"0", retailerCategory, "no name", street.trim(), HNR, PLZ, city, 
				facility.getCoord().getX(), facility.getCoord().getY(), desc);
			
			zhfacilities.add(zhfacility);
		}
		
		
		return zhfacilities;
		
	}

}
