package playground.wrashid.lib.tools.plan;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.PopulationWriter;


public class PlanShrinker {

	public static void main(final String[] args) {
		String inputPlansFile="C:/data/parkingSearch/zurich/input/10pct_plans_ktiClean.xml.gz";
		String inputNetworkFile="H:/data/cvs/ivt/studies/switzerland/networks/teleatlas-ivtcheu/network.xml.gz";
		String inputFacilities="H:/data/cvs/ivt/studies/switzerland/facilities/facilities.xml.gz";
		double populationFraction=0.01;
		// e.g. 0.1 means 10% of input population
		
		String outputPlansFile="C:/data/parkingSearch/zurich/input/1pml_plans_ktiClean.xml.gz";		
		
		// NOTE: if no facilities file is available for the scenario, just specify any valid facilities file
		Scenario scenario= GeneralLib.readScenario(inputPlansFile, inputNetworkFile,inputFacilities);
		new PopulationWriter(scenario.getPopulation(),scenario.getNetwork(),populationFraction).write(outputPlansFile);
	}

}
