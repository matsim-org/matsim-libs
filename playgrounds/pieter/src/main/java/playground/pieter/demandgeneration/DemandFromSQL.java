package playground.pieter.demandgeneration;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;

import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class DemandFromSQL {
	/**
	 * Class to generate person home locations from EA shape file with TAZ and SP defined for each poly
	 * Reads in
	 * 1) a shapefile of EAs, with attributes FID EA_CODE SP_CODE TAZ
	 * 2) a text file containing the number of priv veh drivers residing in each SP
	 * 3) a SQL table of htaz, wtaz and number of trips between each combo
	 *
	 * Writes out
	 * 1) shapefiles of home and work locations
	 * 2) plans.xml file with home departure time ranging between 05h00 and 07h00, work duration 9hrs
	 *
	 * Home and work locations are assigned equiprobably to the EAs that compose a TAZ(w) or SP(h)
	 * */

	private static final int NHTSTAZ_ATTNUM = 5;//number of TAZ attribute in shapefile
	private static final int SP_CODE_ATTNUM = 3;
	private static final int EA_CODE_ATTNUM = 6;
	private Collection<Feature> polygons; //where polygons from the TAZ/EA/SP shapefile are stored
	private String subPlaceTableFile; // SP | number of persons in SP
	private CoordinateReferenceSystem coordRefSystem;
	private String outputPath;
	private final HashMap<Integer,Feature> EAHashMap;
	private final HashMap<Integer, ArrayList<Feature>> TAZ_EA_hashMap; //used to map work locations to EA
	private GeometryFactory geofac; //to create person feature maps
	private FeatureType ftPerson;
	private Random random;
	private Collection<Feature> homeLocationCollection, workLocationCollection;
	private final int PERSON_SCALER = 1; //this will create 1/PERSON_SCALER # of persons
	private HashMatrix demand;


	public DemandFromSQL(Collection<Feature> polys,
			String spTabFile, CoordinateReferenceSystem cRS,
			String outPath, String sqlServer, String sqlUser, String sqlPassword, String sqlTable,
			String fromField, String toField, String valueField) throws Exception{
		this.polygons = polys;
		this.subPlaceTableFile = spTabFile;
		this.EAHashMap = new HashMap<Integer, Feature>();
		this.geofac = new GeometryFactory();
		this.coordRefSystem = cRS;
		this.random = new Random();
		this.outputPath = outPath;
		this.TAZ_EA_hashMap = new HashMap<Integer, ArrayList<Feature>>();
		this.demand = new HashMatrix(sqlServer, sqlUser, sqlPassword, sqlTable,
				fromField, toField, valueField);
		initFeatures();
		indexMapFeatures();
		createTAZ_EA_hashMap();
	}


	private void initFeatures() {
		//define the point collection with its attributes
		final AttributeType[] person = new AttributeType[9];
		person[0] = AttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, this.coordRefSystem);
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
//			long eacode = ((Long)ft.getAttribute(EA_CODE_ATTNUM));
			int eacode = ((Integer)ft.getAttribute(EA_CODE_ATTNUM));
			ft.setDefaultGeometry(multiPoly);
			this.EAHashMap.put(eacode, ft);
		}
	}


	private void createTAZ_EA_hashMap() {
		//		 first, we need to intialise all the arraylists of EAs associated with each TAZ
		Iterator<Integer> headers = this.demand.getHeaderSet().iterator(); //returns list of headers in demand matrix

		while(headers.hasNext()){
			this.TAZ_EA_hashMap.put(headers.next(), new ArrayList<Feature>());
		}
		Iterator<Feature> ftIt = this.EAHashMap.values().iterator();
		while(ftIt.hasNext()){
			Feature currentFt = ftIt.next();
			int TAZNumber = (int)(long)(Long)currentFt.getAttribute(NHTSTAZ_ATTNUM);
			//			ignore TAZs other than those we use in the OD matrix
			try{
				this.TAZ_EA_hashMap.get(TAZNumber).add(currentFt);
			}catch(NullPointerException nullPoint){
				System.err.printf("Shapefile refers to TAZ number %d which is not in OD matrix. Ignoring it.\n", TAZNumber);
			}
		}
	}


	private void createHomeLocations() throws Exception {
		this.homeLocationCollection = new ArrayList<Feature>();
		//reads the input file which lists SP_CODE , number of persons in SP who travel by car
		Scanner inputReader = new Scanner(new File(this.subPlaceTableFile));
		int persId = 0;
		//		while(inputReader.hasNext()){
		//			System.out.println(inputReader.next());
		while (inputReader.hasNext()) {
			//reads SP_CODE as string, same as hashmap keys
			int SP_CODE = inputReader.nextInt();
			int numberOfPeopleInSP = inputReader.nextInt()/this.PERSON_SCALER;
			ArrayList<Feature> homeEAs = findHomeEAs(SP_CODE);
			if (homeEAs.size()==0) {
				//in case the text file references a SP we dont have in  shapefile
				System.err.printf("%s references SP_CODE %d which isn't in shapefile.",this.subPlaceTableFile, SP_CODE);
				continue;
			}

			for (int i = 0; i < numberOfPeopleInSP; i++) {
				//				find a random home EA in this SP
				Feature homeFeature = homeEAs.get(this.random.nextInt(homeEAs.size()));
				//				create point geometry in the polygon
				Point point = getRandomizedCoord((MultiPolygon)homeFeature.getDefaultGeometry());
				long homeTAZ = (Long)homeFeature.getAttribute(NHTSTAZ_ATTNUM);

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


	private void createWorkLocations() throws ArrayIndexOutOfBoundsException, IllegalAttributeException {
		//creates a work location for each person in personCollection
		this.workLocationCollection = new ArrayList<Feature>();
		for(Feature person : this.homeLocationCollection) {
			int homeTAZ = (Integer)person.getAttribute(3);
			int workTAZ = getWorkTAZ(homeTAZ);
			int currentPersonId = (Integer)person.getAttribute(1);
//			StringBuilder tazId = new StringBuilder(String.valueOf(homeTAZ));
//			tazId.append(workTAZ);
//			String bufferedPersonId = String.format("%08d", currentPersonId);
//			tazId.append(bufferedPersonId);
			Feature workEA = getWorkEAfromTAZ(workTAZ);
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


	private int getWorkTAZ(int homeTAZ) {
		//generate random number, find the corresponding interval in the cumulative OD matrix
		double randomNumber = this.random.nextDouble();
		Set<Entry<Integer,Double>> rowProbs = this.demand.getRowProbabilitySet(homeTAZ);
		double threshold =0.0;
		Iterator<Entry<Integer,Double>> rowEntries = rowProbs.iterator();
		while(randomNumber > threshold){
			Entry<Integer,Double> currEntry = rowEntries.next();
			threshold += currEntry.getValue();
			if(randomNumber <= threshold){
				return currEntry.getKey();
			}
		}
		return 0;
	}


	private Feature getWorkEAfromTAZ(int workTAZ) {
		int sizeOfEAarrayList = TAZ_EA_hashMap.get(workTAZ).size();
		if(sizeOfEAarrayList<=0){
			System.out.print("  worktaz:"+workTAZ+ " size odf array = "+sizeOfEAarrayList );
			System.err.println("holy crap!");
		}
		return TAZ_EA_hashMap.get(workTAZ).get(this.random.nextInt(sizeOfEAarrayList));
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
	public String getRandomHomeDepartureTime() {
		int hour = random.nextInt(2) +6;
		int minute = random.nextInt(60);
		return String.format("%02d:%02d:00",hour,minute);
	}

	private void run() throws Exception{
		createHomeLocations();
//		ShapeFileWriter.writeGeometries(this.homeLocationCollection, (this.outputPath + "homeLocations.shp"));
		createWorkLocations();
//		ShapeFileWriter.writeGeometries(this.workLocationCollection, (this.outputPath + "workLocations.shp"));
		createPlansXML();
		//write output
	}


	public static void main( String[] args ) throws Exception {
		final String inputShape = "southafrica/IPDM_ETH_EmmeMOD/GIS/union50meter.shp"; //NHTS shapefile with EAs, SPs, TAZs
		String inputPersons = "southafrica/IPDM_ETH_EmmeMOD/SP_drive2workETH.txt";
		String outputPath = "southafrica/IPDM_ETH_EmmeMOD/smallplans/";
		String sqlServer = "jdbc:mysql://localhost/temp", sqlUser = "pfourie", sqlPass = "koos", sqlTable = "valid_eta_demand";
		String sqlFromField = "htaz", sqlToField = "wtaz", sqlValueField = "trips";
		//read in shapefile, create a feature collection
		FeatureSource featSrc = null;
		try {
			featSrc = ShapeFileReader.readDataFile(inputShape);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		final Collection<Feature> inFC = getFeatures(featSrc);

		CoordinateReferenceSystem cRS = featSrc.getSchema().getDefaultGeometry().getCoordinateSystem();
		new DemandFromSQL(inFC,inputPersons,cRS,outputPath, sqlServer, sqlUser, sqlPass, sqlTable, sqlFromField, sqlToField, sqlValueField).run();

		System.out.printf("Done! Point shapefile output to %s", outputPath);
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
}
