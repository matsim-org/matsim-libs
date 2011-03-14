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
		System.out.println("*******************************************");
		for (Node node : filteredNetwork.getNodes().values()) {
			if (isNodeInShape(node, featuresInShape)) {
				Node fromNode = filteredNetwork.createAndAddNode(node.getId(), node.getCoord());
				Node toNode = filteredNetwork.createAndAddNode(node.getId(), node.getCoord());
				System.out.println("filteredNetwork.getNodes().values() " +toNode);
				for(Link link : filteredNetwork.getLinks().values()){
					//	System.out.println(isLinkInShape(link,featuresInShape));
						if(isLinkInShape(link,featuresInShape)){
							filteredNetwork.createAndAddLink(link.getId(),fromNode,toNode,0.0,0.0,0.0,0.0);		
//							Node toNode = filteredNetwork.createAndAddNode(linkId.getKey(),linkId.getValue().getToNode().getCoord());;
						}
				}
			}		
		}
		
/*		Map<Id, Link> LinksMap =  (Map<Id, Link>) network.getLinks(); // Map | ID; link
		for(Entry<Id,Link> linkId : LinksMap .entrySet()){
		//	System.out.println(isLinkInShape(linkId.getValue(),featuresInShape));
			if(isLinkInShape(linkId.getValue(),featuresInShape)){
				

//				Node fromNode = filteredNetwork.createAndAddNode(linkId.getKey(),linkId.getValue().getFromNode().getCoord());		
//				Node toNode = filteredNetwork.createAndAddNode(linkId.getKey(),linkId.getValue().getToNode().getCoord());
			
				filteredNetwork.createAndAddLink(linkId.getKey(),fromNode ,toNode ,0.0, 0.0, 0.0, 0.0);}
//				filteredNetwork.getNodes().get(linkId.getValue().getFromNode().getCoord()).addInLink(linkId.getValue());
		}*/
		System.out.println(filteredNetwork.getLinks());
		return filteredNetwork;
		
	}

	private boolean isNodeInShape(Node node,Set<Feature> featuresInShape) {
		boolean isInShape = false;

		Coord nodeCoord = node.getCoord();
		//System.out.println("nodeCoord   " + nodeCoord);
		GeometryFactory factory = new GeometryFactory();
		Coordinate[] coor = {new Coordinate(nodeCoord.getX(), nodeCoord.getY())};
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
