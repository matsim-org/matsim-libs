package playground.wrashid.lib.tools.plan;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;

import playground.wrashid.lib.GeneralLib;

public class PlanShrinker {

	public static void main(final String[] args) {
		String inputPlansFile="A:/data/matsim/input/runRW1003/plans-10pct-miv-dilzh30km-unmapped.xml.gz";
		String inputNetworkFile="A:/data/matsim/input/runRW1003/network-osm-ch.xml.gz";
		String inputFacilities="A:/data/matsim/input/runRW1003/facilities.zrhCutC.xml.gz";
		double populationFraction=0.1;
		// e.g. 0.1 means 10% of input population
		
		String outputPlansFile="v:/data/v-temp/plans-new.xml.gz";		
		
		Scenario scenario= GeneralLib.readScenario(inputPlansFile, inputNetworkFile,inputFacilities);
		new PopulationWriter(scenario.getPopulation(),scenario.getNetwork(),populationFraction).write(outputPlansFile);
	}

}
