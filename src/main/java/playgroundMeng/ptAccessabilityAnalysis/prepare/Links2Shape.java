package playgroundMeng.ptAccessabilityAnalysis.prepare;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

public class Links2Shape {
	public static void main(String[] args) {
		
		String netfile = "C:/Users/VW3RCOM/git/matsim-code-examples/outputNetworkFileFinal.xml" ;
		String outputFileLs = "new1.shp" ;
		String defaultCRS = "EPSG:25832";
		boolean commonWealth = false; //to render Commonwealth networks correctly (e.g. drive on left-hand side of the road)

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().global().setCoordinateSystem(defaultCRS);

	
		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(netfile);
	

		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, defaultCRS);
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		builder.setWidthCoefficient(0.5);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		new Links2ESRIShape(network,outputFileLs, builder).write();

	}	
}
