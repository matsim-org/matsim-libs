package playground.smetzler.bikeDemand;


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


public class PopGenerationBoddin implements Runnable {
	

	String outputPop = "../../../shared-svn/studies/countries/de/berlin-bike/input/szenarios/boddin/demand/boddin_bike_50.xml";
	
	private Scenario scenario;

	private Population population;

	public static void main(String[] args) {
		PopGenerationBoddin Pop = new PopGenerationBoddin();
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
	
	
	

	private void generatePopulation() {
		generateHomeWorkHomeTrips("home1", "work1", 50); // create 1000 trips from zone 'home1' to 'work1'
		//... generate more trips here
	}

	int j;
	private void generateHomeWorkHomeTrips(String from, String to, int quantity) {
		for (int i=0; i<quantity; ++i) {
			Person person = population.getFactory().createPerson(createId(from, to, i, TransportMode.bike));
			Plan plan = population.getFactory().createPlan();
			Coord homeLocation = new Coord(4596667.909172385, 5817222.118015156);
			Coord workLocation = new Coord(4589790.720652158, 5821449.8582265675);
			plan.addActivity(createHome(homeLocation));
					plan.addLeg(createDriveLeg());
			plan.addActivity(createWork(workLocation));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createHome(homeLocation));
			person.addPlan(plan);
			population.addPerson(person);
			j++;
		}
	}

	private Leg createDriveLeg() {
		Leg leg = population.getFactory().createLeg(TransportMode.bike);
		return leg;
	}

	private Coord shoot(Coord source) {
		// Insert code here to blur the input coordinate.
		// For example, add a random number to the x and y coordinates.
		return source;
	}

	private Activity createWork(Coord workLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("work", workLocation);
		activity.setEndTime(17*60*60+j*60);
		return activity;
	}

	private Activity createHome(Coord homeLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("home", homeLocation);
		activity.setEndTime(10*60*60+j*60);
		return activity;
	}

	private Id<Person> createId(String source, String sink, int i, String transportMode) {
		return Id.create(transportMode + "_" + i, Person.class);
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
