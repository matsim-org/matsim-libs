package tutorial.programming.example8DemandGeneration;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.utils.gis.matsim2esri.network.CapacityBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilder;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;

/**WARNING: This class uses methods and classes that are non-API.  Result: This class will very probably not work with future 
 * versions of matsim.  Keep matsim jar file & matsim libraries that work with this version if you want to be on the safe side.
 */
public class NetworkGenerator {
	public static final String UTM33N = "PROJCS[\"WGS_1984_UTM_Zone_33N\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",15],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",0],UNIT[\"Meter\",1]]";
	
	
	
	public static void main(String [] args) {
		String osm = "./inputs/map.osm";
		
		Scenario sc = new ScenarioImpl() ;
		Network net = sc.getNetwork();
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM35S); //the coordinate transformation is needed to get a projected  coordinate system
		// for this basic example UTM zone 33 North is the right coordinate system. This may differ depending on your scenario. See also http://en.wikipedia.org/wiki/Universal_Transverse_Mercator
			
		
		OsmNetworkReader onr = new OsmNetworkReader(net,ct); //constructs a new openstreetmap reader
		try {
			onr.parse(osm); //starts the conversion from osm to matsim
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//at this point we already have a matsim network...
		new NetworkCleaner().run(net); //but may be there are isolated not connected links. The network cleaner removes those links
		
		new NetworkWriter(net).write("./inputs/network.xml");//here we write the network to a xml file

		
		//the remaining lines of code are necessary to create a ESRI shape file of the matsim network
		
		sc.getConfig().global().setCoordinateSystem(UTM33N);
		
		FeatureGeneratorBuilder builder = new FeatureGeneratorBuilder(net);
		builder.setWidthCoefficient(0.01);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		new Links2ESRIShape(net,"./inputs/network.shp", builder).write();
		
	}

}
