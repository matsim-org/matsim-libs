package playground.pieter.singapore.utils.plans;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class SelectedCarplansToESRIShape {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if(args.length <4){
			System.out.println("USAGE: plansfile networkfile CRS outputDir");
			System.exit(0);
		}
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(args[1]);
		new MatsimPopulationReader(scenario).readFile(args[0]);
		CoordinateReferenceSystem crs = MGC.getCRS(args[2]);
		Population pop = scenario.getPopulation();
		new PlansStripOutTransitPlans().run(pop);
		SelectedPlans2ESRIShape sp = new SelectedPlans2ESRIShape(scenario.getPopulation(), scenario.getNetwork(), crs, args[3]);
		sp.setOutputSample(0.05);
		sp.setActBlurFactor(100);
		sp.setLegBlurFactor(100);
		sp.setWriteActs(true);
		sp.setWriteLegs(true);

		sp.write();
		
		
	}

}
