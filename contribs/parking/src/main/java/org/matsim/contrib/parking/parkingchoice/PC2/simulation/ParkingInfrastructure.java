package org.matsim.contrib.parking.parkingchoice.PC2.simulation;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingchoice.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.parkingchoice.PC2.infrastructure.PPRestrictedToFacilities;
import org.matsim.contrib.parking.parkingchoice.PC2.infrastructure.PublicParking;
import org.matsim.contrib.parking.parkingchoice.PC2.infrastructure.RentableParking;
import org.matsim.contrib.parking.parkingchoice.PC2.scoring.ParkingScore;
import org.matsim.core.api.experimental.events.EventsManager;

public interface ParkingInfrastructure {

	void setPublicParkings(LinkedList<PublicParking> publicParkings);

//	void setRentableParking(LinkedList<RentableParking> rentableParkings);

//	void setPrivateParkingRestrictedToFacilities(LinkedList<PPRestrictedToFacilities> ppRestrictedToFacilities);

	void notifyBeforeMobsim();

//	PC2Parking parkAtClosestPublicParkingNonPersonalVehicle(Coord destCoordinate, String groupName);

//	void logArrivalEventAtTimeZero(PC2Parking parking);

//	PC2Parking parkAtClosestPublicParkingNonPersonalVehicle(Coord destCoordinate, String groupName, Id<Person> personId,
//			double parkingDurationInSeconds, double arrivalTime);

	// TODO: make this method abstract
	// when person/vehicleId is clearly distinct, then I can change this to
	// vehicleId - check, if this is the case now.
	PC2Parking parkVehicle(ParkingOperationRequestAttributes parkingOperationRequestAttributes);

	// TODO: make this method abstract
	PC2Parking personCarDepartureEvent(ParkingOperationRequestAttributes parkingOperationRequestAttributes);

	void scoreParkingOperation(ParkingOperationRequestAttributes parkingOperationRequestAttributes, PC2Parking parking);

	void unParkVehicle(PC2Parking parking, double departureTime, Id<Person> personId);

//	ParkingScore getParkingScoreManager();

	EventsManager getEventsManager();

	void setEventsManager(EventsManager eventsManager);

	HashMap<Id<PC2Parking>, PC2Parking> getAllParkings();

	void setAllParkings(HashMap<Id<PC2Parking>, PC2Parking> allParkings);

}
