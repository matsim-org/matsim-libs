package playground.wrashid.lib.tools.plan;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.PopulationWriter;

import playground.wrashid.lib.GeneralLib;

public class PlanShrinker {

	public static void main(final String[] args) {
		String inputPlansFile="H:/data/experiments/TRBAug2012/input/census2000v2_8kmCut_5pct_no_network_info.xml.gz";
		String inputNetworkFile="H:/data/cvs/ivt/studies/switzerland/networks/teleatlas-ivtcheu/network.xml.gz";
		String inputFacilities="H:/data/cvs/ivt/studies/switzerland/facilities/facilities.xml.gz";
		double populationFraction=0.2;
		// e.g. 0.1 means 10% of input population
		
		String outputPlansFile="H:/data/experiments/TRBAug2012/input/census2000v2_8kmCut_1pct_no_network_info.xml.gz";		
		
		// NOTE: if no facilities file is available for the scenario, just specify any valid facilities file
		Scenario scenario= GeneralLib.readScenario(inputPlansFile, inputNetworkFile,inputFacilities);
		new PopulationWriter(scenario.getPopulation(),scenario.getNetwork(),populationFraction).write(outputPlansFile);
	}

}
