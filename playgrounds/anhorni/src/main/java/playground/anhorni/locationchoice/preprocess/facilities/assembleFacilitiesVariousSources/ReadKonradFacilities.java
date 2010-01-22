package playground.anhorni.locationchoice.preprocess.facilities.assembleFacilitiesVariousSources;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTime.DayType;

public class ReadKonradFacilities {
	
	private final static Logger log = Logger.getLogger(ReadKonradFacilities.class);
	
	public List<ZHFacilityComposed> readFacilities(String file) {
		
		List<ZHFacilityComposed> zhfacilities = new Vector<ZHFacilityComposed>();
		
		ScenarioImpl scenario = new ScenarioImpl();
		ActivityFacilitiesImpl facilities=scenario.getActivityFacilities();
		new FacilitiesReaderMatsimV1(scenario).readFile(file);
		
		Iterator<? extends ActivityFacilityImpl> facilities_it = facilities.getFacilities().values().iterator();
		while (facilities_it.hasNext()) {
			ActivityFacilityImpl facility = facilities_it.next();
		
			String [] entries = facility.getId().toString().trim().split("_", -1);
			String retailerCategory = entries[0].trim();
			String desc = entries[1].trim();
			String PLZ = entries[4].trim();
			String city = entries[5].trim();
			String streetAndNumber = entries[6].trim();
					
			String [] addressParts = streetAndNumber.split(" ", -1);
			
			String HNR = "-1";
			String lastElement = addressParts[addressParts.length -1];
			
			String street = "";
			if (lastElement.matches("\\d{1,7}") || lastElement.contains("-") || lastElement.contains("/") ||
					lastElement.endsWith("a") || lastElement.endsWith("A") || lastElement.contains("+") || 
					lastElement.endsWith("c") || lastElement.endsWith("b"))  {
				HNR = lastElement;
				for (int i = 0; i < addressParts.length-1; i++) {
					street +=  addressParts[i].toUpperCase() + " ";
				}
			}
			else {
				street = streetAndNumber.toUpperCase();
				
			}
			
			ZHFacilityComposed zhfacility = new ZHFacilityComposed(
				"0", retailerCategory, "no name", street.trim(), HNR, PLZ, city, 
				facility.getCoord().getX(), facility.getCoord().getY(), "-1", desc);
			
			double opentimes[][] = {{-1,-1,-1,-1}, 
					{-1,-1,-1,-1},
					{-1,-1,-1,-1},
					{-1,-1,-1,-1},
					{-1,-1,-1,-1},
					{-1,-1,-1,-1},
					{-1,-1,-1,-1}};
			
			int i = 0;
			for (DayType day : DayType.values()) {				
				SortedSet<OpeningTime>  set = facility.getActivityOptions().get("shop").getOpeningTimes(day);
				if (set != null) {
					if (set.size() == 2) {
						opentimes[i][2] = set.first().getStartTime();
						opentimes[i][3] = set.first().getEndTime();
						opentimes[i][0] = set.last().getStartTime();
						opentimes[i][1] = set.last().getEndTime();
					}
					else {
						opentimes[i][0] = set.first().getStartTime();
						opentimes[i][3] = set.first().getEndTime();
					}
				}
				i++;
			}
			zhfacility.setOpentimes(opentimes);
			zhfacilities.add(zhfacility);
		}
		return zhfacilities;
	}

}
