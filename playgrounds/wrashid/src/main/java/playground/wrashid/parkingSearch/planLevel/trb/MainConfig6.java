package playground.wrashid.parkingSearch.planLevel.trb;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.trafficmonitoring.PessimisticTravelTimeAggregator;

import playground.wrashid.lib.obj.plan.PersonGroups;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenario;

/**
 * This experiment clearly shows, that if we form three groups, the walking
 * distance and score of the groups are different. - and ordered as we would
 * expect.
 * 
 * - point out, why there is a rise in walking distance in the beginning. -
 * point out, why difference between waling distance of group1-group2 is larger
 * than group2-group3 (because the number of parkings increases with bigger
 * radius, that difference is not that important anymore.
 * 
 * 
 * - change the number of parkings and see, what happens (in general the walking
 * distance should decrease).
 * 
 * - change the access time of the closest facilities to home and work => their
 * attractivness should decrease (resulting in longer walking times).
 * 
 * - increase the price of the closest parking: have different groups with
 * different incomes (they should be picket at random, to solve the bias of
 * having an arrival advantage - e.g. select the agents with even ids). -
 * => the agents with higher income should have shorter walking distances.
 * - 
 * 
 * - compare scenarios with 100000 agents and increase parking capacities =>
 * find out, in how many iterations the system finds a proper solution.
 * 
 * @author wrashid
 * 
 */
public class MainConfig6 {
	public static void main(String[] args) {
		Controler controler;
		String configFilePath = "test/input/playground/wrashid/parkingSearch/planLevel/chessConfig6.xml";
		controler = new Controler(configFilePath);

		new BaseControlerScenario(controler);

		initPersonGroupsForStatistics();

		controler.run();
	}

	private static void initPersonGroupsForStatistics() {
		PersonGroups personGroupsForStatistics = new PersonGroups();

		for (int i = 0; i <= 98; i++) {
			personGroupsForStatistics.addPersonToGroup(
					"Group-" + Integer.toString(i / 33 + 1), new IdImpl(i));
		}

		ParkingRoot.setPersonGroupsForStatistics(personGroupsForStatistics);
	}

}
