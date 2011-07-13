package playground.wrashid.parkingChoice.apiDefImpl;

import java.util.Collection;
import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.parkingChoice.ParkingConfigModule;
import playground.wrashid.parkingChoice.ParkingManager;
import playground.wrashid.parkingChoice.api.ParkingSelectionManager;
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.PreferredParking;
import playground.wrashid.parkingChoice.infrastructure.PrivateParking;
import playground.wrashid.parkingChoice.infrastructure.ReservedParking;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;

public class ShortestWalkingDistanceParkingSelectionManager implements ParkingSelectionManager {

	private final ParkingManager parkingManager;

	public ShortestWalkingDistanceParkingSelectionManager(ParkingManager parkingManager){
		this.parkingManager = parkingManager;
		
	}
	
	@Override
	public Parking selectParking(Coord targtLocationCoord, ActInfo targetActInfo, Id personId, Double arrivalTime,
			Double estimatedParkingDuration) {
		// TODO Auto-generated method stub
		return getParkingWithShortestWalkingDistance(targtLocationCoord,targetActInfo,personId);
	}
	

	public Parking getParkingWithShortestWalkingDistance(Coord destCoord, ActInfo targetActInfo, Id personId) {

		Collection<Parking> parkingsInSurroundings = getParkingsInSurroundings(destCoord,
				ParkingConfigModule.getStartParkingSearchDistanceInMeters(), personId, 0, targetActInfo,parkingManager.getParkings());

		
		
		return getParkingWithShortestWalkingDistance(destCoord, parkingsInSurroundings);
	}

	private static Parking getParkingWithShortestWalkingDistance(Coord destCoord, Collection<Parking> parkingsInSurroundings) {
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
			double OPTIONALtimeOfDayInSeconds, ActInfo targetActInfo, QuadTree<Parking> parkings) {
		double maxWalkingDistanceSearchSpaceInMeters = 1000000; // TODO: add this
																// parameter in
																// the
																// configuration
																// file
		
		//+ TO solve problem above the user of this module should provide appropriate parkings
		// Far away with appropriate capacity.
		// In this case this parameter could even be left out (although, 1000km is really a long way to walk...)
		// aber dies factor is wichtig, weil parking far away could still be relevant due to the price
		// so this parameter needs to be chosen in a way keeping this in mind.
		
		Collection<Parking> collection = parkings.get(coord.getX(), coord.getY(), minSearchDistance);

		Collection<Parking> resultCollection = filterReservedAndFullParkings(personId, OPTIONALtimeOfDayInSeconds, targetActInfo,
				collection);

		// widen search space, if no parking found
		while (resultCollection.size() == 0) {
			minSearchDistance *= 2;
			collection = parkings.get(coord.getX(), coord.getY(), minSearchDistance);
			resultCollection = filterReservedAndFullParkings(personId, OPTIONALtimeOfDayInSeconds, targetActInfo, collection);

			if (minSearchDistance > maxWalkingDistanceSearchSpaceInMeters) {
				// TODO: enable this again, when can be set from outside
				//DebugLib.stopSystemAndReportInconsistency("Simulation Stopped, because no parking found (for given 'maxWalkingDistanceSearchSpaceInMeters')!");
			}
		}

		return resultCollection;
	}
	
	private Collection<Parking> filterReservedAndFullParkings(Id personId, double OPTIONALtimeOfDayInSeconds,
			ActInfo targetActInfo, Collection<Parking> collection) {
		Collection<Parking> resultCollection = new LinkedList<Parking>();

		boolean isPersonLookingForCertainTypeOfParking = false;

		if (parkingManager.getPreferredParkingManager() != null) {
			isPersonLookingForCertainTypeOfParking = parkingManager.getPreferredParkingManager().isPersonLookingForCertainTypeOfParking(personId, OPTIONALtimeOfDayInSeconds, targetActInfo);
		}

		for (Parking parking : collection) {

			if (!((ParkingImpl)parking).hasFreeCapacity()) {
				continue;
			}

			if (isPersonLookingForCertainTypeOfParking) {
				if (parking instanceof PreferredParking) {

					PreferredParking preferredParking = (PreferredParking) parking;

					if (parkingManager.getPreferredParkingManager().considerForChoiceSet(preferredParking, personId, OPTIONALtimeOfDayInSeconds,
							targetActInfo)) {
						resultCollection.add(parking);
					}
				}

				continue;
			}

			if (parking instanceof ReservedParking) {
				if (parkingManager.getReservedParkingManager() == null) {
					DebugLib.stopSystemAndReportInconsistency("The reservedParkingManager must be set!");
				}

				ReservedParking reservedParking = (ReservedParking) parking;

				if (parkingManager.getReservedParkingManager().considerForChoiceSet(reservedParking, personId, OPTIONALtimeOfDayInSeconds,
						targetActInfo)) {
					resultCollection.add(parking);
				}
			} else if (parking instanceof PrivateParking) {
				PrivateParking privateParking = (PrivateParking) parking;
				if (privateParking.getActInfo().getFacilityId().equals(targetActInfo.getFacilityId())
						&& privateParking.getActInfo().getActType().equals(targetActInfo.getActType())) {
					resultCollection.add(parking);
				}
			} else {
				resultCollection.add(parking);
			}
		}
		return resultCollection;
	}
	
}
