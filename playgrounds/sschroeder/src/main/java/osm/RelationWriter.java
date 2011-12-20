package osm;

import gis.arcgis.ShapeFileWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.filter.v0_6.TagFilter;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.core.xml.v0_6.XmlReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

public class RelationWriter {
	
	public static class OSMEntityCollector implements Sink, EntityProcessor {

		private Map<Long, Node> nodes;
		private Map<Long, Way> ways;
		private Map<Long, Relation> relations;
		private Map<Long, Bound> bounds;
	
		public OSMEntityCollector() {
			nodes = new HashMap<Long, Node>();
			ways = new HashMap<Long, Way>();
			relations = new HashMap<Long, Relation>();
			bounds = new HashMap<Long, Bound>();
		}

		@Override
		public void complete() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void release() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void process(BoundContainer bound) {
			bounds.put(bound.getEntity().getId(), bound.getEntity());
			
		}

		@Override
		public void process(NodeContainer node) {
			nodes.put(node.getEntity().getId(), node.getEntity());
			
		}

		@Override
		public void process(WayContainer way) {
			ways.put(way.getEntity().getId(), way.getEntity());
			
		}

		@Override
		public void process(RelationContainer relation) {
			relations.put(relation.getEntity().getId(), relation.getEntity());
			
		}

		@Override
		public void process(EntityContainer entityContainer) {
			entityContainer.process(this);
			
		}

		public  Map<Long, Node> getAllNodes() {
			return nodes;
		}

		public Map<Long, Way> getAllWays() {
			return ways;
		}

		public Map<Long, Relation> getAllRelations() {
			return relations;
		}
		
		public Map<Long, Bound> getAllBounds(){
			return bounds;
		}
		
	}
	
	private static Logger logger = Logger.getLogger(RelationWriter.class);
	
	public static void main(String[] args) {
		System.out.println("start");
		
		Map<String, Set<String>> tagKeyValues = new HashMap<String, Set<String>>();
//		tagKeyValues.put("name", new HashSet<String>(Arrays.asList("Karlsruhe, Stadt")));
//		tagKeyValues.put("admin_level", new HashSet<String>(Arrays.asList("6")));//,"8","9","10"
//		tagKeyValues.put("boundary", new HashSet<String>(Arrays.asList("administrative")));//,"8","9","10"
		String filename = "/Users/stefan/Documents/Spielwiese/data/osm/ka_plz.osm";
		Map<String, Set<String>> emptyKVs = Collections.emptyMap();
		Set<String> emptyKeys = Collections.emptySet();
		
		TagFilter tagFilterWays = new TagFilter("accept-ways", emptyKeys, emptyKVs);
		TagFilter tagFilterRelations = new TagFilter("accept-relations", emptyKeys, tagKeyValues);
		TagFilter tagFilterNodes = new TagFilter("accept-node", emptyKeys, emptyKVs);
		
		XmlReader reader = new XmlReader(new File(filename), true, CompressionMethod.None);
		reader.setSink(tagFilterRelations);
		tagFilterWays.setSink(tagFilterWays);
		tagFilterRelations.setSink(tagFilterNodes);
		
		OSMEntityCollector osmEntitiyColl = new OSMEntityCollector();
		tagFilterNodes.setSink(osmEntitiyColl);
		reader.run();
		
		System.out.println("#nodes="+osmEntitiyColl.getAllNodes().size());
		System.out.println("#ways="+osmEntitiyColl.getAllWays().size());
		System.out.println("#relations="+osmEntitiyColl.getAllRelations().size());
		
		FeatureCollection<SimpleFeatureType,SimpleFeature> featureCollection = FeatureCollections.newCollection(); 
		for(Relation rel : osmEntitiyColl.getAllRelations().values()){
			System.out.println(rel.getId());
			LinkedList<Coordinate> coordinates = new LinkedList<Coordinate>();
			LinkedList<LinkedList<Coordinate>> ways = new LinkedList<LinkedList<Coordinate>>();
			for(RelationMember member : rel.getMembers()){
				if(member.getMemberRole().equals("outer")){
					if(member.getMemberType().equals(member.getMemberType().Way)){
						if(member.getMemberRole().equals("outer")){
							Way way = osmEntitiyColl.getAllWays().get(member.getMemberId());
							if(way != null){
								LinkedList<Coordinate> wayCoordinates = new LinkedList<Coordinate>();
								for(WayNode wayNode : way.getWayNodes()){
									Node node = osmEntitiyColl.getAllNodes().get(wayNode.getNodeId());
									if(node != null){
										Coordinate coord = getCoordinate(node);
										wayCoordinates.add(coord);
									}
								}
								ways.add(wayCoordinates);
//								addCoords(coordinates,wayCoordinates);
							}
							else{
								logger.warn("no nodes found for way " + member.getMemberId());
							}
						}
					}
				}
			}
			matchCoords(coordinates,ways);
			if(coordinates.size() >= 4){
				CoordinateList coordList = new CoordinateList();
				coordList.addAll(coordinates);
				SimpleFeature feature = createSimpleFeature(coordList,rel);
				featureCollection.add(feature);
			}
			else{
				continue;
			}
		}
		new ShapeFileWriter(featureCollection).writeFeatures("/Volumes/parkplatz/Stefan/ka_plz_v2.shp");

	}

