package playground.pieter.demandgeneration.emme;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.world.World;
import org.matsim.world.WorldUtils;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class PlansFromEmmeDemand {

	/**
	 * @param args
	 */
	private final Logger log = Logger.getLogger(PlansFromEmmeDemand.class);
	private final int PERSON_SCALER = 10;
	private final CoordinateReferenceSystem outputCRS; 
	private final String matrixFileName;
	private final String zoneCoordsFileName;
	private final String departureTime;
	private HashMatrix demand;
	private String outputPath;
	private Collection<Feature> homeLocationCollection;
	private Collection<Feature> workLocationCollection;
	private Map<Id,Polygon> zonePolygons;
	private ZoneLayer zones;
	private GeometryFactory geofac;
	private FeatureType ftPerson;
	
	private Map<String, ZoneXY> zoneXYs;

	private Random random;

	public PlansFromEmmeDemand (
			String matrixFileName, String zoneCoordsFileName,
			String departureTime, String outPath, CoordinateReferenceSystem CRS) throws Exception{
		
		this.outputCRS = CRS;
		this.matrixFileName = matrixFileName;
		this.zoneCoordsFileName = zoneCoordsFileName;
		this.departureTime = departureTime;
		this.demand = new HashMatrix(matrixFileName);
		this.outputPath = outPath;
		this.geofac = new GeometryFactory();
		this.random = new Random();
	}
	
	public void processInput() throws Exception{
		initFeatures();
		readZones();
		createHomeLocations();
		ShapeFileWriter.writeGeometries(this.homeLocationCollection, (this.outputPath + "homeLocations.shp"));
		createWorkLocations();
		ShapeFileWriter.writeGeometries(this.workLocationCollection, (this.outputPath + "workLocations.shp"));
	}
	
	public void createPlansXML() throws FileNotFoundException, IOException {
		BufferedWriter output = IOUtils.getBufferedWriter((this.outputPath+"plans.xml.gz"));
		output.write("<?xml version=\"1.0\" ?>\n");
		output.write("<!DOCTYPE plans SYSTEM \"http://www.matsim.org/files/dtd/plans_v4.dtd\">\n");
		output.write("<plans>\n");
		for (Feature person : this.workLocationCollection){
			int ID = (Integer)person.getAttribute(1);
			double homeX = (Double)person.getAttribute(4);
			double homeY = (Double)person.getAttribute(5);
			double workX = (Double)person.getAttribute(6);
			double workY = (Double)person.getAttribute(7);
			String xmlEntry = String.format("\t<person id = \"%d\">\n",ID);
			xmlEntry += "\t\t<plan>\n";
			String endTime = this.departureTime;
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

	///////////////////////////////////////	
	//Public methods for zones
	///////////////////////////////////////


	public Map<String, ZoneXY> getZoneXYs() {
		return this.zoneXYs;
	}


	private void initFeatures() {
		//Define the point collection with its attributes
		final AttributeType[] person = new AttributeType[8];
		person[0] = AttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, this.outputCRS);
		person[1] = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		person[2] = AttributeTypeFactory.newAttributeType("homeZone", Integer.class);
		person[3] = AttributeTypeFactory.newAttributeType("workZone", Integer.class);
		person[4] = AttributeTypeFactory.newAttributeType("homeX", Double.class);
		person[5] = AttributeTypeFactory.newAttributeType("homeY", Double.class);
		person[6] = AttributeTypeFactory.newAttributeType("workX", Double.class);
		person[7] = AttributeTypeFactory.newAttributeType("workY", Double.class);

		try {
			this.ftPerson = FeatureTypeBuilder.newFeatureType(person, "person");
		} catch (final FactoryRegistryException e) {
			e.printStackTrace();
		} catch (final SchemaException e) {
			e.printStackTrace();
		}
	}


	public void readZones(){
		World w = new World();

		ZoneLayer zl =  (ZoneLayer) w.createLayer(new IdImpl("zones"), "emme");
//		ZoneLayer layer = new ZoneLayer(new IdImpl("zones"), "emme");
		this.zones = zl;
		this.zoneXYs = new HashMap<String, ZoneXY>();
		BufferedReader zoneReader;
		try {
			zoneReader = IOUtils.getBufferedReader(this.zoneCoordsFileName);
			String zoneLine = "";
			do {
				zoneLine = zoneReader.readLine();
				if (zoneLine != null) {
					String[] zoneLines = zoneLine.split(",");
					this.getZoneXYs().put(zoneLines[0],
							new ZoneXY(new IdImpl(zoneLines[0]), zoneLines[1], zoneLines[2]));
				}
			} while (zoneLine != null);
			zoneReader.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		/*
		 * Now create the zones.
		 */
		for (ZoneXY zxy : this.zoneXYs.values()) {
			this.zones.createZone(zxy.getZoneId(), zxy.getX(), zxy.getY(), null, null, null, null);
		}
		this.zoneXYs.clear();
	}


	private Point getRandomizedCoordInZone(Id zoneId) {
		Coord zoneCoord = WorldUtils.getRandomCoordInZone(
				(Zone) this.zones.getLocation(zoneId), this.zones);
		Point p = this.geofac.createPoint(new Coordinate(zoneCoord.getX(),zoneCoord.getY()));	
		return p;
	}

	private void createHomeLocations() throws Exception {
		log.info("Creating home locations:");
		this.homeLocationCollection = new ArrayList<Feature>();
		
		/*
		 * Get the HashMatrix's set of headers, generate the row total number of people.
		 */
		int persId = 0;
		int personMultiplier = 1;
		Iterator<Integer> zones = this.demand.getHeaderSet().iterator();
		while (zones.hasNext()) {
			/*
			 * Read the SP_CODE as string, same as HashMap keys.
			 */
			int zoneNumber = zones.next();
			
			long numberOfPeopleInZone = Math.round(this.demand.getRowTotal(zoneNumber))/this.PERSON_SCALER;
			for (long i = 0; i < numberOfPeopleInZone; i++) {
				Point point = getRandomizedCoordInZone(new IdImpl(zoneNumber));
				Object [] fta = {point,persId++,zoneNumber, 9999,
						point.getCoordinate().x, point.getCoordinate().y,0,0};
				Feature ft = this.ftPerson.create(fta);
				this.homeLocationCollection.add(ft);
				// Report progress.
				if(persId == personMultiplier){
					log.info("   home locations created: " + persId);
					personMultiplier *= 2;
				}
			}
		}
		log.info("   home locations created: " + persId + " (Done)");
	}

	private void createWorkLocations() throws ArrayIndexOutOfBoundsException, IllegalAttributeException {
		//creates a work location for each person in personCollection
		System.out.println("Creating work locations:");
		this.workLocationCollection = new ArrayList<Feature>();
		int persId = 0;
		int personMultiplier = 1;
		for(Feature person : this.homeLocationCollection) {
			int homeTAZ = (Integer)person.getAttribute(2);
			int workTAZ = getWorkTAZ(homeTAZ);
			Point workPoint = getRandomizedCoordInZone(new IdImpl(workTAZ));
			//update home locations with new info
			person.setAttribute(3, workTAZ);
			person.setAttribute(6, workPoint.getCoordinate().x);
			person.setAttribute(7, workPoint.getCoordinate().y);
			//create work location point
			Object [] workFeature = {workPoint,person.getAttribute(1),
					homeTAZ, workTAZ,
					person.getAttribute(4), person.getAttribute(5),
					workPoint.getCoordinate().x, workPoint.getCoordinate().y};
			persId = (Integer)person.getAttribute(1);
			// Report progress.
			if(persId == personMultiplier){
				log.info("   work locations created: " + persId);
				personMultiplier *= 2;	
			}
			Feature personFeature = this.ftPerson.create(workFeature);
			this.workLocationCollection.add(personFeature);
		}
		log.info("   work locations created: " + persId + " (Done)");
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
}
