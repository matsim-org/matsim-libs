package playground.artemc.scenarioTools;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;
import java.util.Map;



public class UpdateNodesFromShape {

	private static final Logger log = Logger.getLogger(UpdateNodesFromShape.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		
		String networkPath = args[0];
		String newNetworkPath = args[1];
		String shapeFilePath = args[2];
		
		//String shapeFile = "C:/Work/Roadpricing Scenarios/SiouxFalls/Network/SiouxFalls_nodes.shp";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		new NetworkReaderMatsimV1(scenario).parse(networkPath); 
		Network network = scenario.getNetwork();
		Map<Id<Node>, ? extends Node> nodes = network.getNodes();
		
		Double x = 0.0;
		Double y = 0.0;

		System.out.println(shapeFilePath);
		ShapeFileReader shapeFileReader = new ShapeFileReader();
		Collection<SimpleFeature> fts = shapeFileReader.readFileAndInitialize(shapeFilePath); 		
		log.info("Shape file contains "+fts.size()+" features!");		
		
		for(SimpleFeature ft:fts) {
			Geometry geo = (Geometry) ft.getDefaultGeometry();
			Coordinate[] coordinates = geo.getCoordinates();
			Collection<Property> properties = ft.getProperties("Id");
			
			System.out.println("Feature: "+ft.getID()+","+ft.getIdentifier().toString()+","+ft.getName().toString());
			
			for(int i=0;i<coordinates.length;i++){
				System.out.print(coordinates[i].x+","+coordinates[i].y+"   ");
				x = coordinates[i].x;
				y = coordinates[i].y;
			}
			
			
		    for (Property p :properties) {
		    	System.out.println("Value: "+p.getValue().toString());
		    	Node node = nodes.get(Id.create(p.getValue().toString(), Node.class));
		    	node.getCoord().setXY(x, y);
		    	System.out.println("Name: "+p.getName().toString());
		    	System.out.println("Descriptor: "+p.getDescriptor().toString());
		    	
		    	
		    }
		    System.out.println();
			System.out.println();
			

			NetworkWriter writer = new NetworkWriter(network);
			writer.write(newNetworkPath);
			
			
		}		

	}

}
