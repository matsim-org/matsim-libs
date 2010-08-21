package playground.wrashid.parkingSearch.planLevel.trb;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.trafficmonitoring.PessimisticTravelTimeAggregator;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.obj.plan.PersonGroups;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenario;

/**
 * 
 * - 
 * 
 * - increase the price of the closest parking: have different groups with
 * different incomes (they should be picket at random, to solve the bias of
 * having an arrival advantage - e.g. select the agents with even ids). -
 * => the agents with higher income should have shorter walking distances.
 * - => create a Run27 for that, in which both the income of the agents and the price of some parkings is set
 * => show walking distance of groups (high, low income) and compare also the Besetzung der parkplaetze compared 
 *    to szenario, where the price of all parkings is equal.
 *    
 *    
 * 
 * - compare scenarios with 100000 agents and increase parking capacities =>
 * find out, in how many iterations the system finds a proper solution.
 * 
 * 
 * - reason, why there is so much change in the average travel distance: too much of replanning done
 *  and only parking replanning turned on.
 *  
 * - one can see, that the advantage of arriving early (and therefore an advantage regarding the capacity
 * constraint can be established almost immediatly (within the first 5 iterations).
 * 
 * - we can for clarity show in the beginning with only two groups and later with 3 groups.
 * 
 * - add parking type experiments (during parking selection we need to be able to say, what type of parking we require 
 * for an activity, e.g. electrical parking) => electric vehicles as a group will have longer trips if less parkings
 * of that type available (this should be a good example to show that the concept works).
 * 
 * - why is parking score still so negative (even for the vehicles arriving first????????????????)
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
		GlobalRegistry.doPrintGraficDataToConsole=true;

		controler.run();
	}

	private static void initPersonGroupsForStatistics() {
		PersonGroups personGroupsForStatistics = new PersonGroups();

		for (int i = 0; i <= 99; i++) {
			personGroupsForStatistics.addPersonToGroup(
					"Group-" + Integer.toString(i / 50 + 1), new IdImpl(i));
		}

		ParkingRoot.setPersonGroupsForStatistics(personGroupsForStatistics);
	}

}
