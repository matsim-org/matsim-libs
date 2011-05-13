package playground.mzilske.teach;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;


public class PotsdamNet {

	public static void main(String[] args) {
		String osm = "input/network.osm";

		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()) ;
		Network net = sc.getNetwork();

		// CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:3395"); 

		// CoordinateTransformation ct = new WGS84ToOSMMercator.Project();
		
		// CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25833"); 
		
		// CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:45833"); 
		
		String crs = "PROJCS[\"ETRS89_UTM_Zone_33\",GEOGCS[\"GCS_ETRS89\",DATUM[\"D_ETRS89\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",3500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",15.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]";
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, crs); 

		OsmNetworkReader onr = new OsmNetworkReader(net,ct); //constructs a new openstreetmap reader

		onr.parse(osm); //starts the conversion from osm to matsim

		//at this point we already have a matsim network...
		new NetworkCleaner().run(net); //but may be there are isolated not connected links. The network cleaner removes those links

		new NetworkWriter(net).write("input/network.xml");//here we write the network to a xml file
		Links2ESRIShape.main(new String[]{"input/network.xml","input/networkline.shp","input/networkpoly.shp", crs});

	}

}
