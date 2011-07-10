package playground.wrashid.lib.tools.plan;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.PopulationWriter;

import playground.wrashid.lib.GeneralLib;

public class PlanShrinker {

	public static void main(final String[] args) {
		String inputPlansFile="K:/Projekte/herbie/output/demandCreation/plans.xml.gz";
		String inputNetworkFile="K:/Projekte/matsim/data/switzerland/networks/ivtch-multimodal/zh/network.multimodal-wu.xml.gz";
		String inputFacilities="K:/Projekte/herbie/output/demandCreation/facilitiesWFreight.xml.gz";
		double populationFraction=0.1;
		// e.g. 0.1 means 10% of input population
		
		String outputPlansFile="K:/Projekte/herbie/output/demandCreation/plans_1pct.xml.gz";		
		
		// NOTE: if no facilities file is available for the scenario, just specify any valid facilities file
		Scenario scenario= GeneralLib.readScenario(inputPlansFile, inputNetworkFile,inputFacilities);
		new PopulationWriter(scenario.getPopulation(),scenario.getNetwork(),populationFraction).write(outputPlansFile);
	}

}
