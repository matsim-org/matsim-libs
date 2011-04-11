package playground.mzilske.deteval;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.neo4j.graphdb.Transaction;

import playground.mzilske.neo.NeoScenario;

public class VehicleAnalysis {

	public static void main(String[] args) {



		String eventsFileName = "../../run951/it.100/951.100.events.txt.gz";

		Map<String,String> config = new HashMap<String,String>();
		config.put("neostore.nodestore.db.mapped_memory","80M");
		config.put("neostore.relationshipstore.db.mapped_memory","750M");
		config.put("neostore.propertystore.db.mapped_memory","0M");
		config.put("neostore.propertystore.db.strings.mapped_memory","0M");
		config.put("neostore.propertystore.db.arrays.mapped_memory","0M");
		
		final NeoScenario scenario = new NeoScenario("output/neo", config);
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				System.out.println("Closing server...");
				scenario.shutdown();
			}

		});
		final Map<Id, Integer> currentLegs = new HashMap<Id, Integer>();
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();

		events.addHandler(new AgentDepartureEventHandler() {

			@Override
			public void reset(int iteration) {

			}

			@Override
			public void handleEvent(AgentDepartureEvent event) {
				Integer currentLeg = currentLegs.get(event.getPersonId());
				if (currentLeg == null) {
					currentLeg = 0;
				}
				currentLeg++;
				currentLegs.put(event.getPersonId(), currentLeg);
			}
		});

		events.addHandler(new LinkLeaveEventHandler() {

			@Override
			public void reset(int iteration) {

			}

			@Override
			public void handleEvent(LinkLeaveEvent event) {
				Transaction tx = scenario.beginTx();
				try {
					Person person = scenario.getPopulation().getPersons().get(event.getPersonId());
					Plan plan = person.getSelectedPlan();
					System.out.println(person.getId());
					Leg leg = (Leg) plan.getPlanElements().get(2*currentLegs.get(event.getPersonId()) -1);
					System.out.println(leg.getMode());
					tx.success();
				} finally {
					tx.finish();
				}
			}
		});

		new MatsimEventsReader(events).readFile(eventsFileName);

	}

}




