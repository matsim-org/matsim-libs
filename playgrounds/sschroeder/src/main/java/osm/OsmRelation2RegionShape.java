package osm;

import java.io.File;
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
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.filter.v0_6.TagFilter;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.core.xml.v0_6.XmlReader;

import osm.RelationWriter.OSMEntityCollector;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

public class OsmRelation2RegionShape {
	
	private static Logger logger = Logger.getLogger(OsmRelation2RegionShape.class); 
	
	private FeatureCollection features;
	
	private List<String> keyNames = new ArrayList<String>();
	
	private OSMEntityCollector osmEntityCollector;
	
	public OsmRelation2RegionShape(FeatureCollection features) {
		super();
		this.features = features;
		osmEntityCollector = new OSMEntityCollector();
	}
	
	public void addFeatureColumn(String keyName){
		keyNames.add(keyName);
	}
	
	
	public void readOsmAndBuildFeatures(String filename){
		logger.info("start");
		readOsm(filename);
		StatusCounter status = new StatusCounter();
		int successfullCreated = 0;
		for(Relation rel : osmEntityCollector.getAllRelations().values()){
			Polygon polygon = buildGeometry(rel);
			if(polygon == null){
				logger.warn("cannot create geometry for relation " + rel.getId());
				continue;
			}
			else{
				successfullCreated++;
			}
			SimpleFeature feature = createFeature(rel,polygon);
			features.add(feature);
			status.printStatus();
		}
		logger.info("number geometries created " + successfullCreated);
		logger.info("done");
	}
	
	private SimpleFeature createFeature(Entity entity, Polygon polygon) {
		SimpleFeatureType featureType = createFeatureType();
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
		Geometry geometry = null; 
		featureBuilder.add(polygon);
		featureBuilder.add(entity.getId());
		for(String key : keyNames){
			featureBuilder.add(getValue(entity,key));
		}
		return featureBuilder.buildFeature(null);
	}
	
	private String getValue(Entity entity, String key){
		for(Tag t : entity.getTags()){
			if(t.getKey().equals(key)){
				return t.getValue();
			}
		}
		return "n.a.";
	}

	private void readOsm(String filename) {
		Map<String, Set<String>> tagKeyValues = new HashMap<String, Set<String>>();
		Map<String, Set<String>> emptyKVs = Collections.emptyMap();
		Set<String> emptyKeys = Collections.emptySet();

		TagFilter tagFilterWays = new TagFilter("accept-ways", emptyKeys, emptyKVs);
		TagFilter tagFilterRelations = new TagFilter("accept-relations", emptyKeys, tagKeyValues);
		TagFilter tagFilterNodes = new TagFilter("accept-node", emptyKeys, emptyKVs);
		
		XmlReader reader = new XmlReader(new File(filename), true, CompressionMethod.None);
		reader.setSink(tagFilterRelations);
		tagFilterWays.setSink(tagFilterWays);
		tagFilterRelations.setSink(tagFilterNodes);
	
		tagFilterNodes.setSink(osmEntityCollector);
		reader.run();

		logger.info("#nodes="+osmEntityCollector.getAllNodes().size());
		logger.info("#ways="+osmEntityCollector.getAllWays().size());
		logger.info("#relations="+osmEntityCollector.getAllRelations().size());
	}
	
	private SimpleFeatureType createFeatureType() {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("region");
        builder.setCRS(DefaultGeographicCRS.WGS84);
        builder.add("region", Polygon.class); 
        builder.add("id",String.class);
        for(String key : keyNames){
        	builder.add(key,String.class);
        }
        final SimpleFeatureType featureType = builder.buildFeatureType();
        return featureType;
	}
	
	private Polygon buildGeometry(Relation rel){
		LinkedList<Coordinate> coordinates = new LinkedList<Coordinate>();
		LinkedList<LinkedList<Coordinate>> ways = new LinkedList<LinkedList<Coordinate>>();
		for(RelationMember member : rel.getMembers()){
			if(member.getMemberRole().equals("outer")){
				if(member.getMemberType().equals(member.getMemberType().Way)){
					if(member.getMemberRole().equals("outer")){
						Way way = osmEntityCollector.getAllWays().get(member.getMemberId());
						if(way != null){
							LinkedList<Coordinate> wayCoordinates = new LinkedList<Coordinate>();
							for(WayNode wayNode : way.getWayNodes()){
								Node node = osmEntityCollector.getAllNodes().get(wayNode.getNodeId());
								if(node != null){
									Coordinate coord = getCoordinate(node);
									wayCoordinates.add(coord);
								}
							}
							ways.add(wayCoordinates);
						}
						else{
//							logger.warn("no way found for wayId " + member.getMemberId());
						}
					}
				}
			}
		}
		matchCoords(coordinates,ways);
		CoordinateList coordList = new CoordinateList();
		coordList.addAll(coordinates);
		Polygon poly = createPolygon(coordList);
		return poly;
	}
	
	private void matchCoords(LinkedList<Coordinate> coordinates, LinkedList<LinkedList<Coordinate>> ways) {
		if(ways.isEmpty()){
			return;
		}
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
			if(dist > 0.0005){
				logger.warn("this is strange. cannot create geometry. dist: " + dist);
				coordinates.clear();
				return;
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
	
	private double dist(Coordinate last, Coordinate first) {
		return CoordUtils.calcDistance(toCoord(last), toCoord(first));
	}

	private Coord toCoord(Coordinate first) {
		return new CoordImpl(first.x,first.y);
	}
	
	private Polygon createPolygon(CoordinateList coordinates) {
		if(coordinates.size() < 4){
			return null;
		}
		try{
			coordinates.closeRing();
			Polygon polygon = new GeometryFactory().createPolygon(new GeometryFactory().createLinearRing(coordinates.toCoordinateArray()), null);
			return polygon;
		}
		catch(IllegalArgumentException e){
			logger.warn(e.toString());
		}
		return null;
	}
	
	private Coordinate getCoordinate(Node node) {
		return new Coordinate(node.getLongitude(),node.getLatitude());
	}

}
