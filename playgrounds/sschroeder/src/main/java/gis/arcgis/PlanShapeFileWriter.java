package gis.arcgis;

import java.io.IOException;
import java.util.Collection;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.NetworkImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class PlanShapeFileWriter {
	
	private Collection<Plan> plans;
	
	private NetworkImpl network;
	
	private FeatureCollection<SimpleFeatureType, SimpleFeature> activityFeatureCollection = FeatureCollections.newCollection();
	
	private FeatureCollection<SimpleFeatureType, SimpleFeature> legFeatureCollection = FeatureCollections.newCollection();

	public PlanShapeFileWriter(Collection<Plan> plans, NetworkImpl network) {
		super();
		this.plans = plans;
		this.network = network;
	}
	
	public void write(String activityShpFilename, String legShpFilename) throws IOException{
		createFeatures();
		writePlans(activityShpFilename,legShpFilename);
	}

	private void writePlans(String activityShpFilename, String legShpFilename) throws IOException {
		ShapeFileWriter actShpWriter = new ShapeFileWriter(activityFeatureCollection);
		actShpWriter.writeFeatures(activityShpFilename);
		ShapeFileWriter legShpWriter = new ShapeFileWriter(legFeatureCollection);
		legShpWriter.writeFeatures(legShpFilename);
	}

	private void createFeatures() {
		SimpleFeatureType legFeatureType = createLegFeatureType();
		SimpleFeatureType actFeatureType = createActNodeFeatureType();
		int planCounter = 0;
		for(Plan p : plans){
			planCounter++;
			Id carrierId = p.getPerson().getId();
			Activity lastActivity = null;
			Coordinate lastActCoord = null;
			Leg lastLeg = null;
			boolean firstActivity = true;
			int legCounter = 0;
			for(PlanElement e : p.getPlanElements()){
				if(e instanceof Activity){
					Activity a = (Activity)e;
					Id linkId = a.getLinkId();
					Coordinate activityCoord = getActCoord(linkId);
					SimpleFeatureBuilder actFeatureBuilder = new SimpleFeatureBuilder(actFeatureType);
					actFeatureBuilder.add(new GeometryFactory().createPoint(activityCoord));
					actFeatureBuilder.add(linkId.toString());
					actFeatureBuilder.add(a.getType());
					actFeatureBuilder.add(new Double(a.getStartTime()));
					actFeatureBuilder.add(new Double(a.getEndTime()));
					if(firstActivity){
						firstActivity = false;
					}
					else{
						SimpleFeatureBuilder legFeatureBuilder = new SimpleFeatureBuilder(legFeatureType);
						Coordinate[] coordinates = { lastActCoord, activityCoord };
						legFeatureBuilder.add(new GeometryFactory().createLineString(coordinates));
						legFeatureBuilder.add(carrierId.toString());
						legFeatureBuilder.add(planCounter);
						legFeatureBuilder.add(legCounter);
						legFeatureBuilder.add(lastActivity.getLinkId());
						legFeatureBuilder.add(a.getLinkId());
						legFeatureBuilder.add(lastLeg.getDepartureTime());
						legFeatureBuilder.add(lastLeg.getTravelTime());
						legFeatureCollection.add(legFeatureBuilder.buildFeature(null));
					}
					activityFeatureCollection.add(actFeatureBuilder.buildFeature(null));
					lastActivity = a;
					lastActCoord = activityCoord;
				}
				if(e instanceof Leg){
					legCounter++;
					Leg leg = (Leg)e;
					lastLeg = leg;
				}
			}
		}
	}

	private Coordinate getActCoord(Id linkId) {
		Coord coord = network.getLinks().get(linkId).getCoord();
		return new Coordinate(coord.getX(), coord.getY());
	}
	
	private SimpleFeatureType createActNodeFeatureType(){
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("ActivityNode");
		try {
			builder.setCRS(CRS.decode("EPSG:32632"));
		} catch (NoSuchAuthorityCodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // <- Coordinate reference system

        // add attributes in order
        builder.add("ActNode", Point.class); 
        builder.add("LocationId", String.class);
        builder.add("Type", String.class);
        builder.add("Start", Double.class);
        builder.add("End",Double.class);

        // build the type
        final SimpleFeatureType activityFeatureType = builder.buildFeatureType();
        return activityFeatureType;

	}

	private SimpleFeatureType createLegFeatureType() {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Leg");
		try {
			builder.setCRS(CRS.decode("EPSG:32632"));
		} catch (NoSuchAuthorityCodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // <- Coordinate reference system

        // add attributes in order
        builder.add("Leg", LineString.class);
//        builder.add("ActivityNodes", .class);
        builder.add("CarrierId", String.class);
        builder.add("TourId", String.class);
        builder.add("LegNo", String.class);
        builder.add("FromActId", String.class);
        builder.add("ToActId", String.class);
        builder.add("DepTime", String.class);
        builder.add("TravelTime",String.class);

        // build the type
        final SimpleFeatureType legFeatureType = builder.buildFeatureType();
        return legFeatureType;
	}

}
