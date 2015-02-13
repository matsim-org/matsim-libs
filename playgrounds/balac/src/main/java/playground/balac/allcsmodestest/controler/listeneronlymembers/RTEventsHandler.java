package playground.balac.allcsmodestest.controler.listeneronlymembers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import playground.balac.allcsmodestest.events.CarsharingLegFinishedEvent;
import playground.balac.allcsmodestest.events.EndRentalEvent;
import playground.balac.allcsmodestest.events.StartRentalEvent;
import playground.balac.allcsmodestest.events.handler.CarsharingLegFinishedEvenHandler;
import playground.balac.allcsmodestest.events.handler.EndRentalEventHandler;
import playground.balac.allcsmodestest.events.handler.StartRentalEventHandler;

public class RTEventsHandler implements StartRentalEventHandler, EndRentalEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler, CarsharingLegFinishedEvenHandler{

	HashMap<Id<Person>, ArrayList<RentalInfo>> rtRentalsStats = new HashMap<Id<Person>, ArrayList<RentalInfo>>();
	ArrayList<RentalInfo> arr = new ArrayList<RentalInfo>();
	
	Set<Id<Person>> currentRentals = new TreeSet<Id<Person>>();
	
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		rtRentalsStats = new HashMap<Id<Person>, ArrayList<RentalInfo>>();
		arr = new ArrayList<RentalInfo>();
		currentRentals = new TreeSet<Id<Person>>();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		// TODO Auto-generated method stub
		
		if (event.getLegMode().equals("walk_rb")) {
			
			if (currentRentals.contains(event.getPersonId())) {
				
				RentalInfo rentalInfo = this.rtRentalsStats.get(event.getPersonId()).get(this.rtRentalsStats.get(event.getPersonId()).size() - 1);
				
				rentalInfo.egressEndTime = event.getTime();
				
				currentRentals.remove(event.getPersonId());
				this.rtRentalsStats.get(event.getPersonId()).remove(this.rtRentalsStats.get(event.getPersonId()).size() - 1);
				arr.add(rentalInfo);
				
			}
			else {
				
				currentRentals.add(event.getPersonId());
				
				RentalInfo rentalInfo = this.rtRentalsStats.get(event.getPersonId()).get(this.rtRentalsStats.get(event.getPersonId()).size() - 1);

				rentalInfo.accessEndTime = event.getTime();
			}
			
		}
		
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// TODO Auto-generated method stub
		
		if (event.getLegMode().equals("walk_rb")) {
			
			if (currentRentals.contains(event.getPersonId())) {
				
				RentalInfo rentalInfo = this.rtRentalsStats.get(event.getPersonId()).get(this.rtRentalsStats.get(event.getPersonId()).size() - 1);
				
				rentalInfo.egressStartTime = event.getTime();
				
				
			}
			else {
				RentalInfo rentalInfo = new RentalInfo();
				
				rentalInfo.personId = event.getPersonId();
				
				rentalInfo.accessStartTime = event.getTime();
				
				if (this.rtRentalsStats.get(event.getPersonId()) != null) {
					
					this.rtRentalsStats.get(event.getPersonId()).add(rentalInfo);
					
				}
				else {
					
					ArrayList<RentalInfo> temp = new ArrayList<RentalInfo>();
					
					temp.add(rentalInfo);
					
					this.rtRentalsStats.put(event.getPersonId(), temp);

				}
				
			}
			
		}
		
	}


	@Override
	public void handleEvent(EndRentalEvent event) {
		// TODO Auto-generated method stub
		if (event.getvehicleId().toString().startsWith("TW")){
			
			RentalInfo rentalInfo = this.rtRentalsStats.get(event.getPersonId()).get(this.rtRentalsStats.get(event.getPersonId()).size() - 1);
		
			rentalInfo.endTime = event.getTime();
		}
	}

	@Override
	public void handleEvent(StartRentalEvent event) {
		// TODO Auto-generated method stub
		if (event.getvehicleId().toString().startsWith("TW")){
			
			RentalInfo rentalInfo = this.rtRentalsStats.get(event.getPersonId()).get(this.rtRentalsStats.get(event.getPersonId()).size() - 1);
		
			rentalInfo.startTime = event.getTime();
			
			rentalInfo.startLinkId = event.getLinkId();
			
			rentalInfo.vehId = event.getvehicleId();
		}
		
	}
	
	@Override
	public void handleEvent(CarsharingLegFinishedEvent event) {

		if (event.getvehicleId().toString().startsWith("TW") && currentRentals.contains(event.getPersonId())){
		
			RentalInfo rentalInfo = this.rtRentalsStats.get(event.getPersonId()).get(this.rtRentalsStats.get(event.getPersonId()).size() - 1);

			rentalInfo.distance += event.getLeg().getRoute().getDistance();
		}
			
	}
	public ArrayList<RentalInfo> rentals() {
		
		return arr;
	}
	public class RentalInfo {
		private Id<Person> personId = null;
		private double startTime = 0.0;
		private double endTime = 0.0;
		private Id<Link> startLinkId = null;
		private double distance = 0.0;
		private double accessStartTime = 0.0;
		private double accessEndTime = 0.0;
		private double egressStartTime = 0.0;
		private double egressEndTime = 0.0;
		private Id<Vehicle> vehId = null;
		public String toString() {
			
			return personId + " " + Double.toString(startTime) + " " + Double.toString(endTime) + " " +
			startLinkId + " " + Double.toString(distance)+ " " + Double.toString(accessEndTime - accessStartTime)
			+ " " + Double.toString(egressEndTime - egressStartTime) + " " + vehId;
		}
	}

	

	
}
