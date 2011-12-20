package osm;

import gis.arcgis.ShapeFileWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_5.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import osm.RelationWriter.OSMEntityCollector;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

public class OSM2Shape implements Sink{
	
	public static String NODES = "nodes";
	
	public static String WAYS = "ways";
	
	public static String RELATIONS = "relations";
	
	private FeatureCollection features;
	
	private List<String> keyNames = new ArrayList<String>();
	
	private Class<? extends Geometry> geometry;

	private EntityType entityType;

	private OSMEntityCollector entityCollector;
	
	public OSM2Shape() {
		super();
		features = FeatureCollections.newCollection();
		new ArrayList<Node>();
		entityCollector = new OSMEntityCollector();
	}
	
	public void addFeatureColumn(String keyName){
		keyNames.add(keyName);
	}
	
	public void setType2Geometry(EntityType type, Class<? extends Geometry> geoClass){
		this.entityType = type;
		this.geometry = geoClass;
	}
	
	public void write(String filename){
		if(entityType.equals(EntityType.Node)){
			for(Node node : entityCollector.getAllNodes().values()){
				SimpleFeature nodeFeature = createFeature(node);
				if(nodeFeature != null){
					features.add(nodeFeature);
				}
			}
		}
		if(entityType.equals(EntityType.Way)){
			for(Way way : entityCollector.getAllWays().values()){
				SimpleFeature wayFeature = createFeature(way);
				if(wayFeature != null){
					features.add(wayFeature);
				}
			}
		}
		if(entityType.equals(EntityType.Relation)){
			for(Relation relation : entityCollector.getAllRelations().values()){
				features.add(createRelationFeature(relation));
			}
		}
		new ShapeFileWriter(features).writeFeatures(filename);

	}
	
	private SimpleFeature createRelationFeature(Relation relation) {
		// TODO Auto-generated method stub
		return null;
	}

	private SimpleFeature createFeature(Entity entity) {
		SimpleFeatureType featureType = createFeatureType();
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
		Geometry geometry = null; 
		if(entity.getType().name().equals(EntityType.Way.name())){
			geometry = createWayGeometry((Way)entity); 
		}
		else if(entity.getType().name().equals(EntityType.Node.name())){
			geometry = createNodeGeometry((Node)entity);
		}
		if(geometry == null){
			return null;
		}
		featureBuilder.add(geometry);
		for(String key : keyNames){
			featureBuilder.add(getValue(entity,key));
		}
		return featureBuilder.buildFeature(null);
	}

	private Geometry createNodeGeometry(Node entity) {
		return new GeometryFactory().createPoint(getCoordinate((Node)entity));
	}

	private Geometry createWayGeometry(Way way) {
		if(geometry.getClass().isInstance(Polygon.class)){
			CoordinateList coordinates = new CoordinateList();
			if(way != null){
				for(WayNode wayNode : way.getWayNodes()){
					Node node = entityCollector.getAllNodes().get(wayNode.getNodeId());
					if(node != null){
						Coordinate coord = getCoordinate(node);
						coordinates.add(coord);
					}
				}
			}
			if(coordinates.size() < 4){
				return null;
			}
			coordinates.closeRing();
			return new GeometryFactory().createPolygon(new GeometryFactory().createLinearRing(coordinates.toCoordinateArray()), null);	
		}
		else {
			throw new UnsupportedOperationException("another geometry than polygon is not yet supported");
		}
	}

	private static Coordinate getCoordinate(Node node) {
		return new Coordinate(node.getLongitude(),node.getLatitude());
	}
	
	private static String getValue(Entity entity, String key){
		for(Tag t : entity.getTags()){
			if(t.getKey().equals(key)){
				return t.getValue();
			}
		}
		return "n.a.";
	}

	private SimpleFeatureType createFeatureType() {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(entityType.toString());
        builder.setCRS(DefaultGeographicCRS.WGS84);
        builder.add(geometry.getClass().toString(), geometry); 
        for(String key : keyNames){
        	builder.add(key,String.class);
        }
        final SimpleFeatureType featureType = builder.buildFeatureType();
        return featureType;
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
	public void process(EntityContainer entityContainer) {
		entityContainer.process(entityCollector);
	}

	
}
