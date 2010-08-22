package playground.wrashid.parkingSearch.planLevel.strc2010;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.RunLib;
import playground.wrashid.lib.obj.plan.PersonGroups;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.IncomeRelevantForParking;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.ParkingPrice;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.ParkingPrice1;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.ParkingPriceMapping;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.ParkingPriceMapping1;
import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenario;
import playground.wrashid.parkingSearch.planLevel.scoring.ParkingDefaultScoringFunction;

public class Run27  extends IncomeRelevantForParking implements ParkingPriceMapping {

	public static void main(String[] args) {
		int runNumber = RunLib.getRunNumber(new Object() {
		}.getClass().getEnclosingClass());
		Controler controler = RunSeries.getControler(runNumber);

		ParkingRoot.setParkingScoringFunction(new ParkingDefaultScoringFunction(new Run27(), new Run27()));
		
		GlobalRegistry.doPrintGraficDataToConsole=true;
		
		initPersonGroupsForStatistics();
		
		controler.run();
	}

	@Override
	public ParkingPrice getParkingPrice(Id facilityId) {
		if (isPartOfParkingSetCloseToHomeWithin4500Meters(facilityId) || isPartOfParkingSetCloseToWorkWithin4500Meters(facilityId)){
			return getExpensiveParking();
		} else {
			return getCheapParking();
		}
	}
	
	public ParkingPrice getCheapParking(){
		return new ParkingPrice1();
	}
	
	public ParkingPrice getExpensiveParking(){
		return new ParkingPrice() {
			public double getPrice(double startParkingTime, double endParkingTime) {
				return 2*getCheapParking().getPrice(startParkingTime, endParkingTime);
			}
		};
	}
	
	public double getIncome(Id personId){
		int personIdInt=Integer.parseInt(personId.toString());
		if (personIdInt %2==0){
			return 10000;
		} else {
			return 5000;
		}
	}
	
	public boolean isPartOfParkingSetCloseToHomeWithin4500Meters(Id facilityId){
		int facilityIdInt=Integer.parseInt(facilityId.toString());
		if (facilityIdInt>=1 && facilityIdInt<=5){
			return true;
		}
		if (facilityIdInt>=19 && facilityIdInt<=23 ){
			return true;
		}
		return false;
	}
	
	public boolean isPartOfParkingSetCloseToWorkWithin4500Meters(Id facilityId){
		int facilityIdInt=Integer.parseInt(facilityId.toString());
		if (facilityIdInt>=15 && facilityIdInt<=18){
			return true;
		}
		if (facilityIdInt>=33 && facilityIdInt<=36 ){
			return true;
		}
		return false;
	}
	
	private static void initPersonGroupsForStatistics() {
		PersonGroups personGroupsForStatistics = new PersonGroups();

		for (int i = 1; i <= 1000; i++) {
				personGroupsForStatistics.addPersonToGroup(
						"Group-" + i%2, new IdImpl(i));
		}

		ParkingRoot.setPersonGroupsForStatistics(personGroupsForStatistics);
	}
	
	
	
}
