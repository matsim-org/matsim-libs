package ch.sbb.matsim.contrib.railsim.prototype;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.mobsim.qsim.interfaces.SignalizeableItem;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class places a simple adaptive signal on each link and controls them.
 * <p>
 * A signal state for a link-to-link direction is changed depending on the available capacities on the next link.
 * The signal control also makes sure that trains are not able to enter the same link in the same time step.
 *
 * @author Ihab Kaddoura
 */
public class AdaptiveTrainSignalsControler implements IterationEndsListener, MobsimInitializedListener, MobsimBeforeSimStepListener, TrainPathEntersLinkEventHandler, TrainLeavesLinkEventHandler {

	private final boolean printOutputs = true;

	private static final Logger log = LogManager.getLogger(AdaptiveTrainSignalsControler.class);

	@Inject
	private Scenario scenario;

	private final Map<Id<Link>, HashSet<Id<Vehicle>>> linkId2touchingVehicles = new HashMap<>();
	private final HashMap<Id<Link>, SignalInfo> toLink2signalinfo = new HashMap<>();
	private final HashMap<Id<Link>, SignalizeableItem> linkId2signal = new HashMap<>();

	private final List<String> signalInfosToVisualize = new ArrayList<>();
	private Map<Double, Set<Node>> nextCrossingTime2nodes = new HashMap<>();

	private boolean atLeastOneLinkWithOppositeDirectionLink = false;
	private boolean atLeastOneLinkWithMinimumTrainHeadway = false;

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {

		Netsim mobsim = (Netsim) e.getQueueSimulation();

		for (Link link : this.scenario.getNetwork().getLinks().values()) {

			// place a signal on all links
			SignalizeableItem signal = (SignalizeableItem) mobsim.getNetsimNetwork().getNetsimLink(link.getId());
			signal.setSignalized(true);
			this.linkId2signal.put(link.getId(), signal);

			// initialize ingoing links for each link
			SignalInfo signalInfo = new SignalInfo(link, this.scenario.getNetwork());
			this.toLink2signalinfo.put(link.getId(), signalInfo);

			// initialize link utilization
			this.linkId2touchingVehicles.put(link.getId(), new HashSet<>());

			if (RailsimUtils.getOppositeDirectionLink(link, this.scenario.getNetwork()) != null) {
				atLeastOneLinkWithOppositeDirectionLink = true;
			}

			if (RailsimUtils.getMinimumTrainHeadwayTime(link) > 0.) {
				atLeastOneLinkWithMinimumTrainHeadway = true;
			}

		}
	}

	@Override
	public void reset(int iteration) {
		if (iteration > 0) throw new RuntimeException("Running more than 1 iteration. Aborting...");
	}

	@Override
	public void handleEvent(TrainLeavesLink event) {

		Link link = this.scenario.getNetwork().getLinks().get(event.getLinkId());
		Id<Vehicle> vehicleId = event.getVehicleId();

//		log.warn("-");
//		log.warn("Train " + vehicleId + " leaves link " + link.getId() + " (time: " + event.getTime() + ")");

		// bookkeeping: this train is no longer 'on' the link
		this.linkId2touchingVehicles.get(link.getId()).remove(vehicleId);

		// 1) capacity condition

		int trainCapacity = RailsimUtils.getTrainCapacity(link);

		int vehCount = 0;
		if (this.linkId2touchingVehicles.get(link.getId()) != null) {
			vehCount = this.linkId2touchingVehicles.get(link.getId()).size();
		}

		if (vehCount == trainCapacity - 1) {
			// There is capacity available on the link, update the signal condition for all inLinks...
			this.toLink2signalinfo.get(link.getId()).changeCondition(SignalInfo.SignalCondition.linkCapacity, SignalGroupState.GREEN);

			// also make sure the from node is green for all other links
			for (Link linkWithSameFromNode : link.getFromNode().getOutLinks().values()) {
				if (linkWithSameFromNode != link) {
					// not the same link
					if (!isBlocked(linkWithSameFromNode)) {
						this.toLink2signalinfo.get(linkWithSameFromNode.getId()).changeCondition(SignalInfo.SignalCondition.nodeCapacity, SignalGroupState.GREEN);
					}
				}
			}

		} else {
			// do nothing
		}

		// 2) opposite direction

		if (vehCount == 0 && RailsimUtils.getOppositeDirectionLink(link, this.scenario.getNetwork()) != null) {
			Id<Link> oppositeDirectionLink = this.toLink2signalinfo.get(link.getId()).getOppositeLink().getId();
			this.toLink2signalinfo.get(oppositeDirectionLink).changeCondition(SignalInfo.SignalCondition.oppositeDirection, SignalGroupState.GREEN);
		}

		// 3) minimal train headway (for the toNode of the link)

		double minimumTime = RailsimUtils.getMinimumTrainHeadwayTime(link);
		double earliestLeaveTime = event.getTime() + minimumTime;

		if (earliestLeaveTime > event.getTime()) {
			for (Link toLink : link.getToNode().getOutLinks().values()) {
				this.toLink2signalinfo.get(toLink.getId()).changeCondition(SignalInfo.SignalCondition.nodeMinimumHeadway, SignalGroupState.RED);
			}

			if (this.nextCrossingTime2nodes.get(earliestLeaveTime) == null) {
				Set<Node> nodes = new HashSet<>();
				nodes.add(link.getToNode());
				this.nextCrossingTime2nodes.put(earliestLeaveTime, nodes);
			} else {
				this.nextCrossingTime2nodes.get(earliestLeaveTime).add(link.getToNode());
			}

//			log.warn("Node " + link.getToNode().getId() + " blocked until " + earliestLeaveTime);
		}

	}

