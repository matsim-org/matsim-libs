package ch.sbb.matsim.contrib.railsim.prototype;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * This class computes the spatial dimension of trains along several links.
 * <p>
 * Processes default MATSim events (e.g. link enter events), accounts for the length of the train plus the reserved train path
 * and then throws train enters and train leaves events.
 *
 * @author Ihab Kaddoura
 */
public class SpatialTrainDimension implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler {
	private static final Logger log = LogManager.getLogger(SpatialTrainDimension.class);

	@Inject
	private Scenario scenario;

	@Inject
	private EventsManager events;

	@Inject
	private TrainStatistics statistics;

	@Inject
	private RailsimLinkSpeedCalculator linkSpeedCalculator;

	private final HashMap<Id<Vehicle>, LinkedList<Id<Link>>> vehicleId2currentlyTouchedLinksTotalSorted = new HashMap<>();

	// in the following data container we don't need the actual queue or order of vehicles
	private final HashMap<Id<Vehicle>, Set<Id<Link>>> vehicleId2currentlyTouchedLinksByTrain = new HashMap<>();

	private int warnCnt = 0;

	@Override
	public void reset(int iteration) {
		if (iteration > 0) throw new RuntimeException("Running more than 1 iteration. Aborting...");
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {

		// matsim vehicle is the tip of the train path (=fahrweg)
		this.events.processEvent(new TrainPathEntersLink(event.getTime(), event.getLinkId(), event.getVehicleId()));

		// update the information about which links are touched and blocked (by the train and/or by the fahrweg)
		updateBlockLinks(event.getLinkId(), event.getVehicleId(), event.getTime());
	}

	/**
	 * @param linkId
	 * @param vehicleId
	 * @param time
	 */
	private void updateBlockLinks(Id<Link> linkId, Id<Vehicle> vehicleId, double time) {

		final LinkedList<Id<Link>> touchedLinksSorted = this.vehicleId2currentlyTouchedLinksTotalSorted.computeIfAbsent(vehicleId, k -> new LinkedList<>());
		// add the just entered linkId to the 'queue'
		touchedLinksSorted.addFirst(linkId);

		final Set<Id<Link>> touchedLinksByTrain = this.vehicleId2currentlyTouchedLinksByTrain.computeIfAbsent(vehicleId, k -> new HashSet<>());

		VehicleType vehicleType = this.scenario.getTransitVehicles().getVehicles().get(vehicleId).getType();
		final double vehicleLength = vehicleType.getLength();
		final double fahrwegLength = getFahrwegLength(vehicleId, vehicleType, linkId, time);
		final double trainAndFahrwegLength = vehicleLength + fahrwegLength;

		// iterate through the list of touched links (start from where the transit vehicle currently is)

		//   IIIIIIIIIIIIIIIIIIIII----------------------------  >>>
		//   |        train		 |		   fahrweg           'vehicle'

		LinkedList<Id<Link>> touchedLinksSortedUpdated = new LinkedList<>();

		LinkedList<Link> linksNoLongerTouched = new LinkedList<>();

		Link latestTouchedLink = this.scenario.getNetwork().getLinks().get(linkId);
		if (latestTouchedLink.getLength() > trainAndFahrwegLength) {
			// This link is long enough --> This link is touched by both the path and the train itself

			// update the queue
			touchedLinksSortedUpdated.add(linkId);

			// all other links are no longer touched, update the list of no longer touched links
			for (Id<Link> touchedLinkId : touchedLinksSorted) {
				if (touchedLinkId.toString().equals(linkId.toString())) {
					// this link is still touched
				} else {
					Link touchedLink = this.scenario.getNetwork().getLinks().get(touchedLinkId);
					linksNoLongerTouched.addFirst(touchedLink);
				}
			}

			// The link is also touched by the train -> throw train enters link event
			this.events.processEvent(new TrainEntersLink(time, linkId, vehicleId));
			touchedLinksByTrain.add(linkId);

		} else {
			double lengthCumulated = 0.;
			for (Id<Link> touchedLinkId : touchedLinksSorted) {
				Link touchedLink = this.scenario.getNetwork().getLinks().get(touchedLinkId);

				if (lengthCumulated <= trainAndFahrwegLength) {
					// The link is touched by either the fahrweg or the train itself.

					touchedLinksSortedUpdated.add(touchedLinkId);

					// compute the train position (for visualization purposes only)
					if (lengthCumulated < fahrwegLength) {
						// this link is touched by the reserved fahrweg (not by the train itself)
					} else {
						// this link is touched by the train itself

						if (touchedLinksByTrain.contains(touchedLinkId)) {
							// This link has already been touched by the train.
						} else {
							// This link is touched by the train for the first time.
							this.events.processEvent(new TrainEntersLink(time, touchedLinkId, vehicleId));
							touchedLinksByTrain.add(touchedLinkId);
						}
					}

					// increase the cumulated length
					lengthCumulated += touchedLink.getLength();

				} else {
					// collect the links which are no longer touched by the train or fahrweg
					linksNoLongerTouched.addFirst(touchedLink);
				}
			}
		}

		// throw 'train enters link' events for all links no longer touched by the train path
		// otherwise these events are missing and event-based processing may not work correctly
		for (Link linkNoLongerTouched : linksNoLongerTouched) {
			if (touchedLinksByTrain.contains(linkNoLongerTouched.getId())) {
				// a 'train enters link' event has already been thrown for this link
			} else {
				if (warnCnt < 5) {
					log.warn("'train entered link' event is thrown in the same time step as the 'train left link' event. " + "This may be prevented by reducing the link length or reducing the physical extension of the train (train length or train path length).");
					log.warn("link: " + linkId + " / vehicle: " + vehicleId);
					warnCnt++;
				} else if (warnCnt == 5) {
					log.warn("Further warnings of this type will not be printed out.");
					warnCnt++;
				}
				this.events.processEvent(new TrainEntersLink(time, linkNoLongerTouched.getId(), vehicleId));
				touchedLinksByTrain.add(linkNoLongerTouched.getId());
			}
		}

		// throw 'train leaves link' events for all links no longer touched by the train (or train path)
		for (Link linkNoLongerTouched : linksNoLongerTouched) {
			this.events.processEvent(new TrainLeavesLink(time, linkNoLongerTouched.getId(), vehicleId));

			// link is also no longer touched by train
			touchedLinksByTrain.remove(linkNoLongerTouched.getId());
		}

		// add the updated lists of blocked link IDs
		this.vehicleId2currentlyTouchedLinksTotalSorted.put(vehicleId, touchedLinksSortedUpdated);
	}

	/**
	 * @param vehicleId
	 * @param vehicleType
	 * @param linkId
	 * @return
	 */
	private double getFahrwegLength(Id<Vehicle> vehicleId, VehicleType vehicleType, Id<Link> linkId, double time) {

		RailsimConfigGroup railsimConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), RailsimConfigGroup.class);

