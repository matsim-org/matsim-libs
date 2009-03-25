package playground.pieter.demandgeneration;
/**code by Gregor Laemmel, Pieter Fourie.
 * Class to generate person home locations from SP tables and SP shapefile */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class SAInitialDemandGenerator {

	private Collection<Feature> polygons;
	private String inputPersons;
	private HashMap<String,MultiPolygon> homeLocHashMap, tazHashMap;
	private GeometryFactory geofac;
	private FeatureType ftPerson;
	private CoordinateReferenceSystem coordRefSystem;
	private Random random;
	private Collection<Feature> homeLocationCollection, workLocationCollection;
	private String outputPath;
	//private String plansXML;
	private TAZContainer tazCont;
	private boolean checkForTAZ;
	private double[][] cumulODMatrix;
	private int matrixDimensions;
	private final int TAZ_OFFSET = 3000;
	private final int PERSON_SCALER = 1; //this will create 1/PERSON_SCALER # of persons

	public SAInitialDemandGenerator(){

		this.random = new Random(12);

	}

	public SAInitialDemandGenerator(Collection<Feature> polygons, String inputPersons, CoordinateReferenceSystem coordinateReferenceSystem, String outputShape){
		this.polygons = polygons;
		this.inputPersons = inputPersons;
		this.homeLocHashMap = new HashMap<String, MultiPolygon>();
		this.geofac = new GeometryFactory();
		this.coordRefSystem = coordinateReferenceSystem;
		this.random = new Random(12);
		this.outputPath = outputShape;
		this.checkForTAZ = false;
		initFeatures();
	}
	//overloaded in case you want to find TAZ
	public SAInitialDemandGenerator(Collection<Feature> polygons,
			String inputPersons, CoordinateReferenceSystem coordinateReferenceSystem,
			String outputPath, String TAZShape, String ODMatrixFile) throws FileNotFoundException{
		this.polygons = polygons;
		this.inputPersons = inputPersons;
		this.homeLocHashMap = new HashMap<String, MultiPolygon>();
		this.geofac = new GeometryFactory();
		this.coordRefSystem = coordinateReferenceSystem;
		this.random = new Random(0);
		this.outputPath = outputPath;
		initFeatures();
		this.tazCont = new TAZContainer(TAZShape);
		this.checkForTAZ = true;
		this.tazHashMap = tazCont.getTAZHAshMap();
		loadODMatrix(ODMatrixFile);
	}



	private void createWorkLocations() throws ArrayIndexOutOfBoundsException, IllegalAttributeException {
		//creates a worklocation for each person in personCollection
		this.workLocationCollection = new ArrayList<Feature>();
		for(Feature person : this.homeLocationCollection) {
			int homeTAZ = ((Integer) person.getAttribute(3));
			int workTAZ = getWorkTAZ(homeTAZ-this.TAZ_OFFSET);
			MultiPolygon workPoly = this.tazHashMap.get(Integer.toString(workTAZ));
//			System.out.println("Looking for workTAZ number " + workTAZ);
			Point workPoint = getRandomizedCoord(workPoly);
			//update home locatins with new info
			person.setAttribute(4, workTAZ);
			person.setAttribute(7, workPoint.getCoordinate().x);
			person.setAttribute(8, workPoint.getCoordinate().y);
			//create work location point
			Object [] workFeature = {workPoint,person.getAttribute(1), person.getAttribute(2),
					homeTAZ, workTAZ,
					person.getAttribute(5), person.getAttribute(6),
					workPoint.getCoordinate().x, workPoint.getCoordinate().y};
			int persId = (Integer)person.getAttribute(1);
			System.out.print( ((persId%100 == 0)?((persId%2900 == 0)?(persId + "\n"):(persId + " ")):"") );
//			System.out.print( ((Integer(person.getAttribute(1)%100 == 0)?((person.getAttribute(1)%2900 == 0)?(person.getAttribute(1) + "\n"):(person.getAttribute(1) + " ")):"") );
			Feature personFeature = this.ftPerson.create(workFeature);
			this.workLocationCollection.add(personFeature);

		}
	}
	private void createHomeLocations() throws FileNotFoundException, IllegalAttributeException {
		indexPolygons();
		this.homeLocationCollection = new ArrayList<Feature>();
		//reads the input file which lists SP_CODE , number of persons in SP who travel by car
		Scanner inputReader = new Scanner(new File(this.inputPersons));
		int persId = 0;
		while (inputReader.hasNext()) {
			//reads SP_CODE as string, same as hashmap keys
			String SP_CODE = inputReader.next();
			int numberOfPeopleInSP = inputReader.nextInt()/this.PERSON_SCALER;
			MultiPolygon homePoly = this.homeLocHashMap.get(SP_CODE);
			if (homePoly == null) {
				//in case the text file references a SP we dont have in  shapefile
				continue;
			}

			for (int i = 0; i < numberOfPeopleInSP; i++) {
				//create point geometry in the polygon
				Point point = getRandomizedCoord(homePoly);
				String homeTAZ = "3099";
				if(this.checkForTAZ){
					homeTAZ = tazCont.findContainerID(point);
				}
				Object [] fta = {point,persId++,Integer.parseInt(SP_CODE),
						Integer.parseInt(homeTAZ), 9999,
						point.getCoordinate().x, point.getCoordinate().y,0,0};
				System.out.print( ((persId%100 == 0)?((persId%2900 == 0)?(persId + "\n"):(persId + " ")):"") );
				Feature ft = this.ftPerson.create(fta);
				this.homeLocationCollection.add(ft);
			}
		}

	}
	private void createPlansXML() throws FileNotFoundException, IOException {
		BufferedWriter output = IOUtils.getBufferedWriter((this.outputPath+"plans.xml.gz"));
		output.write("<?xml version=\"1.0\" ?>\n");
		output.write("<!DOCTYPE plans SYSTEM \"http://www.matsim.org/files/dtd/plans_v4.dtd\">\n");
		output.write("<plans>\n");
		for (Feature person : this.workLocationCollection){
			int ID = (Integer)person.getAttribute(1);
			double homeX = (Double)person.getAttribute(5);
			double homeY = (Double)person.getAttribute(6);
			double workX = (Double)person.getAttribute(7);
			double workY = (Double)person.getAttribute(8);
			String xmlEntry = String.format("\t<person id = \"%d\">\n",ID);
			xmlEntry += "\t\t<plan>\n";
			String endTime = getRandomHomeDepartureTime();
			xmlEntry += String.format("\t\t\t<act type=\"home\" x=\"%f\" y=\"%f\" end_time=\"%s\"/>\n",homeX,homeY,endTime  );
			xmlEntry += "\t\t\t<leg mode=\"car\"/>\n";
			xmlEntry += String.format("\t\t\t<act type=\"work\" x=\"%f\" y=\"%f\" dur=\"08:00:00\"/>\n",workX,workY );
			xmlEntry += "\t\t\t<leg mode=\"car\"/>\n";
			xmlEntry += String.format("\t\t\t<act type=\"home\" x=\"%f\" y=\"%f\"/>\n",homeX,homeY );
			xmlEntry += "\t\t</plan>\n";
			xmlEntry += "\t</person>\n";
			output.write(xmlEntry);
		}
		output.write("</plans>");
		output.close();
	}
	public String getRandomHomeDepartureTime() {
		int hour = random.nextInt(2) +5;
		int minute = random.nextInt(60);
		return String.format("%02d:%02d:00",hour,minute);
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
	private int getWorkTAZ(int homeTAZ) {
		//generate random number, find the corresponding interval in the cumulative OD matrix
		double randomNumber = this.random.nextDouble();
		if(homeTAZ != 99){
			int workTAZ = -1;
			do {
				workTAZ++;
			} while ((randomNumber > this.cumulODMatrix[homeTAZ][workTAZ])
					&& (workTAZ < this.matrixDimensions));
			return (workTAZ + this.TAZ_OFFSET);
		}else{ //homeTAZ is undefined, assign any workTAZ
			return (this.random.nextInt(this.matrixDimensions) + this.TAZ_OFFSET);
		}
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
			this.homeLocHashMap.put(SP_CODE, multiPoly);
		}
	}

	private void initFeatures() {
		//define the point collection with its attributes
		final AttributeType[] person = new AttributeType[9];
		person[0] = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, this.coordRefSystem);
		person[1] = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		person[2] = AttributeTypeFactory.newAttributeType("SP_code", Integer.class);
		person[3] = AttributeTypeFactory.newAttributeType("homeTAZ", Integer.class);
		person[4] = AttributeTypeFactory.newAttributeType("workTAZ", Integer.class);
		person[5] = AttributeTypeFactory.newAttributeType("homeX", Double.class);
		person[6] = AttributeTypeFactory.newAttributeType("homeY", Double.class);
		person[7] = AttributeTypeFactory.newAttributeType("workX", Double.class);
		person[8] = AttributeTypeFactory.newAttributeType("workY", Double.class);

		try {
			this.ftPerson = FeatureTypeFactory.newFeatureType(person, "person");
			//
		} catch (final FactoryRegistryException e) {
			e.printStackTrace();
		} catch (final SchemaException e) {
			e.printStackTrace();
		}
	}
	private void loadODMatrix(String matrixFile) throws FileNotFoundException {
		Scanner ODMAtrixReader = new Scanner(new File(matrixFile));
		List<Double> numbers = new ArrayList<Double>(0);
		while (ODMAtrixReader.hasNext()){
			numbers.add(ODMAtrixReader.nextDouble());
		}
		this.matrixDimensions = (int)Math.sqrt(numbers.size());
		this.cumulODMatrix = new double[matrixDimensions][matrixDimensions];
		Iterator listIterator = numbers.iterator();
		for(int row = 0; row < matrixDimensions; row++){
			double rowTotal = 0;
			//create cumulative discrete distribution
			for(int col = 0; col < matrixDimensions; col++){
				rowTotal += (Double)listIterator.next();
				this.cumulODMatrix[row][col] = rowTotal;
			}
		}

	}
	private void run() throws IOException, IllegalAttributeException{
		createHomeLocations();
		ShapeFileWriter.writeGeometries(this.homeLocationCollection, (this.outputPath + "homeLocations.shp"));
		createWorkLocations();
		ShapeFileWriter.writeGeometries(this.workLocationCollection, (this.outputPath + "workLocations.shp"));
		createPlansXML();
		//write output
	}

	public static void main( String[] args ) throws Exception {
		final String inputShape = "./southafrica/GP_UTM/SP_UTM.shp";
		String inputPersons = "./southafrica/SP_drive_to_work.csv";
		String outputPath = "./southafrica/initialDemand/";
		String TAZShape = "./southafrica/TAZ/rough_TAZs.shp";
		String ODMatrix = "./southafrica/ODMATRIX.txt";

		//read in shapefile, create a feature collection
		FeatureSource featSrc = null;
		try {
			featSrc = ShapeFileReader.readDataFile(inputShape);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		final Collection<Feature> inputFeatureCollection = getFeatures(featSrc);
		//use this if you have a taz shapefile
		new SAInitialDemandGenerator(inputFeatureCollection,inputPersons,featSrc.getSchema().getDefaultGeometry().getCoordinateSystem(),outputPath, TAZShape, ODMatrix).run();
		//and this if not
		//new SAInitialDemandGenerator(inputFeatureCollection,inputPersons,featSrc.getSchema().getDefaultGeometry().getCoordinateSystem(),outputShape).run();
		System.out.printf("Done! Point shapefile output to %s", outputPath);
	}
//	end Class
}
