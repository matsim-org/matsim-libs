package playground.gregor.sim2d_v2.ghostpopulation;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.api.experimental.events.handler.PersonEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.EventsUtils;

import playground.gregor.sim2d_v2.events.TickEvent;
import playground.gregor.sim2d_v2.events.TickEventHandler;
import playground.gregor.sim2d_v2.events.XYVxVyEvent;
import playground.gregor.sim2d_v2.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v2.events.XYVxVyEventsFileReader;

public class GhostPopulationEngine implements TickEventHandler {

	private static final Logger log = Logger.getLogger(GhostPopulationEngine.class);

	private final EventsManager events;
	private GhostPopulationEvents ghostPopulationEvents;

	public GhostPopulationEngine(String ghostPopulationEventsFile, EventsManager events) {
		this.events = events;
		init(ghostPopulationEventsFile);
	}


	private void init(String ghostPopulationEventsFile) {
		log.info("Reading ghost population from events file");
		EventsManager manager = EventsUtils.createEventsManager();
		this.ghostPopulationEvents = new GhostPopulationEvents();
		manager.addHandler(this.ghostPopulationEvents);
		XYVxVyEventsFileReader reader = new XYVxVyEventsFileReader(manager);
		try {
			reader.parse(ghostPopulationEventsFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("done.");
	}


	@Override
	public void reset(int iteration) {
		this.ghostPopulationEvents.reset();

	}

	@Override
	public void handleEvent(TickEvent event) {
		this.ghostPopulationEvents.parrotEvents(this.events, event.getTime());
	}

	private static final class GhostPopulationEvents implements PersonEventHandler{

		List<PersonEvent> events = new ArrayList<PersonEvent>();
		private int pointer = 0;


		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub

		}


		public void reset() {
			this.pointer = 0;

		}





		public void parrotEvents(EventsManager events, double time) {
			while (this.events.size() > 0 && this.pointer  < this.events.size() && this.events.get(this.pointer).getTime() <= time) {
				PersonEvent e = this.events.get(this.pointer++);
				IdImpl id = new IdImpl("ghost_" + e.getPersonId().toString());
				if (e instanceof AgentDepartureEvent) {
					AgentDepartureEvent depart = (AgentDepartureEvent)e;
					AgentDepartureEventImpl ghost = new AgentDepartureEventImpl(depart.getTime(), id, depart.getLinkId(), depart.getLegMode());
					events.processEvent(ghost);
				} else if (e instanceof XYVxVyEvent) {
					XYVxVyEvent xyz = (XYVxVyEvent)e;
					XYVxVyEventImpl ghost = new XYVxVyEventImpl(id, xyz.getCoordinate(), xyz.getVX(), xyz.getVY(), xyz.getTime());
					events.processEvent(ghost);
				} else if (e instanceof AgentArrivalEvent) {
					AgentArrivalEvent arr = (AgentArrivalEvent)e;
					AgentArrivalEvent ghost = new AgentArrivalEventImpl(arr.getTime(), id, arr.getLinkId(), arr.getLegMode());
					events.processEvent(ghost);
				}
			}
		}


		@Override
		public void handleEvent(PersonEvent event) {
			this.events.add(event);
		}


	}

}
