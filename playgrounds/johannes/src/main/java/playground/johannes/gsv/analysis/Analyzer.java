package playground.johannes.gsv.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzer;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTaskComposite;
import playground.johannes.coopsim.analysis.TripGeoDistanceTask;
import playground.johannes.coopsim.pysical.TrajectoryEventsBuilder;
import playground.johannes.gsv.sim.RailCountsCollector;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Analyzer {
	
	

	public static void main(String[] args) throws IOException {
		Config config = ConfigUtils.createConfig();
		new ConfigReader(config).parse(args[0]);
		
		String eventsFile = args[1];
		String outputDir = args[2];
		
		config.transit().setUseTransit(true);
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());
		netReader.readFile(config.getParam("network", "inputNetworkFile"));
		
		TransitScheduleReader schedReader = new TransitScheduleReader(scenario);
		schedReader.readFile(config.getParam("transit", "transitScheduleFile"));
		
		PopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(config.getParam("plans", "inputPlansFile"));
		
		EventsManager events = new EventsManagerImpl();
		
		Set<Person> person = new HashSet<Person>(scenario.getPopulation().getPersons().values());
		TrajectoryEventsBuilder builder = new TrajectoryEventsBuilder(person);
		events.addHandler(builder);
		
		TransitLineAttributes attribs = TransitLineAttributes.createFromFile(config.getParam("gsv", "transitLineAttributes"));
		
		ActivityFacilities facilities = generateFacilities(scenario);
		
		TrajectoryAnalyzerTaskComposite task = new TrajectoryAnalyzerTaskComposite();
		task.addTask(new TripGeoDistanceTask(facilities));
		task.addTask(new ModeShareTask());
		
		RailCountsCollector countsCollector = new RailCountsCollector(attribs);
		events.addHandler(countsCollector);
		
		RailCounts obsCounts = RailCounts.createFromFile(config.getParam("gsv", "counts"), attribs, scenario.getNetwork(), scenario.getTransitSchedule());
		
		builder.reset(0);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
	
		TrajectoryAnalyzer.analyze(builder.trajectories(), task, outputDir);
		
		RailCounts simCounts = countsCollector.getRailCounts();
		KMLRailCountsWriter railCountsWriter = new KMLRailCountsWriter();
		railCountsWriter.write(simCounts, obsCounts, scenario.getNetwork(), scenario.getTransitSchedule(), attribs, outputDir + "counts.kmz", 5);
	
	}
	
	private static ActivityFacilities generateFacilities(Scenario scenario) {
		Population pop = scenario.getPopulation();
		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();
		
		for(Person person : pop.getPersons().values()) {
			for(Plan plan : person.getPlans()) {
				for(int i = 0; i < plan.getPlanElements().size(); i+=2) {
					Activity act = (Activity) plan.getPlanElements().get(i);
					Id<ActivityFacility> id = Id.create("autofacility_"+ i +"_" + person.getId().toString(), ActivityFacility.class);
					ActivityFacilityImpl fac = ((ActivityFacilitiesImpl)facilities).createAndAddFacility(id, act.getCoord());
					fac.createAndAddActivityOption(act.getType());
					
					((ActivityImpl)act).setFacilityId(id);
				}

			}
		}
		
		return facilities;
	}

}
