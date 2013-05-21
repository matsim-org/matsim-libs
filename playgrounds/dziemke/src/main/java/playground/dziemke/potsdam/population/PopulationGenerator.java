package playground.dziemke.potsdam.population;

import java.util.List;
import java.util.Map;

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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape;

import com.vividsolutions.jts.geom.Geometry;

public class PopulationGenerator implements Runnable {
  
	private Map<Integer, Geometry> zoneGeometries = ShapeReader.read("D:/Workspace/container/potsdam-pg/input/dlm_kreis.shp");
		
	private Scenario scenario;

	private Population population;
	
	private List <Commuter> commutersE = CommuterReader.read("D:/Workspace/container/potsdam-pg/input/Potsdam_E.csv");
	private List <Commuter> commutersA = CommuterReader.read("D:/Workspace/container/potsdam-pg/input/Potsdam_A.csv");
	
	private List <Commuter> commutersEP = PartOfThePopulation.reduce(commutersE, 1);
	private List <Commuter> commutersAP = PartOfThePopulation.reduce(commutersA, 1);
	

	public static void main(String[] args) {
		PopulationGenerator potsdamPop = new PopulationGenerator();
		potsdamPop.run();	
	}

	
	@Override
	public void run() {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		population = scenario.getPopulation();
		NetworkReaderMatsimV1 test = new NetworkReaderMatsimV1(scenario);
		test.parse("D:/Workspace/container/potsdam-pg/data/network.xml");
		generatePopulation();
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write("D:/Workspace/container/potsdam-pg/data/population.xml");
		
		XY2Links xy2Links = new XY2Links((NetworkImpl) scenario.getNetwork());
		xy2Links.run(scenario.getPopulation());
		String directory = "D:/Workspace/container/potsdam-pg/data/";
		String crs = "PROJCS[\"ETRS89_UTM_Zone_33\",GEOGCS[\"GCS_ETRS89\",DATUM[\"D_ETRS89\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",3500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",15.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]";
		SelectedPlans2ESRIShape plans2Shape = new SelectedPlans2ESRIShape(scenario.getPopulation(), scenario.getNetwork(), MGC.getCRS(crs), directory);
		plans2Shape.setWriteActs(true);
		plans2Shape.setWriteLegs(false);
		plans2Shape.write();
	}


	private void generatePopulation() {
		for (int i = 0; i<commutersEP.size(); i++){
			String home = commutersEP.get(i).getResidence();
			String work = "Potsdam";
			int homeI = commutersEP.get(i).getId();
			int workI = 12054000;
			int currentA = commutersEP.get(i).getAutofahrer();
			generateHomeWorkHomeTrips1(home, homeI, work, workI, currentA);
		}
		
		for (int i = 0; i<commutersAP.size(); i++){
			String home = "Potsdam";
			String work = commutersAP.get(i).getResidence();
			int homeI = 12054000;
			int workI = commutersAP.get(i).getId();
			int currentA = commutersAP.get(i).getAutofahrer();
			generateHomeWorkHomeTrips1(home, homeI, work, workI, currentA);
		}
		
		for (int i = 0; i<commutersEP.size(); i++){
			String home = commutersEP.get(i).getResidence();
			String work = "Potsdam";
			int homeI = commutersEP.get(i).getId();
			int workI = 12054000;
			int currentA = commutersEP.get(i).getOev();
			generateHomeWorkHomeTrips2(home, homeI, work, workI, currentA);
		}
		
		for (int i = 0; i<commutersAP.size(); i++){
			String home = "Potsdam";
			String work = commutersAP.get(i).getResidence();
			int homeI = 12054000;
			int workI = commutersAP.get(i).getId();
			int currentA = commutersAP.get(i).getOev();
			generateHomeWorkHomeTrips2(home, homeI, work, workI, currentA);
		}
	}

	
	private void generateHomeWorkHomeTrips1(String from, int fromI, String to, int toI, int quantity) {
		for (int i=0; i<quantity; ++i) {
			Coord homeLocation = DrawRandomPointFromGeometry.DrawRandomPoint(zoneGeometries.get(fromI));
			Coord workLocation = DrawRandomPointFromGeometry.DrawRandomPoint(zoneGeometries.get(toI));
			Person person = population.getFactory().createPerson(createId(from, to, i,TransportMode.car ));
			Plan plan = population.getFactory().createPlan();
			
			plan.addActivity(createHome(homeLocation));
			plan.addLeg(createDriveLeg1());
			plan.addActivity(createWork(workLocation));
			plan.addLeg(createDriveLeg1());
			plan.addActivity(createHome(homeLocation));
			person.addPlan(plan);
			population.addPerson(person);
		}
	}

	
	private void generateHomeWorkHomeTrips2(String from, int fromI, String to, int toI, int quantity) {
		for (int i=0; i<quantity; ++i) {
			Coord homeLocation = DrawRandomPointFromGeometry.DrawRandomPoint(zoneGeometries.get(fromI));
			Coord workLocation = DrawRandomPointFromGeometry.DrawRandomPoint(zoneGeometries.get(toI));
			Person person = population.getFactory().createPerson(createId(from, to, i,TransportMode.pt ));
			Plan plan = population.getFactory().createPlan();
			
			plan.addActivity(createHome(homeLocation));
			plan.addLeg(createDriveLeg2());
			plan.addActivity(createWork(workLocation));
			plan.addLeg(createDriveLeg2());
			plan.addActivity(createHome(homeLocation));
			person.addPlan(plan);
			population.addPerson(person);
		}
	}

	
	private Leg createDriveLeg1() {
		Leg leg = population.getFactory().createLeg(TransportMode.car);
		return leg;
	}

	
	private Leg createDriveLeg2() {
		Leg leg = population.getFactory().createLeg(TransportMode.pt);
		return leg;
	}

	
	private Activity createWork(Coord workLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("work", workLocation);
		int random = (int) (Math.random() * 7200) - 3600;
			activity.setEndTime(17*60*60 + random);

		return activity;
	}

	
	private Activity createHome(Coord homeLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("home", homeLocation);
		int random = (int) (Math.random() * 7200) - 3600;
		activity.setEndTime(8*60*60 + random);
		return activity;
	}

	
	private Id createId(String source, String sink, int i, String transportMode) {
		return new IdImpl(transportMode + "_" + source + "_" + sink + "_" + i);
	}

}