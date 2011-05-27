package playground.mzilske.teach;

import java.util.ArrayList;
import java.util.Collection;

import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.ConfigUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class NetworkToShape {

	public static void main(String[] args) throws Exception {

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("network.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();

		GeometryFactory geoFac = new GeometryFactory();
		CoordinateReferenceSystem crs = MGC.getCRS("EPSG:21781");    // EPSG Code for Swiss CH1903_LV03 coordinate system

		Collection<Feature> features = null;
		AttributeType geom = null;
		AttributeType id = null;
		AttributeType fromNode = null;
		AttributeType toNode = null;
		AttributeType length = null;
		AttributeType type = null;
		AttributeType capacity = null;
		AttributeType freespeed = null;
		FeatureType ftRoad = null;
		FeatureType ftNode = null;

		features = new ArrayList<Feature>();
		geom = DefaultAttributeTypeFactory.newAttributeType("LineString", Geometry.class, true, null, null, crs);
		id = AttributeTypeFactory.newAttributeType("ID", String.class);
		fromNode = AttributeTypeFactory.newAttributeType("fromID", String.class);
		toNode = AttributeTypeFactory.newAttributeType("toID", String.class);
		length = AttributeTypeFactory.newAttributeType("length", Double.class);
		type = AttributeTypeFactory.newAttributeType("type", String.class);
		capacity = AttributeTypeFactory.newAttributeType("capacity", Double.class);
		freespeed = AttributeTypeFactory.newAttributeType("freespeed", Double.class);
		ftRoad = FeatureTypeBuilder.newFeatureType(new AttributeType[] {geom, id, fromNode, toNode, length, type, capacity, freespeed}, "link");

		for (Link link : network.getLinks().values()) {
			Coordinate fromNodeCoordinate = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
			Coordinate toNodeCoordinate = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
			Coordinate linkCoordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
			LineString ls = new LineString(new CoordinateArraySequence(new Coordinate [] {fromNodeCoordinate, linkCoordinate, toNodeCoordinate}), geoFac);
			Feature ft = ftRoad.create(new Object [] {ls , link.getId().toString(), link.getFromNode().getId().toString(),link.getToNode().getId().toString(), link.getLength(), ((LinkImpl)link).getType(), link.getCapacity(), link.getFreespeed()}, "links");
			features.add(ft);
		}   
		ShapeFileWriter.writeGeometries(features, "network_links.shp");

		features = new ArrayList<Feature>();
		geom = DefaultAttributeTypeFactory.newAttributeType("Point", Point.class, true, null, null, crs);
		id = AttributeTypeFactory.newAttributeType("ID", String.class);
		ftNode = FeatureTypeBuilder.newFeatureType(new AttributeType[] {geom, id}, "node");

		for (Node node : network.getNodes().values()) {
			Coordinate nodeCoordinate = new Coordinate(node.getCoord().getX(), node.getCoord().getY());
			Point point = geoFac.createPoint(nodeCoordinate);

			Feature ft = ftNode.create(new Object[] {point, node.getId().toString()}, "nodes");
			features.add(ft);
		}
		ShapeFileWriter.writeGeometries(features, "network_nodes.shp");
	}
}
