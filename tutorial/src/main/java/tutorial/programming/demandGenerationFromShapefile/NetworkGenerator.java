package tutorial.programming.demandGenerationFromShapefile;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.utils.gis.matsim2esri.network.CapacityBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;
import org.xml.sax.SAXException;

class NetworkGenerator {
	// NOT naming this RunXxx since there is already a network generation example and this one here does
	// not add anything (or if it does, that information might be copied to the other one).  kai, feb'15
	
	public static final String UTM33N = "PROJCS[\"WGS_1984_UTM_Zone_33N\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",15],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",0],UNIT[\"Meter\",1]]";
	
	// for this basic example UTM zone 33 North is the right coordinate system. This may differ depending on your scenario. See also http://en.wikipedia.org/wiki/Universal_Transverse_Mercator

	public static void main(String [] args) throws SAXException {
		String osm = "./inputs/map.osm";

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig()) ;
		Network net = sc.getNetwork();

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, UTM33N);

		OsmNetworkReader onr = new OsmNetworkReader(net,ct); //constructs a new openstreetmap reader
		onr.parse(osm); //starts the conversion from osm to matsim
		
		//at this point we already have a matsim network...
		new NetworkCleaner().run(net); // but maybe there are isolated (not connected) links. The network cleaner removes those links

		new NetworkWriter(net).write("./inputs/network.xml");//here we write the network to a xml file


		// Create an ESRI shape file from the MATSim network

		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(net, UTM33N);
		builder.setWidthCoefficient(0.01);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		new Links2ESRIShape(net,"./inputs/network.shp", builder).write();

	}

}
