package playground.wrashid.parkingChoice;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.collections.QuadTree;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.Parking;
import playground.wrashid.parkingChoice.infrastructure.PreferredParking;
import playground.wrashid.parkingChoice.infrastructure.PreferredParkingManager;
import playground.wrashid.parkingChoice.infrastructure.PrivateParking;
import playground.wrashid.parkingChoice.infrastructure.ReservedParking;
import playground.wrashid.parkingChoice.infrastructure.ReservedParkingManager;

public class ParkingManager implements StartupListener {

	QuadTree<Parking> parkings;
	private ReservedParkingManager reservedParkingManager = null;
	private PreferredParkingManager preferredParkingManager = null;
	private final Controler controler;

	public Controler getControler() {
		return controler;
	}

	private Collection<Parking> parkingCollection;
	// key: personId
	private HashMap<Id, Parking> currentParkingLocation;

	public void resetAllParkingOccupancies() {
		for (Parking parking : parkings.values()) {
			parking.resetParkingOccupancy();
		}
	}

	public Parking getCurrentParkingLocation(Id personId) {
		return currentParkingLocation.get(personId);
	}

	public void setReservedParkingManager(ReservedParkingManager reservedParkingManager) {
		this.reservedParkingManager = reservedParkingManager;
	}

	public void addParkings(Collection<Parking> parkingCollection) {
		for (Parking parking : parkingCollection) {
			addParking(parking);
		}
	}

	private void addParking(Parking parking) {
		parkings.put(parking.getCoord().getX(), parking.getCoord().getY(), parking);
	}

	public Parking getParkingWithShortestWalkingDistance(Coord destCoord, ActInfo targetActInfo, Id personId) {
		double minDistanceOfSearchSpaceInMeters = 1000; // TODO: needs also be
														// set from
														// configuration file
														// (we can't narrow too
														// much our search).

		Collection<Parking> parkingsInSurroundings = getParkingsInSurroundings(destCoord,
				ParkingConfigModule.getStartParkingSearchDistanceInMeters(), personId, 0, targetActInfo);

		return getParkingWithShortestWalkingDistance(destCoord, parkingsInSurroundings);
	}

	private Parking getParkingWithShortestWalkingDistance(Coord destCoord, Collection<Parking> parkingsInSurroundings) {
		Parking bestParking = null;
		double currentBestDistance = Double.MAX_VALUE;

		for (Parking parking : parkingsInSurroundings) {
			double distance = GeneralLib.getDistance(destCoord, parking.getCoord());
			if (distance < currentBestDistance) {
				bestParking = parking;
				currentBestDistance = distance;
			}
		}

		return bestParking;
	}

	public Collection<Parking> getParkingsInSurroundings(Coord coord, double minSearchDistance, Id personId,
			double OPTIONALtimeOfDayInSeconds, ActInfo targetActInfo) {
		double maxWalkingDistanceSearchSpaceInMeters = 1000000; // TODO: add this
																// parameter in
																// the
																// configuration
																// file
		
		//+ TO solve problem above the user of this module should provide appropriate parkings
		// Far away with appropriate capacity.
		// In this case this parameter could even be left out (although, 1000km is really a long way to walk...)

		Collection<Parking> collection = parkings.get(coord.getX(), coord.getY(), minSearchDistance);

		Collection<Parking> resultCollection = filterReservedAndFullParkings(personId, OPTIONALtimeOfDayInSeconds, targetActInfo,
				collection);

		// widen search space, if no parking found
		while (resultCollection.size() == 0) {
			minSearchDistance *= 2;
			collection = parkings.get(coord.getX(), coord.getY(), minSearchDistance);
			resultCollection = filterReservedAndFullParkings(personId, OPTIONALtimeOfDayInSeconds, targetActInfo, collection);

			if (minSearchDistance > maxWalkingDistanceSearchSpaceInMeters) {
				DebugLib.stopSystemAndReportInconsistency("Simulation Stopped, because no parking found (for given 'maxWalkingDistanceSearchSpaceInMeters')!");
			}
		}

		return resultCollection;
	}

