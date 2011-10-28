package playground.gregor;

import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.run.NetworkCleaner;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class Startup {


	public static void main(String [] args) {

		String [] argsII = {"/Users/laemmel/devel/sim2dDemoII/input/network.xml","/Users/laemmel/devel/sim2dDemoII/raw_input/networkL.shp","/Users/laemmel/devel/sim2dDemoII/raw_input/networkP.shp","EPSG:3395"};
		Links2ESRIShape.main(argsII);

		final String populationFilename = "/Users/laemmel/devel/sim2dDemoII/input/20.plans.xml.gz";
		final String networkFilename = "/Users/laemmel/devel/sim2dDemoII/input/network.xml";
		//		final String populationFilename = "./test/scenarios/berlin/plans_hwh_1pct.xml.gz";
		//		final String networkFilename = "./test/scenarios/berlin/network.xml.gz";

		final String outputDir = "/Users/laemmel/tmp/vis/";

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFilename);



		new MatsimPopulationReader(scenario).readFile(populationFilename);

		CoordinateReferenceSystem crs = MGC.getCRS("EPSG:3395");
		SelectedPlans2ESRIShape sp = new SelectedPlans2ESRIShape(scenario.getPopulation(), scenario.getNetwork(), crs, outputDir);
		sp.setOutputSample(1.);
		sp.setActBlurFactor(0);
		sp.setLegBlurFactor(0);
		sp.setWriteActs(true);
		sp.setWriteLegs(true);

		sp.write();


	}

}
