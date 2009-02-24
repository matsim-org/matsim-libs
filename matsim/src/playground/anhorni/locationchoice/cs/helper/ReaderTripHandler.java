package playground.anhorni.locationchoice.cs.helper;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.utils.geometry.CoordImpl;

public class ReaderTripHandler {
	
	private final static Logger log = Logger.getLogger(ReaderTripHandler.class);
	private Trip trip;
	private double travelTimeBudget;
	private Id chosenFacilityId;
	

	public void constructTrip(String [] entries, NetworkLayer network, ZHFacilities facilities, 
			MZTrip mzTrip, int tripNr) {
		
		Coord beforeShoppingCoord = new CoordImpl(
				Double.parseDouble(entries[38].trim()), Double.parseDouble(entries[39].trim()));
		Act beforeShoppingAct = new Act("start", beforeShoppingCoord);
		// in seconds after midnight
		double endTimeBeforeShoppingAct = 60.0 * Double.parseDouble(entries[12].trim());
		beforeShoppingAct.setEndTime(endTimeBeforeShoppingAct);
		beforeShoppingAct.setLink(network.getNearestLink(beforeShoppingCoord));
		
		Id chosenFacilityId = new IdImpl(entries[2].trim());
	
		ZHFacility chosenFacility = facilities.getZhFacilities().get(chosenFacilityId);
		Link link = network.getLink(chosenFacility.getLinkId());

		Act shoppingAct = new Act("shop", link);
		shoppingAct.setCoord(link.getCenter());
		
		double startTimeShoppingAct = 60.0 * Double.parseDouble(entries[15].trim());
		shoppingAct.setStartTime(startTimeShoppingAct);
		double endTimeShoppingAct = mzTrip.getStartTime();
		shoppingAct.setEndTime(endTimeShoppingAct);
		
		this.chosenFacilityId = chosenFacilityId;
			
		Coord afterShoppingCoord = mzTrip.getCoord();
		Act afterShoppingAct = new Act("end", afterShoppingCoord);
		afterShoppingAct.setLink(network.getNearestLink(afterShoppingCoord));
		
		if (!(mzTrip.getEndTime() > 0.0)) {
			log.error("No end time found for MZ trip : " + mzTrip.getId());
		}
		double startTimeAfterShoppingAct = mzTrip.getEndTime(); 			
		afterShoppingAct.setStartTime(startTimeAfterShoppingAct);
		
		this.travelTimeBudget = (startTimeAfterShoppingAct- endTimeBeforeShoppingAct) - (endTimeShoppingAct - startTimeShoppingAct);
					
		Trip trip = new Trip(tripNr, beforeShoppingAct, shoppingAct, afterShoppingAct);
		this.trip = trip;
	}
	
	public Trip getTrip() {
		return trip;
	}
	public void setTrip(Trip trip) {
		this.trip = trip;
	}
	public double getTravelTimeBudget() {
		return travelTimeBudget;
	}
	public void setTravelTimeBudget(double travelTimeBudget) {
		this.travelTimeBudget = travelTimeBudget;
	}

	public Id getChosenFacilityId() {
		return chosenFacilityId;
	}	
}
