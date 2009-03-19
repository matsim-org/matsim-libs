package playground.anhorni.locationchoice.facilities;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Facility;

public class ReadKonradFacilities {
	
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
			String lastElement = addressParts[addressParts.length];
			
			if (lastElement.matches("\\d{1-5}"))  {
				HNR = lastElement;
			}
			String street = null;
			
			
		
		
		
			ZHFacilityComposed zhfacility = new ZHFacilityComposed(
				"0", retailerCategory, "no name", street, HNR, PLZ, city, 
				facility.getCoord().getX(), facility.getCoord().getY(), desc);
		}
		
		
		return zhfacilities;
		
	}

}
