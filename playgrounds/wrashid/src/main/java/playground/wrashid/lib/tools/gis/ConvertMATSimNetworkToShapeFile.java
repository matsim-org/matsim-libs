package playground.wrashid.lib.tools.gis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class ConvertMATSimNetworkToShapeFile {

	public static void main(String[] args) {
		String netFileName = "C:/data/parkingSearch/psim/zurich/inputs/ktiRun24/output_network.xml.gz";
		String outputFileP ="C:/data/parkingSearch/psim/zurich/inputs/ktiRun24//network.shp";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().global().setCoordinateSystem("CH1903_LV03_GT");

		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(netFileName);

		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, "CH1903_LV03_GT");
		CoordinateReferenceSystem crs = MGC.getCRS("CH1903_LV03_GT");
		builder.setWidthCoefficient(1);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		new Links2ESRIShape(network,outputFileP, builder).write();
	}
	
}
