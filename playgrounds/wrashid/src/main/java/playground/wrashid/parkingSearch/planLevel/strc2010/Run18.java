package playground.wrashid.parkingSearch.planLevel.strc2010;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.facilities.ActivityFacility;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.RunLib;
import playground.wrashid.lib.obj.plan.PersonGroups;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.parkingType.ParkingAttribute;
import playground.wrashid.parkingSearch.planLevel.parkingType.ParkingFacilityAttributPersonPreferences;
import playground.wrashid.parkingSearch.planLevel.parkingType.ParkingFacilityAttributes;

public class Run18 {

	public static void main(String[] args) {
		int runNumber = RunLib.getRunNumber(new Object() {
		}.getClass().getEnclosingClass());
		Controler controler = RunSeries.getControler(runNumber);

		ParkingRoot
				.setParkingFacilityAttributes(getParkingFacilityAttributes());
		ParkingRoot
				.setParkingFacilityAttributPersonPreferences(getParkingFacilityAttributPersonPreferences());

		GlobalRegistry.doPrintGraficDataToConsole = true;

		initPersonGroupsForStatistics();

		controler.run();
	}

	public static ParkingFacilityAttributPersonPreferences getParkingFacilityAttributPersonPreferences() {
		return new ParkingFacilityAttributPersonPreferences() {
			@Override
			public ParkingAttribute getParkingFacilityAttributPreferencesOfPersonForActivity(
					Id<Person> personId, Activity activity) {
				if (belongsToElectricVehilcleOwnerGroup(personId)) {
					return ParkingAttribute.HAS_DEFAULT_ELECTRIC_PLUG;
				} else {
					return ParkingAttribute.DUMMY_DEFAULT_PARKING;
				}
			}
		};
	}

	private static ParkingFacilityAttributes getParkingFacilityAttributes() {
		return new ParkingFacilityAttributes() {
			@Override
			public LinkedList<ParkingAttribute> getParkingFacilityAttributes(
					Id<ActivityFacility> facilityId) {
				LinkedList<ParkingAttribute> result = new LinkedList<ParkingAttribute>();

				int facilityIdInt = new Integer(facilityId.toString());

				if (facilityIdInt % 2 == 0) {
					// 10%: only electric parking
					result.add(ParkingAttribute.HAS_DEFAULT_ELECTRIC_PLUG);
				} else if(facilityIdInt % 10 == 0) {
					// 10%: both electric and normal parking
					result.add(ParkingAttribute.HAS_DEFAULT_ELECTRIC_PLUG);
					result.add(ParkingAttribute.DUMMY_DEFAULT_PARKING);
				}

				return result;
			}
		};
	}

	private static boolean belongsToElectricVehilcleOwnerGroup(Id<Person> personId) {
		int personIdInt = new Integer(personId.toString());
		if (personIdInt % 5 == 0) {
			return true;
		}
		return false;
	}

	private static void initPersonGroupsForStatistics() {
		PersonGroups personGroupsForStatistics = new PersonGroups();

		for (int i = 1; i <= 1000; i++) {
			Id<Person> personId = Id.create(i, Person.class);
			int groupId = belongsToElectricVehilcleOwnerGroup(personId) ? 0 : 1;
			personGroupsForStatistics.addPersonToGroup("Group-" + groupId,
					personId);
		}

		ParkingRoot.setPersonGroupsForStatistics(personGroupsForStatistics);
	}

}
