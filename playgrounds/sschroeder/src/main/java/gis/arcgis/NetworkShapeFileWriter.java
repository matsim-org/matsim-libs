package gis.arcgis;

import java.io.IOException;

import kid.GeotoolsTransformation;
import kid.filter.SimpleFeatureFilter;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class NetworkShapeFileWriter {
	
	private Network network;
	
	private GeotoolsTransformation transformation;
	
	private CoordinateReferenceSystem referenceSystem;
	
	private FeatureCollection<SimpleFeatureType,SimpleFeature> links = FeatureCollections.newCollection();
	
	private FeatureCollection<SimpleFeatureType,SimpleFeature> nodes = FeatureCollections.newCollection();
	
	private SimpleFeatureFilter featureFilter = new SimpleFeatureFilter() {
		
		public boolean judge(SimpleFeature feature) {
			return true;
		}
	};;;

	public NetworkShapeFileWriter(Network network, GeotoolsTransformation transformation) {
		super();
		this.network = network;
		this.transformation = transformation;
		referenceSystem = transformation.getOldReferenceSystem();
	}
	
	public void setFeatureFilter(SimpleFeatureFilter featureFilter) {
		this.featureFilter = featureFilter;
	}

	public NetworkShapeFileWriter(Network network, CoordinateReferenceSystem referenceSystem) {
		super();
		this.network = network;
		this.referenceSystem = referenceSystem;
	}
	
	public void write(String linkFilename, String nodeFilename) throws IOException{
		createLinkFeatures();
		createNodeFeatures();
		writeFeatures(linkFilename,nodeFilename);
	}

	private void createLinkFeatures() {
		SimpleFeatureType linkType = createLinkType();
		for(Link link : network.getLinks().values()){
			SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(linkType);
			LineString line = new GeometryFactory().createLineString(getCoordinates(link));
			fBuilder.add(line);
			fBuilder.add(link.getId().toString());
			fBuilder.add(link.getFromNode().getId().toString());
			fBuilder.add(link.getToNode().getId().toString());
			fBuilder.add(link.getLength());
			fBuilder.add(link.getFreespeed());
			fBuilder.add(link.getCapacity());
			SimpleFeature feature = fBuilder.buildFeature(null);
			SimpleFeature newFeature = null;
			if(transformation != null){
				newFeature = transformation.transformFeature(feature);
			}
			else {
				newFeature = feature;
			}
			if(featureFilter.judge(newFeature)){
				links.add(newFeature);
			}
		}
	}
	
	private void createNodeFeatures() {
		SimpleFeatureType nodeType = createNodeType();
		for(Node node : network.getNodes().values()){
			SimpleFeatureBuilder fBuilder = new SimpleFeatureBuilder(nodeType);
			fBuilder.add(new GeometryFactory().createPoint(getCoordinate(node)));
			fBuilder.add(node.getId());
			SimpleFeature feature = fBuilder.buildFeature(null); 
			SimpleFeature newFeature = null;
			if(transformation != null){
				newFeature = transformation.transformFeature(feature);
			}
			else {
				newFeature = feature;
			}
			if(featureFilter.judge(newFeature)){
				nodes.add(newFeature);	
			}
		}
	}

	private Coordinate getCoordinate(Node node) {
		return new Coordinate(node.getCoord().getX(),node.getCoord().getY());
	}

	private Coordinate[] getCoordinates(Link link) {
		Coordinate x = new Coordinate(link.getFromNode().getCoord().getX(),link.getFromNode().getCoord().getY());
		Coordinate y = new Coordinate(link.getToNode().getCoord().getX(),link.getToNode().getCoord().getY());
		Coordinate[] coords = {x,y};
		return coords;
	}

	private SimpleFeatureType createLinkType() {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("Links");
		builder.setCRS(referenceSystem);
		builder.add("Link", LineString.class);
		builder.add("Id",String.class);
		builder.add("From", String.class); 
		builder.add("To", String.class);
		builder.add("Length", Double.class);
		builder.add("FreeSpeed", Double.class);
		builder.add("Capacity", Double.class);
		
		final SimpleFeatureType featureType = builder.buildFeatureType();

		return featureType;
	}
	
	private SimpleFeatureType createNodeType() {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("Nodes");
		builder.setCRS(referenceSystem);
		builder.add("Node", Point.class);
		builder.add("Id",String.class);
		return builder.buildFeatureType();
	}

	private void writeFeatures(String linkFilename, String nodeFilename) throws IOException {
		ShapeFileWriter linkWriter = new ShapeFileWriter(links);
		linkWriter.writeFeatures(linkFilename);
		ShapeFileWriter nodeWriter = new ShapeFileWriter(nodes);
		nodeWriter.writeFeatures(nodeFilename);
	}

}