		// Set the reserved train path to 0 if the train is on a link at which the train stops.
		TransitStopFacility stopFacility = this.statistics.getLink2stop().get(linkId);
		if (stopFacility != null) {
			// this link is a stop
			Id<TransitLine> currentLineId = this.statistics.getVehicleId2currentTransitLine().get(vehicleId);
			Id<TransitRoute> currentRouteId = this.statistics.getVehicleId2currentTransitRoute().get(vehicleId);
			TransitRouteStop routeStop = this.scenario.getTransitSchedule().getTransitLines().get(currentLineId).getRoutes().get(currentRouteId).getStop(stopFacility);
			if (routeStop != null) {
				// The train is about to stop at this station and the speed will be reduced to 0.
				// Setting the length of the reserved train path to 0.
				return 0.;
			} else {
				// The train will not stop at this station and the speed will not be reduced.
				// The reserved train path will not be reduced to 0.
			}
		}

		// start with the theoretical maximum speed
		// the following does not account for the acceleration or deceleration...
//		final double maxSpeedOnTheCurrentLink = Math.min(vehicleType.getMaximumVelocity(),
//				this.scenario.getNetwork().getLinks().get(linkId).getFreespeed());

		Vehicle vehicle = scenario.getTransitVehicles().getVehicles().get(vehicleId);
		Link link = scenario.getNetwork().getLinks().get(linkId);

		final double maxSpeedOnTheCurrentLink = linkSpeedCalculator.getRailsimMaximumVelocity(vehicle, this.scenario.getNetwork().getLinks().get(linkId), time);

		// account for congestion effects, e.g. a fast train behind a slower train
		double speedWhenEnteringTheLink = this.statistics.getSpeedOnPreviousLink(vehicleId);
		final double currentSpeed = Math.min(maxSpeedOnTheCurrentLink, speedWhenEnteringTheLink);

		final double reactionTime = railsimConfigGroup.getReactionTime();
		final double gravity = railsimConfigGroup.getGravity();

		double deceleration = RailsimUtils.getTrainDeceleration(vehicle, railsimConfigGroup);
		double grade = RailsimUtils.getGrade(link, railsimConfigGroup);

		// calculate the fahrwegLength with given formula
		double fahrwegLength = (currentSpeed * reactionTime) + (Math.pow(currentSpeed, 2) / (2 * (deceleration + gravity * grade))) + ((currentSpeed / 2) + 20);

		return fahrwegLength;
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {

		// manually process the relevant events on the initial link
		this.events.processEvent(new TrainPathEntersLink(event.getTime(), event.getLinkId(), event.getVehicleId()));
		this.events.processEvent(new TrainEntersLink(event.getTime(), event.getLinkId(), event.getVehicleId()));

		// also add the initial link to the relevant data containers

		Set<Id<Link>> touchedLinksByTrainUpdated = new HashSet<>();
		touchedLinksByTrainUpdated.add(event.getLinkId());
		this.vehicleId2currentlyTouchedLinksByTrain.put(event.getVehicleId(), touchedLinksByTrainUpdated);

		LinkedList<Id<Link>> touchedLinksSorted = new LinkedList<>();
		touchedLinksSorted.add(event.getLinkId());
		this.vehicleId2currentlyTouchedLinksTotalSorted.put(event.getVehicleId(), touchedLinksSorted);
	}

}
