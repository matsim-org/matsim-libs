package playground.wrashid.lib.tools.plan;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.PopulationWriter;

import playground.wrashid.lib.GeneralLib;

public class PlanShrinker {

	public static void main(final String[] args) {
		String inputPlansFile="H:/data/cvs/ivt/studies/switzerland/plans/teleatlas-ivtcheu-zrhCutC/census2000v2_zrhCutC_25pct/plans.xml.gz";
		String inputNetworkFile="H:/data/cvs/ivt/studies/switzerland/networks/teleatlas-ivtcheu-zrhCutC/network.xml.gz";
		String inputFacilities="H:/data/cvs/ivt/studies/switzerland/facilities/facilities.zrhCutC.xml.gz";
		double populationFraction=0.04;
		// e.g. 0.1 means 10% of input population
		
		String outputPlansFile="H:/data/experiments/ARTEMIS/input/plans_census2000v2_zrhCutC_1pct.xml.gz";		
		
		// NOTE: if no facilities file is available for the scenario, just specify any valid facilities file
		Scenario scenario= GeneralLib.readScenario(inputPlansFile, inputNetworkFile,inputFacilities);
		new PopulationWriter(scenario.getPopulation(),scenario.getNetwork(),populationFraction).write(outputPlansFile);
	}

}
