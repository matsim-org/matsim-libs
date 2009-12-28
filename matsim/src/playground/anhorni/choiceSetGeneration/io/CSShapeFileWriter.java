package playground.anhorni.choiceSetGeneration.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Point;
import playground.anhorni.choiceSetGeneration.helper.ChoiceSet;
import playground.anhorni.choiceSetGeneration.helper.ChoiceSetFacility;


public class CSShapeFileWriter extends CSWriter {

	private final static Logger log = Logger.getLogger(CSShapeFileWriter.class);
	
	private FeatureType featureType;
	
	public CSShapeFileWriter() {	
	}

		
	@Override
	public void write(String outdir, String name, List<ChoiceSet> choiceSets)  {
		this.writeTrips(outdir, name, choiceSets);
		if (!super.checkBeforeWriting(choiceSets)) {
			log.warn("No trip shape files created");
			return;
		}				
		this.writeChoiceSets(outdir, name, choiceSets);		
	}
		
	public void writeChoiceSets(String outdir, String name, List<ChoiceSet> choiceSets) {
				
		this.initGeometries();
		ArrayList<Feature> features = new ArrayList<Feature>();	
		
		Iterator<ChoiceSet> choiceSet_it = choiceSets.iterator();
		while (choiceSet_it.hasNext()) {
			ChoiceSet choiceSet = choiceSet_it.next();
			
			ArrayList<Feature> singleFeatures = new ArrayList<Feature>();
			Iterator<ChoiceSetFacility> choiceSetFacilities_it = choiceSet.getFacilities().values().iterator();
			while (choiceSetFacilities_it.hasNext()) {
				ChoiceSetFacility choiceSetFacility = choiceSetFacilities_it.next();
				Coord coord = new CoordImpl(choiceSetFacility.getFacility().getMappedPosition().getX(), 
						choiceSetFacility.getFacility().getMappedPosition().getY());
				
				Feature feature = this.createFeature(coord, choiceSet.getId());
				features.add(feature);
				singleFeatures.add(feature);
			}
			try {
				if (!singleFeatures.isEmpty()) {
					ShapeFileWriter.writeGeometries((Collection<Feature>)singleFeatures, outdir +"/shapefiles/singlechoicesets/" + 
						name + choiceSet.getId()+ "_choiceSet.shp");
				}
				else {
					log.error("Empty choice set: " + choiceSet.getId());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		try {
			if (!features.isEmpty()) {
				ShapeFileWriter.writeGeometries((Collection<Feature>)features, outdir +"/shapefiles/" + name + "_choiceSets.shp");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeTrips(String outdir, String name, List<ChoiceSet> choiceSets) {
	
		this.initGeometries();
		ArrayList<Feature> featuresBefore = new ArrayList<Feature>();
		ArrayList<Feature> featuresShop = new ArrayList<Feature>();
		ArrayList<Feature> featuresAfter = new ArrayList<Feature>();
		
		Iterator<ChoiceSet> choiceSets_it = choiceSets.iterator();
		while (choiceSets_it.hasNext()) {
			ChoiceSet choiceSet = choiceSets_it.next();
			
			ArrayList<Feature> singleFeatures = new ArrayList<Feature>();
			
			Coord coordBefore = new CoordImpl(choiceSet.getTrip().getBeforeShoppingAct().getCoord().getX(), 
					choiceSet.getTrip().getBeforeShoppingAct().getCoord().getY());
			
			Feature featureBefore = this.createFeature(coordBefore, choiceSet.getId());
			featuresBefore.add(featureBefore);
			singleFeatures.add(featureBefore);
			
			Coord coordShopping = new CoordImpl(choiceSet.getTrip().getShoppingAct().getCoord().getX(), 
					choiceSet.getTrip().getShoppingAct().getCoord().getY());
			
			Feature featureShopping = this.createFeature(coordShopping, choiceSet.getId());
			featuresShop.add(featureShopping);
			singleFeatures.add(featureShopping);
			
			Coord coordAfter = new CoordImpl(choiceSet.getTrip().getAfterShoppingAct().getCoord().getX(), 
					choiceSet.getTrip().getAfterShoppingAct().getCoord().getY());
			
			Feature featureAfter = this.createFeature(coordAfter, choiceSet.getId());
			featuresAfter.add(featureAfter);
			singleFeatures.add(featureAfter);
			
			try {
				if (!singleFeatures.isEmpty()) {
					ShapeFileWriter.writeGeometries((Collection<Feature>)singleFeatures, outdir +"/shapefiles/singletrips/" + name + 
						choiceSet.getId()+"_Trip.shp");
				}
				else {
					log.error("Empty trip : " + choiceSet.getId());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}		
		}			
		try {
			if (!featuresBefore.isEmpty()) {
				ShapeFileWriter.writeGeometries((Collection<Feature>)featuresBefore, outdir +"/shapefiles/" + name + "_TripPriorLocations.shp");
			}
			if (!featuresShop.isEmpty()) {
				ShapeFileWriter.writeGeometries((Collection<Feature>)featuresShop, outdir +"/shapefiles/" + name + "_TripShopLocations.shp");
			}
			if (!featuresAfter.isEmpty()) {
				ShapeFileWriter.writeGeometries((Collection<Feature>)featuresAfter, outdir +"/shapefiles/" + name + "_TripPosteriorLocations.shp");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void initGeometries() {
		AttributeType [] attr = new AttributeType[2];
		attr[0] = AttributeTypeFactory.newAttributeType("Point", Point.class);
		attr[1] = AttributeTypeFactory.newAttributeType("ID", String.class);
		
		try {
			this.featureType = FeatureTypeBuilder.newFeatureType(attr, "point");
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}
	
	private Feature createFeature(Coord coord, Id id) {
		
		Feature feature = null;
		
		try {
			feature = this.featureType.create(new Object [] {MGC.coord2Point(coord), id.toString()});
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}
		return feature;
	}
	
	
	/*
	private List<Geometry> createGeometryCollection(ChoiceSet choiceSet, GeometryFactory geometryFactory) {
				
		List<Geometry> geometryList = new Vector<Geometry>();
								
		Coordinate coord = new Coordinate(choiceSet.getTrip().getBeforeShoppingAct().getCoord().getX(), 
				choiceSet.getTrip().getBeforeShoppingAct().getCoord().getY());
		Point point = geometryFactory.createPoint(coord);
		geometryList.add(point);
				
		Coordinate coordShopping = new Coordinate(choiceSet.getTrip().getShoppingAct().getCoord().getX(), 
				choiceSet.getTrip().getShoppingAct().getCoord().getY());
		Point pointShopping = geometryFactory.createPoint(coordShopping);
		geometryList.add(pointShopping);
		
		return geometryList;
	}
	
	
	public void write(String outdir, String name, List<ChoiceSet> choiceSets)  {
		
		if (!super.checkBeforeWriting(choiceSets)) {
			log.warn("No trip shape files created");
			return;
		}
		
		List<Geometry> geometryListAll = new Vector<Geometry>();
		GeometryFactory geometryFactory = new GeometryFactory();
		
		RandomAccessFile shpAll = null;
		RandomAccessFile shxAll = null;
		try {
			shpAll = new RandomAccessFile(
					new File(outdir + "trip.shp"), "rw");
			shxAll = new RandomAccessFile(
					new File(outdir + "trip.shx"), "rw"); 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
				
		Iterator<ChoiceSet> choiceSets_it = choiceSets.iterator();
		while (choiceSets_it.hasNext()) {
			ChoiceSet choiceSet = choiceSets_it.next();
							
			RandomAccessFile shp = null;
			RandomAccessFile shx = null;
			try {
				shp = new RandomAccessFile(
						new File(outdir + "/persontrips/" + "person" + choiceSet.getPersonId()+".trip.shp"), "rw");
				shx = new RandomAccessFile(
						new File(outdir + "/persontrips/" + "person" + choiceSet.getPersonId()+".trip.shx"), "rw"); 
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} 
		
			List<Geometry> geometryList = this.createGeometryCollection(choiceSet, geometryFactory);
			geometryListAll.addAll(geometryList);			
			
			GeometryCollection geometryCollection = new GeometryCollection(
					geometryList.toArray(new Geometry[geometryList.size()]), geometryFactory);
			
			ShapefileWriter writer;
			try {
				writer = new ShapefileWriter(shp.getChannel(),shx.getChannel(), new Lock());
				writer.write(geometryCollection, ShapeType.POINT);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		GeometryCollection geometryCollectionAll = new GeometryCollection(
				geometryListAll.toArray(new Geometry[geometryListAll.size()]), geometryFactory);
		
		ShapefileWriter allWriter;
		try {
			allWriter = new ShapefileWriter(shpAll.getChannel(),shxAll.getChannel(), new Lock());
			allWriter.write(geometryCollectionAll, ShapeType.POINT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	*/

}
