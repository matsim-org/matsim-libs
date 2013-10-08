package playground.mzilske.energy;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

public class LinkActivityOccupancyCounter implements ActivityStartEventHandler,
		ActivityEndEventHandler {

	private class Link {
		private int occupancy = 0;
		private int maximumWithinTimeWindow;

		void inc() {
			occupancy++;
			check();
		}

		private void check() {
			if (time >= minTime && time <= maxTime) {
				maximumWithinTimeWindow = Math.max(maximumWithinTimeWindow, occupancy);
			}
		}

		void dec() {
			occupancy--;
		}
	
	}

	private double time = 0.0;
	private final double minTime;
	private final double maxTime;
	
	private Map<Id, Link> links = new HashMap<Id, Link>();
	private boolean checkedWhenLeavingInterval = false;
	private boolean checkedWhenEnteringInterval = false;
	
	private boolean hasRun = false;
	private boolean finished = false;
	
	public LinkActivityOccupancyCounter(Population population) {
		this(population, 0.0, Double.POSITIVE_INFINITY);
	}
	
	public LinkActivityOccupancyCounter(Population population, double minTime, double maxTime) {
		this.minTime = minTime;
		this.maxTime = maxTime;
		// Put all the people into their beds.
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			if (!plan.getPlanElements().isEmpty() && plan.getPlanElements().get(0) instanceof Activity) {
				Activity nightActivity = (Activity) plan.getPlanElements().get(0);
				Link nightLink = getLink(nightActivity.getLinkId());
				nightLink.occupancy++;
			}
		}
	}

	@Override
	public void reset(int iteration) {
		if (hasRun) {
			throw new RuntimeException("This is a once-only event handler.");
		}
		hasRun = true;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		tick(event.getTime());
		Link link = getLink(event.getLinkId());
		link.dec();
		assert(link.occupancy >= 0);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		tick(event.getTime());
		Link link = getLink(event.getLinkId());
		link.inc();
	}

	private void tick(double time) {
		this.time = time;
		if(time >= minTime && !checkedWhenEnteringInterval) {
			checkAllLinks();
			checkedWhenEnteringInterval = true;
		}
		if(time <= maxTime && !checkedWhenLeavingInterval) {
			checkAllLinks();
			checkedWhenLeavingInterval = true;
		}
	}

	private void checkAllLinks() {
		for (Link link : links.values()) {
			link.check();
		}
	}

	private Link getLink(Id linkId) {
		Link link = links.get(linkId);
		if (link == null) {
			link = new Link();
			links.put(linkId, link);
		}
		return link;
	}
	
	public void finish() {
		checkAllLinks();
		finished = true;
	}
	
	public void dump() {
		assertFinished();
		for (Entry<Id, Link> entry : links.entrySet()) {
			System.out.println(entry.getKey() + ": " + entry.getValue().maximumWithinTimeWindow);
		}
	}
	
	private void assertFinished() {
		if (!finished) {
			throw new RuntimeException();
		}
	}

	public int getMaximumOccupancy(Id linkId) {
		assertFinished();
		return getLink(linkId).maximumWithinTimeWindow;
	}
	

}
