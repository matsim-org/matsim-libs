package playground.jbischoff.taxibus.scenario.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;

import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdAnalyzer;

class WobEventHandler implements PersonDepartureEventHandler,
		PersonArrivalEventHandler, LinkEnterEventHandler,
		LinkLeaveEventHandler, ActivityStartEventHandler {

	private Map<Id<Link>, Double> agentsPerLink = new HashMap<>();
	private Map<Id<Link>, Double> delayPerLink = new HashMap<>();
	private Map<Id<Person>, Double> delayPerAgent = new HashMap<>();

	private Map<Id<Person>, Double> currentCarLegDistance = new HashMap<>();
	private Map<Id<Person>, Double> currentCarLegDepartureTime = new HashMap<>();
	private Map<Id<Person>, Id<Link>> currentCarLegStartLink = new HashMap<>();

	private double drivenDistances = 0;
	private double drivenTimes = 0;
	private Map<String, Double> drivenEuclidianDistance = new HashMap<>();
	private Map<String, Double> drivenBeeLineDerivationFactor = new HashMap<>();

	private Map<String, Double> travelTimeToActivity = new TreeMap<>();
	private Map<String, Double> travelDistanceToActivity = new TreeMap<>();
	private Map<String, Double> travelLegsPerActivity = new TreeMap<>();

	private long leg = 0;

	private Map<Id<Link>, Map<Id<Person>, Double>> agentsOnLinkWithArrivalTime = new HashMap<>();
	private Network network;
	private PtDriverIdAnalyzer ptDriverIdAnalyzer;

	public WobEventHandler(Network network,
			PtDriverIdAnalyzer ptDriverIdAnalyzer) {
		this.network = network;
		this.ptDriverIdAnalyzer = ptDriverIdAnalyzer;
		initializeLinks();
	}

	private void initializeLinks() {
		for (Id<Link> linkId : network.getLinks().keySet()) {
			this.agentsOnLinkWithArrivalTime.put(linkId,
					new HashMap<Id<Person>, Double>());
			this.agentsPerLink.put(linkId, 0.0);
			this.delayPerLink.put(linkId, 0.0);

		}
	}

	@Override
	public void reset(int iteration) {
		initializeLinks();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {

		Id<Person> personId = event.getPersonId();
		this.agentsOnLinkWithArrivalTime.get(event.getLinkId())
				.remove(personId);

	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id<Person> personId = event.getPersonId();
		if (this.ptDriverIdAnalyzer.isPtDriver(personId))
			return;
		if (event.getLegMode().equals("car")) {
			this.currentCarLegDepartureTime.put(personId, event.getTime());
			this.currentCarLegDistance.put(personId, 0.0);
			this.currentCarLegStartLink.put(personId, event.getLinkId());
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> driverId = Id.createPersonId(event.getVehicleId().toString());

		if (this.agentsOnLinkWithArrivalTime.get(event.getLinkId())
				.containsKey(driverId)) {
			double timeOnLink = event.getTime()
					- this.agentsOnLinkWithArrivalTime.get(event.getLinkId())
							.get(driverId);
			Link link = network.getLinks().get(event.getLinkId());
			double freeSpeedTravelTime = link.getLength() / link.getFreespeed();
			double linkDelay = timeOnLink - freeSpeedTravelTime;
			WobDistanceAnalyzer.addIdDoubleToMap(this.agentsPerLink,
					event.getLinkId(), 1.0);
			WobDistanceAnalyzer.addIdDoubleToMap(this.delayPerLink,
					event.getLinkId(), linkDelay);
			WobDistanceAnalyzer.addPersonIdDoubleToMap(this.delayPerAgent,
					driverId, linkDelay);

		}

	}

	@SuppressWarnings("deprecation")
	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> driverId = Id.createPersonId(event.getVehicleId().toString());
		if (this.ptDriverIdAnalyzer.isPtDriver(driverId))
			return;
		this.agentsOnLinkWithArrivalTime.get(event.getLinkId()).put(
				driverId, event.getTime());

		WobDistanceAnalyzer.addPersonIdDoubleToMap(this.currentCarLegDistance,
				driverId, network.getLinks().get(event.getLinkId())
						.getLength());
	}

	public Map<Id<Link>, Double> getAgentsPerLink() {
		return agentsPerLink;
	}

	public Map<Id<Link>, Double> getDelayPerLink() {
		return delayPerLink;
	}

	public Map<Id<Person>, Double> getDelayPerAgent() {
		return delayPerAgent;
	}

	public Map<Id<Link>, Map<Id<Person>, Double>> getAgentsOnLinkWithArrivalTime() {
		return agentsOnLinkWithArrivalTime;
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id<Person> personId = event.getPersonId();

		if (this.currentCarLegDepartureTime.containsKey(personId)) {
			double drivenTime = event.getTime()
					- currentCarLegDepartureTime.remove(personId);
			double drivenDistance = currentCarLegDistance.remove(personId);
			Link startLink = this.network.getLinks().get(
					currentCarLegStartLink.remove(personId));
			Link destinationLink = this.network.getLinks().get(
					event.getLinkId());

			double beelineDistance = NetworkUtils.getEuclideanDistance(
					startLink.getCoord(), destinationLink.getCoord());
			String s = personId + "_" + leg;
			this.drivenDistances = this.drivenDistances+drivenDistance;
			this.drivenTimes = this.drivenTimes+drivenDistance;
			
			this.drivenEuclidianDistance.put(s, beelineDistance);
//			this.drivenTimes.put(s, drivenTime);
			
			if ((drivenDistance!=0)&&(beelineDistance > 1) ){
				
				double beelineDistanceFactor = drivenDistance / beelineDistance;
				if (beelineDistanceFactor > 10){
					System.out.println(beelineDistanceFactor+" seems a lil high for sl" + startLink.getId() + " and dl "+ destinationLink.getId()+" Driven distance: "+ drivenDistance +" vs bl distance "+ beelineDistance);
				}
				this.drivenBeeLineDerivationFactor.put(s, beelineDistanceFactor);
			}

			String activity = event.getActType();
			WobDistanceAnalyzer.addStringDoubleToMap(travelDistanceToActivity,
					activity, drivenDistance);
			WobDistanceAnalyzer.addStringDoubleToMap(travelTimeToActivity,
					activity, drivenTime);
			WobDistanceAnalyzer.addStringDoubleToMap(travelLegsPerActivity,
					activity, 1.0);
			leg++;

		}
	}

	public double getDrivenDistances() {
		return drivenDistances;
	}

	public double getDrivenTimes() {
		return drivenTimes;
	}
	public long getLegs() {
		return leg;
	}

	public Map<String, Double> getDrivenEuclidianDistance() {
		return drivenEuclidianDistance;
	}

	public Map<String, Double> getDrivenBeeLineDerivationFactor() {
		return drivenBeeLineDerivationFactor;
	}

	public Map<String, Double> getTravelTimeToActivity() {
		return travelTimeToActivity;
	}

	public Map<String, Double> getTravelDistanceToActivity() {
		return travelDistanceToActivity;
	}

	public Map<String, Double> getTravelLegsPerActivity() {
		return travelLegsPerActivity;
	}

}