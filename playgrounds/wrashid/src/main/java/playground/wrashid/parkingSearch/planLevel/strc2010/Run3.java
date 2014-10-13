package playground.wrashid.parkingSearch.planLevel.strc2010;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;

import playground.wrashid.lib.RunLib;
import playground.wrashid.lib.obj.plan.PersonGroups;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;

/**
 * 
 * @author wrashid
 * 
 */
public class Run3 {
		public static void main(String[] args) {
			int runNumber = RunLib.getRunNumber(new Object() {
			}.getClass().getEnclosingClass());
			Controler controler = RunSeries.getControler(runNumber);

		initPersonGroupsForStatistics();

		controler.run();
	}

	private static void initPersonGroupsForStatistics() {
		PersonGroups personGroupsForStatistics = new PersonGroups();

		for (int i = 0; i <= 99; i++) {
			personGroupsForStatistics.addPersonToGroup(
					"Group-" + Integer.toString(i / 33 + 1), Id.create(i, Person.class));
		}

		ParkingRoot.setPersonGroupsForStatistics(personGroupsForStatistics);
	}
	
	
}
