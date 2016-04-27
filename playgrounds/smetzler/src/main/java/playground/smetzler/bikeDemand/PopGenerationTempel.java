package playground.smetzler.bikeDemand;


import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Point;

public class PopGenerationTempel implements Runnable {
	

	String outputPop = "../../../shared-svn/studies/countries/de/berlin-bike/input/demand/tempelhof_carnbike.xml";
	//String DHDN = "EPSG:3068";


	//private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, DHDN);

	private Scenario scenario;

	private Population population;

	public static void main(String[] args) {
		PopGenerationTempel Pop = new PopGenerationTempel();
		Pop.run();
	}

	@Override
	public void run() {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		population = scenario.getPopulation();
		generatePopulation();
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write(outputPop);
	}
	
	private Random rnd = new Random(1234); //1234 = seed
	// Create a GeometryFactory if you don't have one already
	GeometryFactory geometryFactory = new GeometryFactory();

	// Simply pass an array of Coordinate or a CoordinateSequence to its method
	Coordinate sw = new Coordinate(4593802, 5816301);
	Coordinate se = new Coordinate(4597418, 5816301);
	Coordinate nw = new Coordinate(4593802, 5818751);
	Coordinate ne = new Coordinate(4597418, 5818751);
	
	Coordinate[] arr = new Coordinate[] {sw, se, ne, nw, sw};
	
	Polygon polygonFromCoordinates = geometryFactory.createPolygon(arr);


	private Coord drawRandomPointFromGeometry() {
//		//tempelhof
//		double minX =4593802;
//		double minY =5816301;
//		double maxX =4597418;
//		double maxY =5818751;
		
		Point p;
		double x, y;
		do {
			x = polygonFromCoordinates.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (polygonFromCoordinates.getEnvelopeInternal().getMaxX() - polygonFromCoordinates.getEnvelopeInternal().getMinX());
			y = polygonFromCoordinates.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (polygonFromCoordinates.getEnvelopeInternal().getMaxY() - polygonFromCoordinates.getEnvelopeInternal().getMinY());
//			x = minX + rnd.nextDouble() * (maxX - minX);
//			y = minY + rnd.nextDouble() * (maxY - minY);
			p = MGC.xy2Point(x, y);
		} while (!polygonFromCoordinates.contains(p));
		Coord coord = new Coord(p.getX(), p.getY());
		return coord;
	}
	
	

	private void generatePopulation() {
		generateHomeWorkHomeTrips("home1", "work1", 1000, TransportMode.bike);
		generateHomeWorkHomeTrips("home1", "work1", 1000, TransportMode.car);/// create 1000 trips from zone 'home1' to 'work1'
		//... generate more trips here
	}

	private void generateHomeWorkHomeTrips(String from, String to, int quantity, String mode) {
		for (int i=0; i<quantity; ++i) {
//			Coord source = zoneGeometries.get(from);
//			Coord sink = zoneGeometries.get(to);
			Coord source = drawRandomPointFromGeometry();
			Coord sink = drawRandomPointFromGeometry();
			Person person = population.getFactory().createPerson(createId(from, to, i, mode));
			Plan plan = population.getFactory().createPlan();
//			Coord homeLocation = shoot(ct.transform(source));
//			Coord workLocation = shoot(ct.transform(sink));
			Coord homeLocation = shoot(source);
			Coord workLocation = shoot(sink);
			plan.addActivity(createHome(homeLocation));
					plan.addLeg(createDriveLeg(mode));
			plan.addActivity(createWork(workLocation));
			plan.addLeg(createDriveLeg(mode));
			plan.addActivity(createHome(homeLocation));
			person.addPlan(plan);
			population.addPerson(person);
		}
	}

	private Leg createDriveLeg(String mode) {
		Leg leg = population.getFactory().createLeg(mode);
		return leg;
	}

	private Coord shoot(Coord source) {
		// Insert code here to blur the input coordinate.
		// For example, add a random number to the x and y coordinates.
		return source;
	}

	private Activity createWork(Coord workLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("work", workLocation);
		activity.setEndTime(17*60*60);
		return activity;
	}

	private Activity createHome(Coord homeLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("home", homeLocation);
		activity.setEndTime(10*60*60);
		return activity;
	}

	private Id<Person> createId(String source, String sink, int i, String transportMode) {
		return Id.create(transportMode + "_" + source + "_" + sink + "_" + i, Person.class);
	}

}


//falls ich die bounderies doch nochmal aus einem shapefalie auslesen will und nicht manuel eingeben

//../../../shared-svn/studies/countries/de/berlin-bike/input/network/tempelhof_MATsim_line.shp	


//public Map<String,Geometry> readShapeFile(String filename, String attrString){
//	Map<String,Geometry> shapeMap = new HashMap<String, Geometry>();
//	for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {
//		GeometryFactory geometryFactory= new GeometryFactory();
//		WKTReader wktReader = new WKTReader(geometryFactory);
//		Geometry geometry;
//		try {
//			geometry = wktReader.read((ft.getAttribute("the_geom")).toString());
//			shapeMap.put(ft.getAttribute(attrString).toString(),geometry);
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	} 
//	return shapeMap;
//}
