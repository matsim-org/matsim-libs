package playground.pieter.demandgeneration;
/**code by Gregor Laemmel, Pieter Fourie.
 * Class to generate person home locations from EA shape file with TAZ and SP defined for each poly
 * Reads in 
 * 1) a shapefile of EAs, with attributes FID EA_CODE SP_CODE TAZ
 * 2) a text file contaiing the number of priv veh drivers residding in each SP
 * 3) a text file containing an nxn normalised demand matrix of work trips 
 * 
 * Writes out
 * 1) shapefiles of home and work locations
 * 2) plans.xml file with home departure time ranging between 05h00 and 07h00, work duration 9hrs
 * 
 * Home and work locations are assigned equiprobably to the EAs that compose a TAZ(w) or SP(h)
 * */
import java.io.BufferedReader;
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
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;

import org.matsim.utils.gis.ShapeFileReader;
import org.matsim.utils.gis.ShapeFileWriter;
import org.matsim.utils.io.IOUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class NHTSDemandHWH {

	private static final int TAZ_ATTNUM = 4;//number of TAZ attribute in shapefile
	private static final int SP_CODE_ATTNUM = 3;
	private static final int EA_CODE_ATTNUM = 1;
	private Collection<Feature> polygons; //where polygons from the TAZ/EA/SP shapefile are stored
	private String subPlaceTableFile; // SP | number of persons in SP
	private final HashMap<Integer,Feature> EAHashMap;
	private GeometryFactory geofac; //to create person feature maps
	private FeatureType ftPerson; 
	private CoordinateReferenceSystem coordRefSystem;
	private Random random;
	private Collection<Feature> homeLocationCollection, workLocationCollection;
	private String outputPath;
	private double[][] cumulODMatrix; //where the normalised OD matrix gets stored
	private int matrixDimensions;
	private final int TAZ_OFFSET = 3000; //TODO need a TAZ mapping system so real TAZ numbers map to column and row numbers in matrix
	private final int PERSON_SCALER = 10; //this will create 1/PERSON_SCALER # of persons
	private HashMap<Integer, ArrayList<Feature>> TAZ_EA_hashMap; //used to map work locations to EA
	 //change this to whatever the SP_CODE attribute number is in the shapefile


	public NHTSDemandHWH(Collection<Feature> polygons,
			String subPlaceTableFile, CoordinateReferenceSystem coordinateReferenceSystem,
			String outputPath, String ODMatrixFile) throws FileNotFoundException, IllegalAttributeException{
		this.polygons = polygons;
		this.subPlaceTableFile = subPlaceTableFile;
		this.EAHashMap = new HashMap<Integer, Feature>();
		this.geofac = new GeometryFactory();
		this.coordRefSystem = coordinateReferenceSystem;
		this.random = new Random();
		this.outputPath = outputPath;
		this.TAZ_EA_hashMap = new HashMap<Integer, ArrayList<Feature>>();
		initFeatures();
		loadODMatrix(ODMatrixFile);
		indexMapFeatures();
		createTAZ_EA_hashMap();
	}



	private void createHomeLocations() throws FileNotFoundException, IllegalAttributeException {
		this.homeLocationCollection = new ArrayList<Feature>();
		//reads the input file which lists SP_CODE , number of persons in SP who travel by car
		Scanner inputReader = new Scanner(new File(this.subPlaceTableFile));
		int persId = 0;
		while (inputReader.hasNext()) {
			//reads SP_CODE as string, same as hashmap keys
			int SP_CODE = inputReader.nextInt();
			int numberOfPeopleInSP = inputReader.nextInt()/this.PERSON_SCALER;
			ArrayList<Feature> homeEAs = findHomeEAs((long)SP_CODE);
			if (homeEAs.size()==0) {
				//in case the text file references a SP we dont have in  shapefile
				continue;
			}

			for (int i = 0; i < numberOfPeopleInSP; i++) {
//				find a random home EA in this SP
				Feature homeFeature = homeEAs.get(this.random.nextInt(homeEAs.size()));
//				create point geometry in the polygon
				Point point = getRandomizedCoord((MultiPolygon)homeFeature.getDefaultGeometry());
				double homeTAZ = (Double)homeFeature.getAttribute(TAZ_ATTNUM);
//				create a person feature
//				if(homeTAZ<3000){
//					homeTAZ *=1000;
//				}
				Object [] fta = {point,persId++,SP_CODE,
						(int)homeTAZ, 9999,
						point.getCoordinate().x, point.getCoordinate().y,0,0};
				Feature ft = this.ftPerson.create(fta);
				this.homeLocationCollection.add(ft);
//				this is just a counter
				System.out.print( ((persId%100 == 0)?((persId%2900 == 0)?(persId + "\n"):(persId + " ")):"") );
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
			xmlEntry += String.format("\t\t\t<act type=\"work\" x=\"%f\" y=\"%f\" dur=\"09:00:00\"/>\n",workX,workY );
			xmlEntry += "\t\t\t<leg mode=\"car\"/>\n";
			xmlEntry += String.format("\t\t\t<act type=\"home\" x=\"%f\" y=\"%f\"/>\n",homeX,homeY );
			xmlEntry += "\t\t</plan>\n";
			xmlEntry += "\t</person>\n";
			output.write(xmlEntry);
		}
		output.write("</plans>");
		output.close();
	}
	private void createTAZ_EA_hashMap() {
	//		 first, we need to intialise all the arraylists
			for(int i = 0; i<this.cumulODMatrix.length; i++){
				this.TAZ_EA_hashMap.put(i, new ArrayList<Feature>());
			}
			Iterator<Feature> ftIt = this.EAHashMap.values().iterator();
			while(ftIt.hasNext()){
				Feature currentFt = ftIt.next();
				double doubleTAZ = (Double)currentFt.getAttribute(TAZ_ATTNUM);
				int TAZnumber = (int)doubleTAZ - this.TAZ_OFFSET;
	//			ignore TAZs other than those we use in the OD matrix
				if(TAZnumber < 0 || TAZnumber >= cumulODMatrix.length)
					continue;
				this.TAZ_EA_hashMap.get(TAZnumber).add(currentFt);
			}
		}



	private void createWorkLocations() throws ArrayIndexOutOfBoundsException, IllegalAttributeException {
			//creates a work location for each person in personCollection
			this.workLocationCollection = new ArrayList<Feature>();
			for(Feature person : this.homeLocationCollection) {
				int homeTAZ = (Integer)person.getAttribute(3);
				int workTAZ = getWorkTAZ(homeTAZ-this.TAZ_OFFSET);
				Feature workEA = getWorkEAfromTAZ(workTAZ-this.TAZ_OFFSET);
				Point workPoint = getRandomizedCoord((MultiPolygon)workEA.getDefaultGeometry());
				//update home locations with new info
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



	private ArrayList<Feature> findHomeEAs(long sp_code) {
		ArrayList<Feature> returnList = new ArrayList<Feature>();
		Iterator<Feature> featIter = this.EAHashMap.values().iterator();
		while(featIter.hasNext()){
			Feature ft = featIter.next();
			long ftSPCode = (Long)ft.getAttribute(SP_CODE_ATTNUM);
			if(ftSPCode == sp_code)
				returnList.add(ft);
		}
		return returnList;
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



	private Feature getWorkEAfromTAZ(int workTAZ) {
		int sizeOfEAarrayList = TAZ_EA_hashMap.get(workTAZ).size();
		return TAZ_EA_hashMap.get(workTAZ).get(this.random.nextInt(sizeOfEAarrayList));
	}



	private int getWorkTAZ(int homeTAZ) {
		//generate random number, find the corresponding interval in the cumulative OD matrix
		double randomNumber = this.random.nextDouble();
		if(homeTAZ >=0 && homeTAZ<this.matrixDimensions){
			int workTAZ = -1;
			try{
				do {
					workTAZ++;
				} while (randomNumber > this.cumulODMatrix[homeTAZ][workTAZ]);
				return (workTAZ + this.TAZ_OFFSET);
			}catch(IndexOutOfBoundsException iOBX){
				return (this.random.nextInt(this.matrixDimensions) + this.TAZ_OFFSET);
			}
		}else{ //homeTAZ is undefined, assign any workTAZ
			return (this.random.nextInt(this.matrixDimensions) + this.TAZ_OFFSET);
		}
	}

	private void indexMapFeatures() throws IllegalAttributeException {
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
			long eacode = ((Long)ft.getAttribute(EA_CODE_ATTNUM));
			ft.setDefaultGeometry(multiPoly);
			this.EAHashMap.put((int)eacode, ft);
		}
	}

	private void initFeatures() {
		//define the point collection with its attributes
		final AttributeType[] person = new AttributeType[9];
		person[0] = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, this.coordRefSystem);
		person[1] = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		person[2] = AttributeTypeFactory.newAttributeType("SP_code", Integer.class);
		person[3] = AttributeTypeFactory.newAttributeType("TAZ", Integer.class);
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
		Iterator<Double> listIterator = numbers.iterator();
		for(int row = 0; row < matrixDimensions; row++){
			double rowTotal = 0;
			//create cumulative discrete distribution
			for(int col = 0; col < matrixDimensions; col++){
				rowTotal += (Double)listIterator.next();
				this.cumulODMatrix[row][col] = rowTotal;
			}
		}

	}
	private void run() throws IOException, FactoryException, SchemaException, IllegalAttributeException{
		createHomeLocations();
		ShapeFileWriter.writeGeometries(this.homeLocationCollection, (this.outputPath + "homeLocations.shp"));
		createWorkLocations();
		ShapeFileWriter.writeGeometries(this.workLocationCollection, (this.outputPath + "workLocations.shp"));
		createPlansXML();
		//write output
	}

	public static void main( String[] args ) throws Exception {
		final String inputShape = "southafrica/GP_UTM/TAZ_UTM/GT_NHTS_TAZ.shp"; //NHTS shapefile with EAs, SPs, TAZs
		String inputPersons = "southafrica/initialDemandINPUT/SP_drive_to_work.txt";
		String ODMatrix = "southafrica/initialDemandINPUT/ODMATRIX.txt";
		String outputPath = "southafrica/plans/RPTS/";

		//read in shapefile, create a feature collection
		FeatureSource featSrc = null;
		try {
			featSrc = ShapeFileReader.readDataFile(inputShape);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		final Collection<Feature> inputFeatureCollection = getFeatures(featSrc);

		new NHTSDemandHWH(inputFeatureCollection,inputPersons,featSrc.getSchema().getDefaultGeometry().getCoordinateSystem(),outputPath, ODMatrix).run();

		System.out.printf("Done! Point shapefile output to %s", outputPath);
	}
//	end Class
}
