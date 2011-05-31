package kid;

import gis.arcgis.ShapeFileWriter;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LinearGeometryBuilder;



public class KiDShapeFileWriter {
	
	private static Logger logger = Logger.getLogger(KiDShapeFileWriter.class);
	
	private ScheduledVehicles scheduledVehicles;

	private String shapeFileName;
	
	private boolean writeShapes = true;
	
	private boolean writeNodes = true;
	
	private FeatureCollection features = FeatureCollections.newCollection();
	
	private FeatureCollection nodeFeatures = FeatureCollections.newCollection();

	private String nodeFileName;

	public void setShapeFileName(String shapeFileName) {
		this.shapeFileName = shapeFileName;
	}

	public KiDShapeFileWriter(ScheduledVehicles scheduledVehicles) {
		super();
		this.scheduledVehicles = scheduledVehicles;
	}
	
	public void run() throws IOException{
		createFeatures();
		writeFeatures();
	}

	private void writeFeatures() throws IOException {
		logger.info("write tours");
		ShapeFileWriter tourShpWriter = new ShapeFileWriter(features);
		tourShpWriter.writeFeatures(shapeFileName);
		logger.info("write activity nodes");
		ShapeFileWriter nodeShpWriter = new ShapeFileWriter(nodeFeatures);
		nodeShpWriter.writeFeatures(nodeFileName);
	}

	public void setNodeFileName(String nodeFileName) {
		this.nodeFileName = nodeFileName;
	}

	private void createFeatures() {
		logger.info("create features");
		SimpleFeatureType featureType = createFeatureType();

		SimpleFeatureType nodeFeatureType = createNodeType();
		
		for(ScheduledVehicle v : scheduledVehicles.getScheduledVehicles().values()){
			for(ScheduledTransportChain chain : v.getScheduledTransportChains()){
				if(KiDUtils.isGeoCodable(chain)){
					SimpleFeatureBuilder tourFeatureBuilder = new SimpleFeatureBuilder(featureType);
					LinearGeometryBuilder polyLineBuilder = new LinearGeometryBuilder(new GeometryFactory());
					TransportLeg lastLeg = null;
					boolean firstLeg = true;
					
					for(TransportLeg currentLeg : chain.getTransportLegs()){
						Coordinate coord = KiDUtils.getFromGeocode(currentLeg);
						polyLineBuilder.add(coord);
						SimpleFeatureBuilder nodeBuilder = new SimpleFeatureBuilder(nodeFeatureType);
						nodeBuilder.add((new GeometryFactory()).createPoint(coord));
						if(firstLeg){
							nodeBuilder.add(KiDUtils.getSourceLocationType(currentLeg));
							nodeBuilder.add("Start");
							nodeBuilder.add("");
							firstLeg = false;
						}
						else{
							nodeBuilder.add(KiDUtils.getDestinationLocationType(lastLeg));
							nodeBuilder.add(KiDUtils.getActivity(lastLeg));
							nodeBuilder.add(KiDUtils.getArrivalTime(lastLeg));
						}
						nodeBuilder.add(KiDUtils.getDepartureTime(currentLeg));
						nodeBuilder.add(KiDUtils.getDate(currentLeg));
						nodeFeatures.add(nodeBuilder.buildFeature(null));
						lastLeg = currentLeg;
					}
					Coordinate toCoord = KiDUtils.getToGeocode(lastLeg);
					polyLineBuilder.add(toCoord);
					polyLineBuilder.endLine();
					LineString polyLine = (LineString)polyLineBuilder.getGeometry();
					tourFeatureBuilder.add(polyLine);
					tourFeatureBuilder.add(v.getVehicle().getId().toString());
					tourFeatureBuilder.add(((Integer)chain.getTransportChain().getId()));
					SimpleFeature feature = tourFeatureBuilder.buildFeature(null);
					features.add(feature);
					
					SimpleFeatureBuilder nodeBuilder = new SimpleFeatureBuilder(nodeFeatureType);
					nodeBuilder.add((new GeometryFactory()).createPoint(toCoord));
					nodeBuilder.add(KiDUtils.getDestinationLocationType(lastLeg));
					nodeBuilder.add(KiDUtils.getActivity(lastLeg));
					nodeBuilder.add(KiDUtils.getArrivalTime(lastLeg));
					nodeBuilder.add("");
					nodeBuilder.add(KiDUtils.getDate(lastLeg));
					nodeFeatures.add(nodeBuilder.buildFeature(null));
					
				}
			}
		}
	}
	
	private SimpleFeatureType createNodeType() {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("ActivityNode");
		builder.setCRS(DefaultGeographicCRS.WGS84);
		builder.add("Node", Point.class);
		builder.add("Location", String.class);
		builder.add("Activity", String.class);
		builder.add("ArrivalTime", String.class);
		builder.add("DepartureTime", String.class);
		builder.add("Date", String.class);
		final SimpleFeatureType nodeType = builder.buildFeatureType();  
		return nodeType;
	}
	
	private SimpleFeatureType createFeatureType() {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Tour");
		builder.setCRS(DefaultGeographicCRS.WGS84); // <- Coordinate reference system

        // add attributes in order
        builder.add("Tour", LineString.class);
        builder.add("VehicleId", String.class); 
        builder.add("TourId", Integer.class);

        // build the type
        final SimpleFeatureType polyLineType = builder.buildFeatureType();

        return polyLineType;

		
	}

}
