package playground.wrashid.lib.tools.plan;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.PopulationWriter;


public class PlanShrinker {

	public static void main(final String[] args) {
		
		String inputPlansFile = "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/10pct_plans_30km.xml.gz";
		String inputNetworkFile = "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/output_network.xml.gz";
		String inputFacilities = "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/output_facilities.xml.gz";
		
		double populationFraction=0.1;
		// e.g. 0.1 means 10% of input population
		
		String outputPlansFile="c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/1pct_plans_30km.xml.gz";		
		
		// NOTE: if no facilities file is available for the scenario, just specify any valid facilities file
		Scenario scenario= GeneralLib.readScenario(inputPlansFile, inputNetworkFile,inputFacilities);
		new PopulationWriter(scenario.getPopulation(),scenario.getNetwork(),populationFraction).write(outputPlansFile);
	}

}