	private Collection<Parking> filterReservedAndFullParkings(Id personId, double OPTIONALtimeOfDayInSeconds,
			ActInfo targetActInfo, Collection<Parking> collection) {
		Collection<Parking> resultCollection = new LinkedList<Parking>();

		boolean isPersonLookingForCertainTypeOfParking = false;

		if (preferredParkingManager != null) {
			isPersonLookingForCertainTypeOfParking = preferredParkingManager.isPersonLookingForCertainTypeOfParking(personId, OPTIONALtimeOfDayInSeconds, targetActInfo);
		}

		for (Parking parking : collection) {

			if (!parking.hasFreeCapacity()) {
				continue;
			}

			if (isPersonLookingForCertainTypeOfParking) {
				if (parking instanceof PreferredParking) {

					PreferredParking preferredParking = (PreferredParking) parking;

					if (preferredParkingManager.considerForChoiceSet(preferredParking, personId, OPTIONALtimeOfDayInSeconds,
							targetActInfo)) {
						resultCollection.add(parking);
					}
				}

				continue;
			}

			if (parking instanceof ReservedParking) {
				if (reservedParkingManager == null) {
					DebugLib.stopSystemAndReportInconsistency("The reservedParkingManager must be set!");
				}

				ReservedParking reservedParking = (ReservedParking) parking;

				if (reservedParkingManager.considerForChoiceSet(reservedParking, personId, OPTIONALtimeOfDayInSeconds,
						targetActInfo)) {
					resultCollection.add(parking);
				}
			} else if (parking instanceof PrivateParking) {
				PrivateParking privateParking = (PrivateParking) parking;
				if (privateParking.getCorrespondingActInfo().getFacilityId().equals(targetActInfo.getFacilityId())
						&& privateParking.getCorrespondingActInfo().getActType().equals(targetActInfo.getActType())) {
					resultCollection.add(parking);
				}
			} else {
				resultCollection.add(parking);
			}
		}
		return resultCollection;
	}

	public ParkingManager(Controler controler, Collection<Parking> parkingCollection) {
		this.controler = controler;
		this.parkingCollection = parkingCollection;
		currentParkingLocation = new HashMap<Id, Parking>();
	}

	private void initializeQuadTree(NetworkImpl network) {
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;

		for (Link link : network.getLinks().values()) {
			if (link.getCoord().getX() < minX) {
				minX = link.getCoord().getX();
			}

			if (link.getCoord().getY() < minY) {
				minY = link.getCoord().getY();
			}

			if (link.getCoord().getX() > maxX) {
				maxX = link.getCoord().getX();
			}

			if (link.getCoord().getY() > maxY) {
				maxY = link.getCoord().getY();
			}
		}

		parkings = new QuadTree<Parking>(minX, minY, maxX + 1.0, maxY + 1.0);
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		initializeQuadTree(controler.getNetwork());
		addParkings(parkingCollection);
		parkingCollection = null;

		// initialize parking occupations
		for (Person person : controler.getPopulation().getPersons().values()) {
			Plan selectedPlan = person.getSelectedPlan();

			ActInfo lastActivityInfo = ParkingChoiceLib.getLastActivityInfo(selectedPlan);

			ActivityFacility activityFacility = controler.getFacilities().getFacilities().get(lastActivityInfo.getFacilityId());

			Coord activityCoord = activityFacility.getCoord();

			// park car
			Collection<Parking> parkingsInSurroundings = getParkingsInSurroundings(activityCoord,
					ParkingConfigModule.getStartParkingSearchDistanceInMeters(), person.getId(), 0, lastActivityInfo);

			// score parkings (only according to distance)
			for (Parking parking : parkingsInSurroundings) {
				parking.setScore(-parking.getWalkingDistance(activityCoord) / 10);
			}

			// rank parkings
			PriorityQueue<Parking> rankedParkings = new PriorityQueue<Parking>();
			for (Parking parking : parkingsInSurroundings) {
				rankedParkings.add(parking);
			}

			// park vehicle
			Parking bestParking = rankedParkings.poll();
			parkVehicle(person.getId(), bestParking);
		}
	}

	public void parkVehicle(Id personId, Parking parking) {
		parking.parkVehicle();
		currentParkingLocation.put(personId, parking);
	}

	public void unParkVehicle(Id personId, Parking parking) {
		parking.removeVehicle();
		currentParkingLocation.put(personId, null);
	}

	public void setPreferredParkingManager(PreferredParkingManager preferredParkingManager) {
		this.preferredParkingManager = preferredParkingManager;
	}

}
