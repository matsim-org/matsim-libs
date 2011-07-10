package playground.wrashid.parkingChoice;

import java.util.LinkedList;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.wrashid.parkingChoice.api.PreferredParkingManager;
import playground.wrashid.parkingChoice.api.ReservedParkingManager;
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.PreferredParking;
import playground.wrashid.parkingChoice.infrastructure.ReservedParking;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;

public class PreferredParkingTest extends TestCase {

	public void testBaseTestCase() {
		PreferredParkingManager reservedParkingManager = new PreferredParkingManager() {

			@Override
			public boolean considerForChoiceSet(PreferredParking preferredParking, Id personId,
					double OPTIONALtimeOfDayInSeconds, ActInfo targetActInfo) {

				if (preferredParking.getAttributes().contains("EV")) {
					return true;
				}

				return false;
			}

			@Override
			public boolean isPersonLookingForCertainTypeOfParking(Id personId, double oPTIONALtimeOfDayInSeconds,
					ActInfo targetActInfo) {
				if (personId.equals(new IdImpl(1))) {
					return true;
				}
				return false;
			}

			
		};

		assertEquals(9594, walkingDistanceFor3CarScenario(reservedParkingManager, 1), 5.0);
	}

	public void testAllAgentsWantToUseFarAwayPreferredParkingShouldIncreaseAverageWalkingDistance() {
		PreferredParkingManager reservedParkingManager = new PreferredParkingManager() {

			@Override
			public boolean considerForChoiceSet(PreferredParking preferredParking, Id personId,
					double OPTIONALtimeOfDayInSeconds, ActInfo targetActInfo) {
				return true;
			}

			@Override
			public boolean isPersonLookingForCertainTypeOfParking(Id personId, double oPTIONALtimeOfDayInSeconds,
					ActInfo targetActInfo) {
				return true;
			}

			
		};

		assertEquals(24167, walkingDistanceFor3CarScenario(reservedParkingManager, 3), 5.0);
	}
	
	public void testOnlyUsePreferredParkingAtWorkAndNoteHomeShouldDecreaseWalkingDistance() {
		PreferredParkingManager reservedParkingManager = new PreferredParkingManager() {

			@Override
			public boolean considerForChoiceSet(PreferredParking preferredParking, Id personId,
					double OPTIONALtimeOfDayInSeconds, ActInfo targetActInfo) {
				return true;
			}

			@Override
			public boolean isPersonLookingForCertainTypeOfParking(Id personId, double oPTIONALtimeOfDayInSeconds,
					ActInfo targetActInfo) {
				if (targetActInfo.getActType().equalsIgnoreCase("work")){
					return true;
				}
				return false;
			}

			
		};

		assertEquals(22929, walkingDistanceFor3CarScenario(reservedParkingManager, 10), 5.0);
	}

	private double walkingDistanceFor3CarScenario(PreferredParkingManager preferredParkingManager, int parkingCapacity) {
		Config config = ConfigUtils.loadConfig("test/input/playground/wrashid/parkingChoice/chessConfig.xml");
		Controler controler = new Controler(config);

		// setup parking infrastructure
		LinkedList<Parking> parkingCollection = new LinkedList<Parking>();

		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				ParkingImpl parking = new ParkingImpl(new CoordImpl(i * 1000 + 500, j * 1000 + 500));
				parking.setMaxCapacity(parkingCapacity);
				parkingCollection.add(parking);
			}
		}

		PreferredParking preferredParking = new PreferredParking(new CoordImpl(1000.0, 1000), "EV, Mobility");
		preferredParking.setMaxCapacity(parkingCapacity);
		parkingCollection.add(preferredParking);

		ParkingModule parkingModule = new ParkingModule(controler, parkingCollection);

		parkingModule.setPreferredParkingManager(preferredParkingManager);

		controler.setOverwriteFiles(true);

		controler.run();

		return parkingModule.getAverageWalkingDistance();
	}
}
