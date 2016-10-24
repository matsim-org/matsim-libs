package playground.wrashid.parkingSearch.planLevel.scenario;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.facilities.MatsimFacilitiesReader;

public class BaseNonControlerScenario {

	/**
	 * If you just want to work on population, network or facilities without the controler for tests, use this scenario.
	 * @param sc
	 * @return
	 */
	public static Network loadNetwork(MutableScenario sc) {
		String facilitiesPath = "test/input/playground/wrashid/parkingSearch/planLevel/chessFacilities.xml";
		String networkFile = "test/input/playground/wrashid/parkingSearch/planLevel/network.xml";
		String inputPlansFile = "test/input/playground/wrashid/parkingSearch/planLevel/chessPlans2.xml";
	
		new MatsimFacilitiesReader(sc).readFile(facilitiesPath);
	
		MatsimReader popReader = new PopulationReader(sc);
		popReader.readFile(inputPlansFile);
	
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);
	
		return sc.getNetwork();
	}

}