	private static void matchCoords(LinkedList<Coordinate> coordinates, LinkedList<LinkedList<Coordinate>> ways) {
		if(coordinates.isEmpty()){
			coordinates.addAll(ways.pollFirst());
		}
		while(!ways.isEmpty()){
			Coordinate lastCoord = coordinates.getLast();
			LinkedList<Coordinate> next = null;
			boolean reverseOrder = false;
			double dist = Double.MAX_VALUE;
			for(LinkedList<Coordinate> wayCoords : ways){
				if(dist(lastCoord,wayCoords.getFirst()) < dist){
					reverseOrder = false;
					dist = dist(lastCoord,wayCoords.getFirst());
					next = wayCoords;
				}
				if(dist(lastCoord,wayCoords.getLast()) < dist){
					reverseOrder = true;
					dist = dist(lastCoord,wayCoords.getLast());
					next = wayCoords;
				}
			}
			if(reverseOrder){
				Iterator<Coordinate> iter = next.descendingIterator();
				while(iter.hasNext()){
					coordinates.add(iter.next());
				}
			}
			else{
				coordinates.addAll(next);
			}
			ways.remove(next);
		}
	}

	private static void addCoords(LinkedList<Coordinate> coordinates, LinkedList<Coordinate> wayCoordinates) {
		if(coordinates.isEmpty()){
			coordinates.addAll(wayCoordinates);
			return;
		}
		Coordinate last = coordinates.getLast();
		if(dist(last,wayCoordinates.getLast()) < dist(last,wayCoordinates.getFirst())){
			Iterator<Coordinate> iter = wayCoordinates.descendingIterator();
			while(iter.hasNext()){
				coordinates.add(iter.next());
			}
		}
		else{
			coordinates.addAll(wayCoordinates);
		}
	}

	private static double dist(Coordinate last, Coordinate first) {
		return CoordUtils.calcDistance(toCoord(last), toCoord(first));
	}

	private static Coord toCoord(Coordinate first) {
		return new CoordImpl(first.x,first.y);
	}

	private static void writeToTxt(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) throws IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter("/Users/stefan/Documents/Spielwiese/data/osm/polygonFilter.poly");
		writer.write("polygonFilter\n");
		int counter = 1;
		FeatureIterator<SimpleFeature> featureIter = featureCollection.features();
		while(featureIter.hasNext()){
			SimpleFeature f  = featureIter.next();
			writer.write(counter + "\n");
			for(Coordinate coord : ((Polygon)f.getDefaultGeometry()).getCoordinates()){
				writer.write(coord.x + " " + coord.y + "\n");
			}
			counter++;
			writer.write("END\n");
		}
		writer.write("END");
		writer.close();
	}

	private static org.opengis.feature.simple.SimpleFeature createSimpleFeature(CoordinateList coordinates, Relation rel) {
		SimpleFeatureType featureType = createFeatureType();
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
		featureBuilder.add(createPolygon(coordinates));
		featureBuilder.add(rel.getId());
		featureBuilder.add(getName(rel));
		featureBuilder.add(getLevel(rel));
		featureBuilder.add("region");
		return featureBuilder.buildFeature(null);
	}
	
	private static String getLevel(Relation rel) {
		for(Tag t : rel.getTags()){
			if(t.getKey().equals("admin_level")){
				return t.getValue();
			}
		}
		return "n.a.";
	}

	private static String getName(Relation rel) {
		for(Tag t : rel.getTags()){
			if(t.getKey().equals("postal_code")){
				return t.getValue();
			}
		}
		return "n.a.";
	}

	private static Polygon createPolygon(CoordinateList coordinates) {
		try{
			coordinates.closeRing();
//			new GeometryFactory().c
			Polygon polygon = new GeometryFactory().createPolygon(new GeometryFactory().createLinearRing(coordinates.toCoordinateArray()), null);
			return polygon;
		}
		catch(IllegalArgumentException e){
			logger.warn(e.toString());
		}
		return null;
	}

	private static SimpleFeatureType createFeatureType() {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Polygon");
        builder.setCRS(DefaultGeographicCRS.WGS84);
        builder.add("Polygon", Polygon.class);
        builder.add("Id", String.class); 
        builder.add("Name", String.class);
        builder.add("Level",String.class);
        builder.add("Type", String.class);
  
        final SimpleFeatureType featureType = builder.buildFeatureType();

        return featureType;
	}

	private static Coordinate getCoordinate(Node node) {
		return new Coordinate(node.getLongitude(),node.getLatitude());
	}

}
