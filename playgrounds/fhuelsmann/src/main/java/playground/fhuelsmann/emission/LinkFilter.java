package playground.fhuelsmann.emission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
//	private Map<Id, double[]> nodeCollector = new TreeMap<Id,double[]>();
	
	private final Map<Id, Node> nodeCollector = new TreeMap<Id, Node>();
	
	public LinkFilter(Network network) {
	super();
	this.network = network;
}
	
	Network getRelevantNetwork(Set<Feature> featuresInShape) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network filteredNetwork = scenario.getNetwork();
//		NetworkImpl filteredNetwork = scenario.getNetwork();
			
		for (Node node : network.getNodes().values()) {

			if (isNodeInShape(node, featuresInShape)) {
				Node fromtonode = filteredNetwork.getFactory().createNode(node.getId(),node.getCoord());
				filteredNetwork.addNode(fromtonode);
			}
		}
	
	 
		for(Link link : network.getLinks().values()){
		
			if (filteredNetwork.getNodes().containsKey(link.getFromNode().getId()) && filteredNetwork.getNodes().containsKey(link.getToNode().getId())
					&& !filteredNetwork.getLinks().containsKey(link.getId())){

				Link onelink = filteredNetwork.getFactory().createLink(link.getId(), link.getFromNode().getId(), link.getToNode().getId());
				filteredNetwork.addLink(onelink);
//				System.out.println("onelink " + onelink.getId());
			}
		}	
				
//		System.out.println("Result "+ filteredNetwork +"         " +filteredNetwork.getLinks());
		
		return filteredNetwork;	
	
	}

		
			
	public Map<Id, Node> getNodeCollector() {
		return nodeCollector;
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
	
	
	Set<Feature> readShape(String shapeFile) {
		final Set<Feature> featuresInShape;
		featuresInShape = new ShapeFileReader().readFileAndInitialize(shapeFile);
		return featuresInShape;
	}

}
