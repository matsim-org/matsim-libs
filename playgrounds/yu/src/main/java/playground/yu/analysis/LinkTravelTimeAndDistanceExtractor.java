/**
 *
 */
package playground.yu.analysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;

public class LinkTravelTimeAndDistanceExtractor implements
		LinkEnterEventHandler, LinkLeaveEventHandler, AgentArrivalEventHandler {
	private Map<Id/* person */, LinkEnterEvent> enteredPersons = new HashMap<Id, LinkEnterEvent>();
	private Network network;
	private int cnt = 0;
	private double sumDist = 0d, sumTravTime = 0d;

	public LinkTravelTimeAndDistanceExtractor(Network network) {
		this.network = network;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id personId = event.getPersonId();
		Id linkId = event.getLinkId();
		if (enteredPersons.containsKey(personId)) {
			throw new RuntimeException("This person (Id " + personId
					+ " ) has entered oder is still in link (Id "
					+ enteredPersons.get(personId).getLinkId()
					+ " )!? Now it wants to enter link (Id " + linkId
					+ " ) (again)?!");
		} else {
			enteredPersons.put(personId, event);
		}
	}

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id personId = event.getPersonId();
		Id linkId = event.getLinkId();
		double leaveTime = event.getTime();
		if (enteredPersons.containsKey(personId)) {
			if (enteredPersons.get(personId).getLinkId().toString().equals(
					linkId.toString())) {

				cnt++;
				sumDist += network.getLinks().get(linkId).getLength();/*
																	 * distance
																	 * [m]
																	 */
				sumTravTime += leaveTime
						- enteredPersons.get(personId).getTime();/* [s] */
			} else {
				throw new RuntimeException("This person (Id " + personId
						+ " ) is still in link (Id "
						+ enteredPersons.get(personId).getLinkId()
						+ " )! How could it leave link (Id " + linkId + " )?!");
			}
			enteredPersons.remove(personId);
		}

	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Id personId = event.getPersonId();
		if (enteredPersons.containsKey(personId)) {
			enteredPersons.remove(personId);
		}

	}

	public String output() {
		StringBuffer sb = new StringBuffer();
		sb.append("number of agents:\t");
		sb.append(cnt);
		sb.append("\n");

		sb.append("avg. used link length:\t");
		sb.append(sumDist / cnt);
		sb.append("\t[m]\n");

		sb.append("avg. travel time of used link:\t");
		sb.append(sumTravTime / cnt);
		sb.append("\t[s]");
		return sb.toString();

	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final String netFilename, eventsFilename, outFilename;
		if (args.length == 0) {
			netFilename = "../matsim/examples/equil/network.xml";
			eventsFilename = "../matsim/output/equil/ITERS/it.10/10.events.txt.gz";
		} else {
			netFilename = args[0];
			eventsFilename = args[1];
		}

		Gbl.startMeasurement();

		Scenario scenario = new ScenarioImpl();

		new MatsimNetworkReader(scenario).readFile(netFilename);

		EventsManagerImpl events = new EventsManagerImpl();

		LinkTravelTimeAndDistanceExtractor lttade = new LinkTravelTimeAndDistanceExtractor(
				scenario.getNetwork());
		events.addHandler(lttade);

		System.out.println("-->reading evetsfile: " + eventsFilename);
		new MatsimEventsReader(events).readFile(eventsFilename);

		System.out.println(lttade.output());

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
