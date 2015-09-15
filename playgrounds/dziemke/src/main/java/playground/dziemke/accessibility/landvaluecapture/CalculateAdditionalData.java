package playground.dziemke.accessibility.landvaluecapture;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;




public class CalculateAdditionalData {
	// private final Logger log = Logger.getLogger(CalculateAdditionalData.class);

	
	public static void main(String[] args) {
		
		
		String fromCRS = "EPSG:31468"; //GK4
		//String fromCRS2 = "EPSG:3857"; //WGS84PsMerc
		String toCRS = "EPSG:3068"; //DHDN / Soldner Berlin
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(fromCRS, toCRS);
		//CoordinateTransformation ct2 = TransformationFactory.getCoordinateTransformation(fromCRS2, toCRS);
		
		GeometryFactory geometryFactory = new GeometryFactory();
		
		// Input files: gewaesser, parks, centers, population density, BRW (land value)
		String GewaesserInputFile = "/Users/ich/Documents/VSP SVN/landvaluecapture/QGIs/Gewaesser_3068.shp";
		String ParkInputFile ="/Users/ich/Documents/VSP SVN/landvaluecapture/QGIs/Parks_groesser_80000_3068.shp";
		String CenterInputFile ="/Users/ich/Documents/VSP SVN/landvaluecapture/QGIs/Zentren_3068.shp";
		String PopDensityInputFile ="/Users/ich/Documents/VSP SVN/landvaluecapture/QGIs/EWD_HA2014_3068.shp";
		String BRWInputFile ="/Users/ich/Documents/VSP SVN/landvaluecapture/QGIs/BRW_3068.shp";
		
		double minX = 4574000;
		double minY = 5802000;
		double maxX = 4620000;
		double maxY = 5839000;
		double gridSize = 1000;
		
		ActivityFacilitiesImpl measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(minX, minY, maxX, maxY, gridSize);		
		
		// Output csv file
		String csvString [] = new String [measuringPoints.getFacilities().size() +1];
		String csvLocation = "/Users/ich/Documents/VSP SVN/landvaluecapture/QGIs/AdditionalData.csv";

		
		
		// create water facilities
		
		ActivityFacilitiesFactory aff = new ActivityFacilitiesFactoryImpl();
		final ActivityFacilities facilities;
		facilities = FacilitiesUtils.createActivityFacilities("Amenities");

		
		ShapeFileReader shapefileReader = new ShapeFileReader();
		shapefileReader.readFileAndInitialize(GewaesserInputFile);
		Collection<SimpleFeature> waterFeatures = shapefileReader.getFeatureSet();
		
		int counter = 0;
		
		for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values()) {
			
			Coord measuringPointCoord = ct.transform(measuringPoint.getCoord());
			Coordinate measuringPointCoordinate = new Coordinate(measuringPointCoord.getX(), measuringPointCoord.getY());
			Geometry measuringPointAsGeometry = geometryFactory.createPoint(measuringPointCoordinate);
			
			
			for (SimpleFeature waterFeature : waterFeatures) {
				Geometry waterGeometry = (Geometry) waterFeature.getDefaultGeometry();
								
				Double distanceToWater = measuringPointAsGeometry.distance(waterGeometry);
				
				
				if (distanceToWater == 0) {
					
					Id<ActivityFacility> newId = Id.create(counter, ActivityFacility.class);
					ActivityFacility af;
					if(!facilities.getFacilities().containsKey(newId)){
						af = aff.createActivityFacility(newId, measuringPointCoord);
						((ActivityFacilityImpl)af).setDesc(String.valueOf(counter));
						facilities.addActivityFacility(af);
					} else{
						af = (ActivityFacilityImpl) facilities.getFacilities().get(newId);
					}
					ActivityOption ao = aff.createActivityOption("water");
					af.addActivityOption(ao);
										
				}
				
				counter++;
				
			}
						
			FacilitiesWriter fw = new FacilitiesWriter(facilities);
			fw.write("/Users/ich/Documents/VSP SVN/landvaluecapture/QGIs/AdditionalData_WaterFacilities.xml");
			
		}
		
		
		
		// Distance to nearest park
		
		ActivityFacilitiesFactory affPark = new ActivityFacilitiesFactoryImpl();
		final ActivityFacilities ParkFacilities;
		ParkFacilities = FacilitiesUtils.createActivityFacilities("Amenities");

		
		ShapeFileReader shapefileReaderPark = new ShapeFileReader();
		shapefileReaderPark.readFileAndInitialize(ParkInputFile);
		Collection<SimpleFeature> ParkFeatures = shapefileReaderPark.getFeatureSet();
		
		int ParkCounter = 0;
		
