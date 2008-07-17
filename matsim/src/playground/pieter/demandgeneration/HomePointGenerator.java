package playground.pieter.demandgeneration;
/**code by Gregor Laemmel. Class to generate person home locations from SP tables and SP shapefile */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;

import org.matsim.utils.gis.ShapeFileReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.gis.utils.ShapeFileWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class HomePointGenerator {
	
	private Collection<Feature> polygons;
	private String inputPersons;
	private HashMap<String,MultiPolygon> polygonHashMap;
	private GeometryFactory geofac;
	private FeatureType ftHome;
	private CoordinateReferenceSystem coordRefSystem;
	private Random random;
	private Collection<Feature> pointCollection;
	private String outputShape;
	private TAZContainer tazContainer;
	

	public HomePointGenerator(Collection<Feature> polygons, String inputPersons, CoordinateReferenceSystem coordinateReferenceSystem, String outputShape){
		this.polygons = polygons;
		this.inputPersons = inputPersons;
		this.polygonHashMap = new HashMap<String, MultiPolygon>();
		this.geofac = new GeometryFactory();
		this.coordRefSystem = coordinateReferenceSystem;
		this.random = new Random(0);
		this.outputShape = outputShape;
		initFeatures();
	}
	//overloaded in case you want to find TAZ
	public HomePointGenerator(Collection<Feature> polygons, String inputPersons, CoordinateReferenceSystem coordinateReferenceSystem, String outputShape, String TAZShape){
		this.polygons = polygons;
		this.inputPersons = inputPersons;
		this.polygonHashMap = new HashMap<String, MultiPolygon>();
		this.geofac = new GeometryFactory();
		this.coordRefSystem = coordinateReferenceSystem;
		this.random = new Random(0);
		this.outputShape = outputShape;
		initFeatures();
		this.tazContainer = new TAZContainer(TAZShape);
	}
	
		
	
	private void initFeatures() {
		//define the point collection with its attributes
		final AttributeType[] homes = new AttributeType[4];
		homes[0] = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, this.coordRefSystem);
		homes[1] = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		homes[2] = AttributeTypeFactory.newAttributeType("SP_code", Integer.class);
		homes[3] = AttributeTypeFactory.newAttributeType("homeTAZ", Integer.class);

		try {
			this.ftHome = FeatureTypeFactory.newFeatureType(homes, "home");
//						
		} catch (final FactoryRegistryException e) {
			e.printStackTrace();
		} catch (final SchemaException e) {
			e.printStackTrace();
		}
	}
	
	private void run() throws IllegalAttributeException, IOException, FactoryException, SchemaException {
		
		indexPolygons();
		this.pointCollection = new ArrayList<Feature>();
		//reads the input file which lists SP_CODE , number of persons in SP who travel by car
		Scanner inputReader = new Scanner(new File(this.inputPersons));
		int persId = 0;
		boolean checkForTAZ = !this.tazContainer.equals(null);
		while (inputReader.hasNext()) {
			//reads SP_CODE as string, same as hashmap keys
			String SP_CODE = inputReader.next();
			int numberOfPeopleInSP = inputReader.nextInt();
			MultiPolygon multiPoly = this.polygonHashMap.get(SP_CODE);
			if (multiPoly == null) {
				//in case the text file references a SP we dont have in  shapefile
				continue;
			}
			
			for (int i = 0; i < numberOfPeopleInSP; i++) {
				//create point geometry in the polygon
				Point point = getRandomizedCoord(multiPoly);
				String homeTAZ = "9999";
				if(checkForTAZ){
					homeTAZ = tazContainer.findContainerID(point);
				}
				Object [] fta = {point,persId++,Integer.parseInt(SP_CODE),Integer.parseInt(homeTAZ)};
				System.out.print( ((persId%100 == 0)?((persId%2900 == 0)?(persId + "\n"):(persId + " ")):"") );
				Feature ft = this.ftHome.create(fta);
				this.pointCollection.add(ft);
			}
		}
		ShapeFileWriter.writeGeometries(this.pointCollection, this.outputShape);
	}
	
	private Point getRandomizedCoord(MultiPolygon mp) {
		Polygon bounds = (Polygon)mp.getEnvelope();
		//get the opposing corner coords from the bounding box
		Coordinate min = bounds.getExteriorRing().getCoordinateN(0); //bottom left corner
		Coordinate max = bounds.getExteriorRing().getCoordinateN(2); //top right corner
		double dMaxX = max.x - min.x;
		double dMaxY = max.y - min.y;
		Point p = null;
		do {

			double offsetX = this.random.nextDouble() * dMaxX;
			double offsetY = this.random.nextDouble() * dMaxY;		
			p = this.geofac.createPoint(new Coordinate(min.x+offsetX,min.y+offsetY));

		} while (!mp.contains(p)); //keep doing this until the point falls inside the multipoly

		return p; 
	}

	private void indexPolygons() {
		//goes through the collection of polygons
		for (Feature ft : this.polygons){
			Geometry geo = ft.getDefaultGeometry();
			//converts geometry to Multipolygon, if not already
			MultiPolygon multiPoly = null;
			if ( geo instanceof MultiPolygon ) {
				multiPoly = (MultiPolygon) geo;
			} else if (geo instanceof Polygon ) {
				multiPoly = this.geofac.createMultiPolygon(new Polygon[] {(Polygon) geo});
			} else {
				throw new RuntimeException("Feature does not contain a polygon/multipolygon!");
			}
			String SP_CODE = ((Long)ft.getAttribute(2)).toString();
			this.polygonHashMap.put(SP_CODE, multiPoly);
		}
	}

	public static Collection<Feature> getFeatures(final FeatureSource n) {
		
		final Collection<Feature> featColl = new ArrayList<Feature>();
		org.geotools.feature.FeatureIterator ftIterator = null;
		try {
			ftIterator = n.getFeatures().features();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		//add features to arraylist collection
		while (ftIterator.hasNext()) {
			final Feature feature = ftIterator.next();
			featColl.add(feature);
		}
		return featColl;
	}


	public static void main( String[] args ) throws Exception {
		final String inputShape = "./southafrica/SA_UTM/SP_UTM.shp";
		String inputPersons = "./southafrica/SP_drive_to_work.csv";
		String outputShape = "./southafrica/homelocationsWithTAZ.shp";
		String TAZShape = "./southafrica/SA_UTM/MP_UTM.shp";

		//read in shapefile, create a feature collection
		FeatureSource featSrc = null;
		try {
			featSrc = ShapeFileReader.readDataFile(inputShape);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		final Collection<Feature> inputFeatureCollection = getFeatures(featSrc);
		
		new HomePointGenerator(inputFeatureCollection,inputPersons,featSrc.getSchema().getDefaultGeometry().getCoordinateSystem(),outputShape, TAZShape).run();
		System.out.printf("Done! Point shapefile output to %s", outputShape);
	}
//end Class
}
