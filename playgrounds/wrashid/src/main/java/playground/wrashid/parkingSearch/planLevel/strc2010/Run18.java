package playground.wrashid.parkingSearch.planLevel.strc2010;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.RunLib;
import playground.wrashid.lib.obj.plan.PersonGroups;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.parkingType.ParkingAttribute;
import playground.wrashid.parkingSearch.planLevel.parkingType.ParkingFacilityAttributPersonPreferences;
import playground.wrashid.parkingSearch.planLevel.parkingType.ParkingFacilityAttributes;
import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenario;
import playground.wrashid.parkingSearch.planLevel.scoring.ParkingDefaultScoringFunction;

public class Run18 {

	public static void main(String[] args) {
		int runNumber = RunLib.getRunNumber(new Object() {
		}.getClass().getEnclosingClass());
		Controler controler = RunSeries.getControler(runNumber);

		ParkingRoot.setParkingFacilityAttributes(getParkingFacilityAttributes());
		ParkingRoot.setParkingFacilityAttributPersonPreferences(getParkingFacilityAttributPersonPreferences());
		
		GlobalRegistry.doPrintGraficDataToConsole=true;
		
		initPersonGroupsForStatistics();
		
		controler.run();
	}
	
	public static ParkingFacilityAttributPersonPreferences getParkingFacilityAttributPersonPreferences(){
		return new ParkingFacilityAttributPersonPreferences() {
			public ParkingAttribute getParkingFacilityAttributPreferencesOfPersonForActivity(Id personId, ActivityImpl activity) {
				if (belongsToElectricVehilcleOwnerGroup(personId)){
					return ParkingAttribute.HAS_DEFAULT_ELECTRIC_PLUG;
				} else {
					return ParkingAttribute.DUMMY_DEFAULT_PARKING;
				}
			}
		};
	}
	
	
	private static ParkingFacilityAttributes getParkingFacilityAttributes(){
		return new ParkingFacilityAttributes() {
			public LinkedList<ParkingAttribute> getParkingFacilityAttributes(Id facilityId) {
				LinkedList<ParkingAttribute> result=new LinkedList<ParkingAttribute>();
				
				int facilityIdInt=new Integer(facilityId.toString());
				
				if (facilityIdInt%5==0){
					if (facilityIdInt%10==0){
						// 10%: only electric parking
						result.add(ParkingAttribute.HAS_DEFAULT_ELECTRIC_PLUG);
					} else {
						// 10%: both electric and normal parking
						result.add(ParkingAttribute.HAS_DEFAULT_ELECTRIC_PLUG);
						result.add(ParkingAttribute.DUMMY_DEFAULT_PARKING);
					}
				} else {
					// 90%: only dummy parking
					result.add(ParkingAttribute.DUMMY_DEFAULT_PARKING);
				}
				
				return null;
			}
		};
	}
	
	private static boolean belongsToElectricVehilcleOwnerGroup(Id personId){
		int personIdInt=new Integer(personId.toString());
		if (personIdInt%10==0){
			return true;
		}
		return false;
	}
	
	private static void initPersonGroupsForStatistics() {
		PersonGroups personGroupsForStatistics = new PersonGroups();

		for (int i = 1; i <= 1000; i++) {
			Id personId=new IdImpl(i);
			int groupId=belongsToElectricVehilcleOwnerGroup(personId)?0:1;
				personGroupsForStatistics.addPersonToGroup(
						"Group-" + groupId, personId);
		}

		ParkingRoot.setPersonGroupsForStatistics(personGroupsForStatistics);
	}
	
}