		for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values()) {
			
			Coord measuringPointCoord = ct.transform(measuringPoint.getCoord());
			Coordinate measuringPointCoordinate = new Coordinate(measuringPointCoord.getX(), measuringPointCoord.getY());
			Geometry measuringPointAsGeometry = geometryFactory.createPoint(measuringPointCoordinate);
			
			
			for (SimpleFeature ParkFeature : ParkFeatures) {
				Geometry ParkGeometry = (Geometry) ParkFeature.getDefaultGeometry();
								
				Double distanceToPark = measuringPointAsGeometry.distance(ParkGeometry);
				
				
				if (distanceToPark == 0) {
					
					Id<ActivityFacility> newId = Id.create(ParkCounter, ActivityFacility.class);
					ActivityFacility af;
					if(!ParkFacilities.getFacilities().containsKey(newId)){
						af = affPark.createActivityFacility(newId, measuringPointCoord);
						((ActivityFacilityImpl)af).setDesc(String.valueOf(ParkCounter));
						ParkFacilities.addActivityFacility(af);
					} else{
						af = (ActivityFacilityImpl) ParkFacilities.getFacilities().get(newId);
					}
					ActivityOption ao = affPark.createActivityOption("park");
					af.addActivityOption(ao);
										
				}
				
				ParkCounter++;
				
			}
						
			FacilitiesWriter fw = new FacilitiesWriter(ParkFacilities);
			fw.write("/Users/ich/Documents/VSP SVN/landvaluecapture/QGIs/AdditionalData_ParkFacilities.xml");
			
		}		
		
		/*
		
		ShapeFileReader shapefileReaderPark = new ShapeFileReader();
		shapefileReaderPark.readFileAndInitialize(ParkInputFile);
		Collection<SimpleFeature> ParkFeatures = shapefileReaderPark.getFeatureSet();
		
		int numberOfMeasurePointsPark = 0;
		
		for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values()) {
			
			Coord measuringPointCoord = ct.transform(measuringPoint.getCoord());
			Coordinate measuringPointCoordinate = new Coordinate(measuringPointCoord.getX(), measuringPointCoord.getY());
			Geometry measuringPointAsGeometry = geometryFactory.createPoint(measuringPointCoordinate);
			
			Double minDistanceToPark = Double.POSITIVE_INFINITY;
			
			for (SimpleFeature ParkFeature : ParkFeatures) {
				Geometry ParkGeometry = (Geometry) ParkFeature.getDefaultGeometry();
				
				Double distanceToPark = measuringPointAsGeometry.distance(ParkGeometry);
				
				if (distanceToPark < minDistanceToPark) {
					minDistanceToPark = distanceToPark;
				}				
			}
			
			numberOfMeasurePointsPark++;
			System.out.println("distance to park from measuringPoint = " + numberOfMeasurePointsPark + " with coordinate"
					+ measuringPoint.getCoord() + " is = " + minDistanceToPark + "m.");
			
			csvString [numberOfMeasurePointsPark] = csvString [numberOfMeasurePointsPark].concat(String.valueOf(minDistanceToPark) + ", ");
			
		}
		
		*/
		
		
		// Distance to nearest center
		
		ShapeFileReader shapefileReaderCenter = new ShapeFileReader();
		shapefileReaderCenter.readFileAndInitialize(CenterInputFile);
		Collection<SimpleFeature> CenterFeatures = shapefileReaderCenter.getFeatureSet();
		
		int numberOfMeasurePointsCenter = 0;
		
		for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values()) {
			
			Coord measuringPointCoord = ct.transform(measuringPoint.getCoord());
			Coordinate measuringPointCoordinate = new Coordinate(measuringPointCoord.getX(), measuringPointCoord.getY());
			Geometry measuringPointAsGeometry = geometryFactory.createPoint(measuringPointCoordinate);
			
			Double minDistanceToCenter = Double.POSITIVE_INFINITY;
			
			for (SimpleFeature CenterFeature : CenterFeatures) {
				Geometry CenterGeometry = (Geometry) CenterFeature.getDefaultGeometry();
				
				
				Double distanceToCenter = measuringPointAsGeometry.distance(CenterGeometry);
				
				if (distanceToCenter < minDistanceToCenter) {
					minDistanceToCenter = distanceToCenter;
				}				
			}
			
			numberOfMeasurePointsCenter++;
			System.out.println("distance to center from measuringPoint = " + numberOfMeasurePointsCenter + " with coordinate"
					+ measuringPoint.getCoord() + " is = " + minDistanceToCenter + "m.");
			
			csvString [numberOfMeasurePointsCenter] =  String.valueOf(numberOfMeasurePointsCenter) + ", " + measuringPoint.getCoord().getX() + ", " + measuringPoint.getCoord().getY() + ", " + String.valueOf(minDistanceToCenter) + ", ";
			
		}
		
		
		
		// Find attribute of nearest feature on population density layer
		
		ShapeFileReader shapefileReaderPopDensity = new ShapeFileReader();
		shapefileReaderPopDensity.readFileAndInitialize(PopDensityInputFile);
		Collection<SimpleFeature> PopDensityFeatures = shapefileReaderPopDensity.getFeatureSet();
		
		int numberOfMeasurePointsPopDensity = 0;
		
		for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values()) {
			
			Coord measuringPointCoord = ct.transform(measuringPoint.getCoord());
			Coordinate measuringPointCoordinate = new Coordinate(measuringPointCoord.getX(), measuringPointCoord.getY());
			Geometry measuringPointAsGeometry = geometryFactory.createPoint(measuringPointCoordinate);
			
			Double minDistanceToPopDensity = Double.POSITIVE_INFINITY;
			Object minDistanceToPopDensityAttribute = null;
			
			for (SimpleFeature PopDensityFeature : PopDensityFeatures) {
				Geometry PopDensityGeometry = (Geometry) PopDensityFeature.getDefaultGeometry();
				
				
				Double distanceToPopDensity = measuringPointAsGeometry.distance(PopDensityGeometry);
				
				if (distanceToPopDensity <= 300.0 && distanceToPopDensity < minDistanceToPopDensity && !(PopDensityFeature.getAttribute(4).toString()).equals("") && !(PopDensityFeature.getAttribute(4).toString()).equals("0")) {
					minDistanceToPopDensity = distanceToPopDensity;
					minDistanceToPopDensityAttribute = PopDensityFeature.getAttribute(4);
				}				
			}
			
			numberOfMeasurePointsPopDensity++;
			System.out.println("distance to PopDensity layer from measuringPoint = " + numberOfMeasurePointsPopDensity + " with coordinate"
					+ measuringPoint.getCoord() + " is = " + minDistanceToPopDensity + "m." + " PopDensity layer attribute = " + minDistanceToPopDensityAttribute + " inhabitans/HA");
			
			csvString [numberOfMeasurePointsPopDensity] = csvString [numberOfMeasurePointsPopDensity].concat(String.valueOf(minDistanceToPopDensity) + ", " + String.valueOf(minDistanceToPopDensityAttribute) + ", ");
			
		}
		
		
		
		// Find attribute of nearest feature on BRW layer
		
		ShapeFileReader shapefileReaderBRW = new ShapeFileReader();
		shapefileReaderBRW.readFileAndInitialize(BRWInputFile);
		Collection<SimpleFeature> BRWFeatures = shapefileReaderBRW.getFeatureSet();
		
		int numberOfMeasurePointsBRW = 0;
		
		for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values()) {
			
			Coord measuringPointCoord = ct.transform(measuringPoint.getCoord());
			Coordinate measuringPointCoordinate = new Coordinate(measuringPointCoord.getX(), measuringPointCoord.getY());
			Geometry measuringPointAsGeometry = geometryFactory.createPoint(measuringPointCoordinate);
			
			Double minDistanceToBRW = Double.POSITIVE_INFINITY;
			Object minDistanceToBRWAttribute = null;
			
			for (SimpleFeature BRWFeature : BRWFeatures) {
				Geometry BRWGeometry = (Geometry) BRWFeature.getDefaultGeometry();
				
				
				Double distanceToBRW = measuringPointAsGeometry.distance(BRWGeometry);
				
				if (distanceToBRW <= 100.0 && distanceToBRW < minDistanceToBRW && !(BRWFeature.getAttribute(4).toString()).equals("") && !(BRWFeature.getAttribute(7).toString()).equals("0")) {
					minDistanceToBRW = distanceToBRW;
					minDistanceToBRWAttribute = BRWFeature.getAttribute(7);
				}				
			}
			
			numberOfMeasurePointsBRW++;
			System.out.println("distance to BRW layer from measuringPoint = " + numberOfMeasurePointsBRW + " with coordinate"
					+ measuringPoint.getCoord() + " is = " + minDistanceToBRW + "m." + " BRW layer attribute = " + minDistanceToBRWAttribute + " Euro/sqm");
			
			csvString [numberOfMeasurePointsBRW] = csvString [numberOfMeasurePointsBRW].concat(String.valueOf(minDistanceToBRW) + ", " + String.valueOf(minDistanceToBRWAttribute) + "\n");
			}
					
		
		
		// write to csv file		    		
		
			try{
	    		
				File file = new File(csvLocation);
	 
	    		//create file if file does not exists
	    		if(!file.exists()){
	    			file.createNewFile();
	    		}
	 
	    		//append file
	    		FileWriter fileWritter = new FileWriter(file,true);
	    	        BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
	    	        bufferWritter.write("accessibility id, x, y, distance to center, distance to popDensity layer, popDensity, distance to BRW layer, BRW" + "\n");

	    			for (int numberOfMeasurePointsCSV = 1; numberOfMeasurePointsCSV <= measuringPoints.getFacilities().size(); numberOfMeasurePointsCSV++) {

	    	        bufferWritter.write(csvString[numberOfMeasurePointsCSV]);
	    	        
	    			}
	    	        
	    	        bufferWritter.close();	    			    	    
	 
	 
	    	}catch(IOException e){
	    		e.printStackTrace();
	    	}
							
		
	}
}