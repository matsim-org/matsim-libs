package playground.mzilske.vis;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.MobsimConfigGroupI;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class LeakTest {
	
	public static void main(String[] args) throws InterruptedException, InvocationTargetException {
//		 String fileName = "../../detailedEval/Net/network.xml.gz";
//		 String eventsFileName = "../../run950/it.1000/950.1000.events.txt.gz";
//		
//		 String fileName = "../../run749/749.output_network.xml.gz";
//		 String eventsFileName = "../../run749/it.1000/749.1000.events.txt.gz";
//		
//		String networkFileName = "../../matsim/output/example5/output_network.xml.gz";
//		String eventsFileName = "../../matsim/output/example5/ITERS/it.10/10.events.xml.gz";
//		String populationFileName = "../../matsim/output/example5/output_plans.xml.gz";
		
		String networkFileName = "../../run951/951.output_network.xml.gz";
		String eventsFileName = "../../run951/it.100/951.100.events.txt.gz";
		// String populationFileName = "../../run951/951.output_plans.xml.gz";
		
		String populationFileName = "../../detailedEval/pop/befragte-personen/plans.xml";
		
		
		double snapshotPeriod = 60;
		MobsimConfigGroupI simulationConfigGroup = new SimulationConfigGroup();
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		scenario.setPopulation(new PopulationOnDisk((ScenarioImpl) scenario, DirectoryUtils.createTempDirectory()));
		
		new MatsimNetworkReader(scenario).readFile(networkFileName);
		new MatsimPopulationReader(scenario).readFile(populationFileName);
		
		
		for (int i=0; ; i++) {
			int j=0;
			Collection<Person> persons = new ArrayList<Person>();
			for (Person person : scenario.getPopulation().getPersons().values()) {
				persons.add(person);
			}
			System.out.println("Got " + persons.size()+ " people.");
			for (Person person : persons) {
				person.setId(new IdImpl(i + "_" + j++));
				scenario.getPopulation().addPerson(person);
			}
			System.out.println("Wrote them.");
		}
		
//		EventsManagerImpl events = new EventsManagerImpl();
		
		
//		final OTFVisLiveServer server = new OTFVisLiveServer(scenario, events);
//		SnapshotGenerator snapshotGenerator = new SnapshotGenerator(scenario.getNetwork(), (int) snapshotPeriod, simulationConfigGroup); 
//		snapshotGenerator.addSnapshotWriter(server.getSnapshotReceiver());
//		events.addHandler(snapshotGenerator);
//		
//		OTFHostConnectionManager hostConnectionManager = new OTFHostConnectionManager("Wurst", server);
//		
//		OTFVisClient client = new OTFVisClient();
//		client.setHostConnectionManager(hostConnectionManager);
//		client.setSwing(false);
//		client.run();
//		
//		System.out.println("Reading...");
//		new MatsimEventsReader(events).readFile(eventsFileName);
		
	}

}
