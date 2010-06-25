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
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;

import playground.mzilske.neo.NeoScenario;

public class VehicleAnalysis {

	public static void main(String[] args) {



		String eventsFileName = "../../run951/it.100/951.100.events.txt.gz";

		final NeoScenario scenario = new NeoScenario("output/neo");
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				System.out.println("Closing server...");
				scenario.shutdown();
			}

		});
		final Map<Id, Integer> currentLegs = new HashMap<Id, Integer>();
		EventsManager events = new EventsManagerImpl();

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
				scenario.beginTx();
				try {
					Person person = scenario.getPopulation().getPersons().get(event.getPersonId());
					Plan plan = person.getSelectedPlan();
					System.out.println(person.getId());
					Leg leg = (Leg) plan.getPlanElements().get(2*currentLegs.get(event.getPersonId()) -1);
					System.out.println(leg.getMode());
					scenario.success();
				} finally {
					scenario.finish();
				}
			}
		});

		new MatsimEventsReader(events).readFile(eventsFileName);

	}

}