	@Override
	public void handleEvent(TrainPathEntersLink event) {

		Id<Vehicle> vehicleId = event.getVehicleId();
		Id<Link> linkId = event.getLinkId();
		Link link = this.scenario.getNetwork().getLinks().get(linkId);

//		log.warn("-------");
//		log.warn("Fahrweg of " + vehicleId + " enters link " + linkId);

		// bookkeeping: register the train 'on' the link
		this.linkId2touchingVehicles.get(linkId).add(vehicleId);

		// 1) capacity condition

		if (isBlocked(link)) {
			this.toLink2signalinfo.get(link.getId()).changeCondition(SignalInfo.SignalCondition.linkCapacity, SignalGroupState.RED);

			// also make sure the from node is blocked for all other links
			for (Link linkWithSameFromNode : link.getFromNode().getOutLinks().values()) {
				if (linkWithSameFromNode != link) {
					// not the same link
					this.toLink2signalinfo.get(linkWithSameFromNode.getId()).changeCondition(SignalInfo.SignalCondition.nodeCapacity, SignalGroupState.RED);

				}
			}
		}

		// 2) opposite direction

		if (RailsimUtils.getOppositeDirectionLink(link, this.scenario.getNetwork()) != null) {
			Id<Link> oppositeDirectionLink = this.toLink2signalinfo.get(link.getId()).getOppositeLink().getId();
			this.toLink2signalinfo.get(oppositeDirectionLink).changeCondition(SignalInfo.SignalCondition.oppositeDirection, SignalGroupState.RED);
		}
	}

	/**
	 * @param link
	 * @return
	 */
	private boolean isBlocked(Link link) {
		int trainCapacity = RailsimUtils.getTrainCapacity(link);

		int vehCount = 0;
		if (this.linkId2touchingVehicles.get(link.getId()) != null) {
			vehCount = this.linkId2touchingVehicles.get(link.getId()).size();
		}
		if (vehCount == trainCapacity) {
			return true;
		} else if (vehCount < trainCapacity) {
			return false;
		} else {
			log.warn("+++++++++++++++++++++++++++++++++++++++++++++++++++++");
			log.warn("Link: " + link.getId());
			log.warn("Vehicles touching this link: " + this.linkId2touchingVehicles.get(link.getId()));
			log.warn("trainCapacity: " + trainCapacity);
			log.warn("attributes: " + link.getAttributes().toString());
			log.warn("+++++++++++++++++++++++++++++++++++++++++++++++++++++");
			throw new RuntimeException("More vehicles than allowed on link " + link.getId());
		}
	}

	private void storeSignalStateInfo(double simulationTime, Link fromLink, Link toLink, SignalGroupState state) {

		// I want the signals to be visualized right before the end of the from link...
		LineSegment ls = new LineSegment(fromLink.getFromNode().getCoord().getX(), fromLink.getFromNode().getCoord().getY(), fromLink.getToNode().getCoord().getX(), fromLink.getToNode().getCoord().getY());
		Coordinate point = ls.pointAlong(0.8);

		this.signalInfosToVisualize.add(simulationTime + ";" + point.getX() + ";" + point.getY() + ";" + toLink.getCoord().getX() + ";" + toLink.getCoord().getY() + ";" + state.toString());
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (printOutputs) {
			printSignalInfos(this.scenario.getConfig().controler().getOutputDirectory() + "/" + this.scenario.getConfig().controler().getRunId() + ".visSignalStates.csv", this.signalInfosToVisualize);
		}
	}

	private void printSignalInfos(String outputFile, List<String> strings) {

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);

		try {
			writer.write("time;X;Y;toLinkX;toLinkY;state");
			writer.newLine();
			for (String line : strings) {
				writer.write(line);
				writer.newLine();
			}
			writer.close();

			log.info("Text info written to file.");
		} catch (Exception e) {
			log.warn("Text info not written to file.");
		}
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {

		double time = e.getSimulationTime();

		// see if enough time has passed and we can update the headway condition
		if (atLeastOneLinkWithMinimumTrainHeadway) updateHeadwaySignalCondition(time);

		// update conflicts between toLinks
		if (atLeastOneLinkWithOppositeDirectionLink) updateConflicts();

		// update all signal states based on the current conditions
		updateSignalStates(time);
	}

