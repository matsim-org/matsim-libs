package playground.fhuelsmann.emission;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import org.matsim.api.core.v01.Id;


public class LinkFilter {
	
	private static final Logger logger = Logger.getLogger(LinkFilter.class);
	private Network network;
	
	public LinkFilter(Network network) {
	super();
	this.network = network;
}
	
	Network getRelevantNetwork(Set<Feature> featuresInShape) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl filteredNetwork = scenario.getNetwork();

		for (Node node : network.getNodes().values()) {

			if (isNodeInShape(node, featuresInShape)) {
				for(Link link : network.getLinks().values()){
					filteredNetwork.createAndAddNode(node.getId(), node.getCoord());
						if(isLinkInShape(link,featuresInShape)){
							filteredNetwork.createAndAddLink(link.getId(), link.getFromNode(), link.getToNode(), 0.0,0.0,0.0,0.0);
						
						}
				}
			}		
		}
		

		System.out.println(filteredNetwork.getLinks());
		return filteredNetwork;
		
	}

	private boolean isNodeInShape(Node node,Set<Feature> featuresInShape) {
		boolean isInShape = false;

		Coord nodeCoord = node.getCoord();
		//System.out.println("nodeCoord   " + nodeCoord);
		GeometryFactory factory = new GeometryFactory();
		Coordinate coor = new Coordinate(nodeCoord.getX(), nodeCoord.getY());
		//System.out.println("coor   " + coor);
		Geometry geo = factory.createPoint(coor);
		//System.out.println("geo   " + geo);
		for(Feature feature : featuresInShape){
			if(feature.getDefaultGeometry().contains(geo)){
				//	logger.debug("found homeLocation of person " + person.getId() + " in feature " + feature.getID());
				isInShape = true;
				break;
			}
		}
		return isInShape;
	}
	
	private boolean isLinkInShape(Link link,Set<Feature> featuresInShape) {
		boolean isInShape = false;

		Coord nodeCoord = link.getFromNode().getCoord();
		//System.out.println("nodeCoord   " + nodeCoord);
		Coord toCoord = link.getToNode().getCoord();
		//System.out.println("toCoord   " + toCoord);
		GeometryFactory factory = new GeometryFactory();
		Coordinate[] coor = {new Coordinate(nodeCoord.getX(), nodeCoord.getY()), new Coordinate(toCoord.getX(),toCoord.getY())};
		//System.out.println("coor   " + coor);
		Geometry geo = factory.createLineString(coor);
		//System.out.println("geo   " + geo);
		for(Feature feature : featuresInShape){
			if(feature.getDefaultGeometry().contains(geo)){
				//	logger.debug("found homeLocation of person " + person.getId() + " in feature " + feature.getID());
				isInShape = true;
				break;
			}
		}
		return isInShape;
	}

/*	private static boolean isLinkInShape(Coord linkCoord, Set<Feature> featuresInShape) {
		boolean isInShape = false;
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(linkCoord.getX(), linkCoord.getY()));
		for (Feature feature : featuresInShape) {
			if (feature.getDefaultGeometry().contains(geo)) {
				isInShape = true;
				break;
			}
		}
		return isInShape;
	}*/
	
	Set<Feature> readShape(String shapeFile) {
		final Set<Feature> featuresInShape;
		try {
			featuresInShape = new ShapeFileReader().readFileAndInitialize(shapeFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return featuresInShape;
	}

}
