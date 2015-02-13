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

public class FFEventsHandler implements StartRentalEventHandler, EndRentalEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler, CarsharingLegFinishedEvenHandler{

	HashMap<Id<Person>, ArrayList<RentalInfoFF>> rtRentalsStats = new HashMap<Id<Person>, ArrayList<RentalInfoFF>>();
	ArrayList<RentalInfoFF> arr = new ArrayList<RentalInfoFF>();
	
	Set<Id<Person>> currentRentals = new TreeSet<Id<Person>>();
	
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		rtRentalsStats = new HashMap<Id<Person>, ArrayList<RentalInfoFF>>();
		arr = new ArrayList<RentalInfoFF>();
		currentRentals = new TreeSet<Id<Person>>();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		// TODO Auto-generated method stub
		
		if (event.getLegMode().equals("walk_ff")) {
			
			if (currentRentals.contains(event.getPersonId())) {
				
				RentalInfoFF rentalInfo = this.rtRentalsStats.get(event.getPersonId()).get(this.rtRentalsStats.get(event.getPersonId()).size() - 1);
				
				rentalInfo.egressEndTime = event.getTime();
				
				currentRentals.remove(event.getPersonId());
				this.rtRentalsStats.get(event.getPersonId()).remove(this.rtRentalsStats.get(event.getPersonId()).size() - 1);
				arr.add(rentalInfo);
				
			}
			else {
				
				currentRentals.add(event.getPersonId());
				
				RentalInfoFF rentalInfo = this.rtRentalsStats.get(event.getPersonId()).get(this.rtRentalsStats.get(event.getPersonId()).size() - 1);

				rentalInfo.accessEndTime = event.getTime();
			}
			
		}
		
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// TODO Auto-generated method stub
		
		if (event.getLegMode().equals("walk_ff")) {
			
			if (currentRentals.contains(event.getPersonId())) {
				
				RentalInfoFF rentalInfo = this.rtRentalsStats.get(event.getPersonId()).get(this.rtRentalsStats.get(event.getPersonId()).size() - 1);
				
				rentalInfo.egressStartTime = event.getTime();
				
				
			}
			else {
				RentalInfoFF rentalInfo = new RentalInfoFF();
				
				rentalInfo.personId = event.getPersonId();
				
				rentalInfo.accessStartTime = event.getTime();
				
				if (this.rtRentalsStats.get(event.getPersonId()) != null) {
					
					this.rtRentalsStats.get(event.getPersonId()).add(rentalInfo);
					
				}
				else {
					
					ArrayList<RentalInfoFF> temp = new ArrayList<RentalInfoFF>();
					
					temp.add(rentalInfo);
					
					this.rtRentalsStats.put(event.getPersonId(), temp);

				}
				
			}
			
		}
		
	}


	@Override
	public void handleEvent(EndRentalEvent event) {
		// TODO Auto-generated method stub
		if (event.getvehicleId().toString().startsWith("FF")){
			
			RentalInfoFF rentalInfo = this.rtRentalsStats.get(event.getPersonId()).get(this.rtRentalsStats.get(event.getPersonId()).size() - 1);
		
			rentalInfo.endTime = event.getTime();
			rentalInfo.endLinkId = event.getLinkId();
		}
	}

	@Override
	public void handleEvent(StartRentalEvent event) {
		// TODO Auto-generated method stub
		if (event.getvehicleId().toString().startsWith("FF")){
			
			RentalInfoFF rentalInfo = this.rtRentalsStats.get(event.getPersonId()).get(this.rtRentalsStats.get(event.getPersonId()).size() - 1);
		
			rentalInfo.startTime = event.getTime();
			
			rentalInfo.startLinkId = event.getLinkId();
			
			rentalInfo.vehId = event.getvehicleId();
		}
		
	}
	
	@Override
	public void handleEvent(CarsharingLegFinishedEvent event) {

		if (event.getvehicleId().toString().startsWith("FF") && currentRentals.contains(event.getPersonId())){
		
			RentalInfoFF rentalInfo = this.rtRentalsStats.get(event.getPersonId()).get(this.rtRentalsStats.get(event.getPersonId()).size() - 1);

			rentalInfo.distance += event.getLeg().getRoute().getDistance();
		}
			
	}
	public ArrayList<RentalInfoFF> rentals() {
		
		return arr;
	}

	public class RentalInfoFF {
		private Id<Person> personId = null;
		private double startTime = 0.0;
		private double endTime = 0.0;
		private Id<Link> startLinkId = null;
		private Id<Link> endLinkId = null;
		private double distance = 0.0;
		private double accessStartTime = 0.0;
		private double accessEndTime = 0.0;
		private double egressStartTime = 0.0;
		private double egressEndTime = 0.0;
		private Id<Vehicle> vehId = null;
		public String toString() {
			
			return personId + " " + Double.toString(startTime) + " " + Double.toString(endTime) + " " +
			startLinkId.toString() + " " +	endLinkId.toString()+ " " + Double.toString(distance)
			+ " " +	Double.toString(accessEndTime - accessStartTime)+ 
			" " + Double.toString(egressEndTime - egressStartTime) +
			" " + vehId;
		}
	}
	
	
}