	/**
	 * if there is a conflicting toLink, set the conflictingOppositeLink condition to Red for one of the conflicting toLinks
	 */
	private void updateConflicts() {
		// update conflicting opposite links
		this.toLink2signalinfo.values().stream().forEach(signalInfo -> {
			if (signalInfo.getOppositeLink() != null) {
				if (signalInfo.isConsiderInNextTimeStep()) {
					signalInfo.changeCondition(SignalInfo.SignalCondition.conflictingOppositeLink, SignalGroupState.GREEN);
					this.toLink2signalinfo.get(signalInfo.getOppositeLink().getId()).changeCondition(SignalInfo.SignalCondition.conflictingOppositeLink, SignalGroupState.RED);
					// change for next time step
					signalInfo.setConsiderInNextTimeStep(false);

				} else {
					signalInfo.changeCondition(SignalInfo.SignalCondition.conflictingOppositeLink, SignalGroupState.RED);
					this.toLink2signalinfo.get(signalInfo.getOppositeLink().getId()).changeCondition(SignalInfo.SignalCondition.conflictingOppositeLink, SignalGroupState.GREEN);

					// change for next time step
					signalInfo.setConsiderInNextTimeStep(true);
				}
			}
		});
	}

	/**
	 * Iterate through all signals and see where all conditions are green...
	 * <p>
	 * - if there is only one inLink, directly switch to green (there are no potential conflicts between any inLinks)
	 * - if there is more than one inLink, make sure there is only from link green (otherwise vehicles may enter a link in the same time step...)
	 *
	 * @param time
	 */
	private void updateSignalStates(double time) {

		// TODO: improve performance
		// with parallel() I get some weird test failures...
		// parallel().forEachOrdered() runs without any problems but does not improve performance

		this.toLink2signalinfo.values().stream().forEach(signalInfo -> {

			boolean demandOnAFromLink = false;
			for (Link fromLinkInfo : signalInfo.getFromLink2fromLinkInfo()) {
				if (this.linkId2touchingVehicles.get(fromLinkInfo.getId()).size() > 0) {
					demandOnAFromLink = true;
					break;
				}
			}

			if (demandOnAFromLink) {
				if (signalInfo.allConditionsGreen()) {
					if (signalInfo.getFromLink2fromLinkInfo().size() <= 1) {
						// no potential conflict, set for all ingoing links to green
						for (Link fromLinkInfo : signalInfo.getFromLink2fromLinkInfo()) {
							switchSignalState(time, fromLinkInfo, signalInfo.getToLink(), SignalGroupState.GREEN);
						}
					} else {
						// potential conflict, only set to green for one inlink
						Link fromLink = signalInfo.getNextFromLink();
						switchSignalState(time, fromLink, signalInfo.getToLink(), SignalGroupState.GREEN);

						// and switch all other fromLinks to red.
						for (Link fromLinkInfo : signalInfo.getFromLink2fromLinkInfo()) {
							if (fromLinkInfo != fromLink) {
								switchSignalState(time, fromLinkInfo, signalInfo.getToLink(), SignalGroupState.RED);
							}
						}
					}
				} else {
//					log.info("At least one Red condition. Setting to red: from link: " + fromLinkInfo.getFromLink().getId() + " --- to link: " + signalInfo.getToLink().getId());
					for (Link fromLinkInfo : signalInfo.getFromLink2fromLinkInfo()) {
						switchSignalState(time, fromLinkInfo, signalInfo.getToLink(), SignalGroupState.RED);
					}
				}
			}

		});
	}

	/**
	 * @param time
	 */
	private void updateHeadwaySignalCondition(double time) {
		if (this.nextCrossingTime2nodes.get(time) != null) {
			for (Node node : this.nextCrossingTime2nodes.get(time)) {
				for (Link toLink : node.getOutLinks().values()) {
					this.toLink2signalinfo.get(toLink.getId()).changeCondition(SignalInfo.SignalCondition.nodeMinimumHeadway, SignalGroupState.GREEN);
				}
			}
			this.nextCrossingTime2nodes.remove(time);
		}
	}

	private void switchSignalState(double time, Link fromLink, Link toLink, SignalGroupState state) {
		boolean previousSignalStateIsGreen = this.linkId2signal.get(fromLink.getId()).hasGreenForToLink(toLink.getId());

		if (previousSignalStateIsGreen && state == SignalGroupState.GREEN) {
			// was already green, nothing to do!
//			log.warn("Signal was already green and was then requested to be changed to green again..." + fromLink.getId() + " -> " + toLink.getId());
		} else if (!previousSignalStateIsGreen && state == SignalGroupState.RED) {
			// was already red, nothing to do!
//			log.warn("Signal was already red and was then requested to be changed to red again..." + fromLink.getId() + " -> " + toLink.getId());
		} else {
			// the signal state changes from green to red or from red to green
//			log.info("change signal state: " + fromLink.getId() + " --> " + toLink.getId() + ":" + state.toString());
			this.linkId2signal.get(fromLink.getId()).setSignalStateForTurningMove(state, toLink.getId());
			if (printOutputs) this.storeSignalStateInfo(time, fromLink, toLink, state);
		}

	}

}
