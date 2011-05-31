package gis.arcgis;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import facilities.Facility;
import facilities.FacilityRelation;

public class FacilityRelationShapeFileWriter {
	
	private Collection<FacilityRelation> facilityRelations;
	
	private Collection<Facility> facilities;
	
	private Map<Id,Facility> facilityMap = new HashMap<Id, Facility>();
	
	private Network network;
	
	private FeatureCollection<SimpleFeatureType,SimpleFeature> featureCollection = FeatureCollections.newCollection();

	public FacilityRelationShapeFileWriter(Collection<FacilityRelation> facilityRelations, Collection<Facility> facilities, Network network) {
		super();
		this.facilityRelations = facilityRelations;
		this.facilities = facilities;
		this.network = network;
		createFacilityMap();
	}
	
	private void createFacilityMap() {
		for(Facility f : facilities){
			facilityMap.put(f.getId(), f);
		}
	}

	public void write(String filename) throws IOException{
		createFeatures();
		writeFeatures(filename);
	}

	private void writeFeatures(String filename) throws IOException {
		ShapeFileWriter shpWriter = new ShapeFileWriter(featureCollection);
		shpWriter.writeFeatures(filename);
	}

	private void createFeatures() {
		SimpleFeatureType featureType = createFeatureType();
		for(FacilityRelation rel : facilityRelations){
			SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
			Id fromLocationId = facilityMap.get(rel.getFromFacility()).getLocationId();
			Coordinate fromCoord = getCoordinate(fromLocationId);
			Id toLocationId = facilityMap.get(rel.getToFacility()).getLocationId();
			Coordinate toCoord = getCoordinate(toLocationId);
			Coordinate[] coords = { fromCoord, toCoord };
			featureBuilder.add(new GeometryFactory().createLineString(coords));
			featureBuilder.add(rel.getFromFacility().toString());
			featureBuilder.add(rel.getToFacility().toString());
			featureBuilder.add(rel.getSize());
			if(rel.getTimeWindow() != null){
				featureBuilder.add(rel.getTimeWindow().getStartTime());
				featureBuilder.add(rel.getTimeWindow().getEndTime());
				featureBuilder.add(rel.getTimeWindow().getStartTime());
			}
			featureCollection.add(featureBuilder.buildFeature(null));
		}
		
		
		
	}

	private Coordinate getCoordinate(Id facilityLocation) {
		Link link = network.getLinks().get(facilityLocation);
		return new Coordinate(link.getCoord().getX(),link.getCoord().getY());
	}

	private SimpleFeatureType createFeatureType() {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("FacilityRelation");
		try {
			builder.setCRS(CRS.decode("EPSG:32632"));
		} catch (NoSuchAuthorityCodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // <- Coordinate reference system
		builder.add("FacilityRelation", LineString.class);
		builder.add("FromFacilityId", String.class); 
		builder.add("ToFacilityId", String.class);
		builder.add("NofPalletsPerDay", Integer.class);
		builder.add("StartTime", String.class);
		builder.add("EndTime", String.class);
		builder.add("TimeUnit", String.class);

		// build the type
		final SimpleFeatureType featureType = builder.buildFeatureType();

		return featureType;

	}

}
