package ch.sbb.matsim.contrib.railsim.prototype;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * This class writes out data to visualize which link is touched or even blocked by trains.
 *
 * @author Ihab Kaddoura
 */
public class LinkUsageVisualizer implements IterationEndsListener, TrainPathEntersLinkEventHandler, TrainLeavesLinkEventHandler, TrainEntersLinkEventHandler {

	private static final Logger log = LogManager.getLogger(LinkUsageVisualizer.class);

	@Inject
	private Scenario scenario;

	private final Map<Id<Link>, HashSet<Id<Vehicle>>> linkId2touchingTrainPaths = new HashMap<>();
	private final Map<Id<Link>, HashSet<Id<Vehicle>>> linkId2touchingTrains = new HashMap<>();

	private final List<String> blockedLinksInfosToVisualize = new ArrayList<>();
	private final List<String> touchedLinksByTrainInfosToVisualize = new ArrayList<>();
	private final List<String> touchedLinksCountToVisualize = new ArrayList<>();
	private final List<String> touchedLinksToVisualize = new ArrayList<>();

	@Override
	public void handleEvent(TrainPathEntersLink event) {

		HashSet<Id<Vehicle>> touchingTrainPaths = this.linkId2touchingTrainPaths.getOrDefault(event.getLinkId(), new HashSet<>());
		touchingTrainPaths.add(event.getVehicleId());
		this.linkId2touchingTrainPaths.put(event.getLinkId(), touchingTrainPaths);

		Link link = this.scenario.getNetwork().getLinks().get(event.getLinkId());

		this.touchedLinksCountToVisualize.add(event.getTime() + ";" + link.getFromNode().getCoord().getX() + ";" + link.getFromNode().getCoord().getY() + ";" + link.getToNode().getCoord().getX() + ";" + link.getToNode().getCoord().getY() + ";" + touchingTrainPaths.size());

		if (touchingTrainPaths.size() == 1) {
			this.touchedLinksToVisualize.add(event.getTime() + ";" + link.getFromNode().getCoord().getX() + ";" + link.getFromNode().getCoord().getY() + ";" + link.getToNode().getCoord().getX() + ";" + link.getToNode().getCoord().getY() + ";" + SignalGroupState.RED.toString());
		}

		if (isBlocked(link)) {
			// the link is blocked
			this.blockedLinksInfosToVisualize.add(event.getTime() + ";" + link.getFromNode().getCoord().getX() + ";" + link.getFromNode().getCoord().getY() + ";" + link.getToNode().getCoord().getX() + ";" + link.getToNode().getCoord().getY() + ";" + SignalGroupState.RED.toString());

		}
	}

	@Override
	public void handleEvent(TrainEntersLink event) {

		HashSet<Id<Vehicle>> touchingTrains = this.linkId2touchingTrains.getOrDefault(event.getLinkId(), new HashSet<>());
		touchingTrains.add(event.getVehicleId());
		this.linkId2touchingTrains.put(event.getLinkId(), touchingTrains);

		Link link = this.scenario.getNetwork().getLinks().get(event.getLinkId());

		if (touchingTrains.size() == 1) {
			this.touchedLinksByTrainInfosToVisualize.add(event.getTime() + ";" + link.getFromNode().getCoord().getX() + ";" + link.getFromNode().getCoord().getY() + ";" + link.getToNode().getCoord().getX() + ";" + link.getToNode().getCoord().getY() + ";" + SignalGroupState.RED.toString());
		}
	}

	@Override
	public void handleEvent(TrainLeavesLink event) {

		Link link = this.scenario.getNetwork().getLinks().get(event.getLinkId());

		HashSet<Id<Vehicle>> touchingTrainPaths = this.linkId2touchingTrainPaths.getOrDefault(event.getLinkId(), new HashSet<>());
		touchingTrainPaths.remove(event.getVehicleId());
		this.linkId2touchingTrainPaths.put(event.getLinkId(), touchingTrainPaths);

		this.touchedLinksCountToVisualize.add(event.getTime() + ";" + link.getFromNode().getCoord().getX() + ";" + link.getFromNode().getCoord().getY() + ";" + link.getToNode().getCoord().getX() + ";" + link.getToNode().getCoord().getY() + ";" + touchingTrainPaths.size());

		int trainCapacity = RailsimUtils.getTrainCapacity(link);

		if (touchingTrainPaths.size() == trainCapacity - 1) {
			// the link is no longer blocked
			this.blockedLinksInfosToVisualize.add(event.getTime() + ";" + link.getFromNode().getCoord().getX() + ";" + link.getFromNode().getCoord().getY() + ";" + link.getToNode().getCoord().getX() + ";" + link.getToNode().getCoord().getY() + ";" + SignalGroupState.GREEN.toString());
		}

		if (touchingTrainPaths.size() == 0) {
			this.touchedLinksToVisualize.add(event.getTime() + ";" + link.getFromNode().getCoord().getX() + ";" + link.getFromNode().getCoord().getY() + ";" + link.getToNode().getCoord().getX() + ";" + link.getToNode().getCoord().getY() + ";" + SignalGroupState.GREEN.toString());
		}

		HashSet<Id<Vehicle>> touchingTrains = this.linkId2touchingTrains.getOrDefault(event.getLinkId(), new HashSet<>());
		touchingTrains.remove(event.getVehicleId());
		this.linkId2touchingTrains.put(event.getLinkId(), touchingTrains);

		if (touchingTrains.size() == 0) {
			this.touchedLinksByTrainInfosToVisualize.add(event.getTime() + ";" + link.getFromNode().getCoord().getX() + ";" + link.getFromNode().getCoord().getY() + ";" + link.getToNode().getCoord().getX() + ";" + link.getToNode().getCoord().getY() + ";" + SignalGroupState.GREEN.toString());
		}
	}

	private boolean isBlocked(Link link) {
		int trainCapacity = RailsimUtils.getTrainCapacity(link);

		int vehCount = 0;
		if (this.linkId2touchingTrainPaths.get(link.getId()) != null) {
			vehCount = this.linkId2touchingTrainPaths.get(link.getId()).size();
		}
		if (vehCount == trainCapacity) {
			return true;
		} else if (vehCount < trainCapacity) {
			return false;
		} else {
			throw new RuntimeException("More vehicles than allowed on link " + link.getId());
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		printSignalInfos(this.scenario.getConfig().controler().getOutputDirectory() + "/" + this.scenario.getConfig().controler().getRunId() + ".visTouchedLinks.csv", this.touchedLinksToVisualize);
		printSignalInfos(this.scenario.getConfig().controler().getOutputDirectory() + "/" + this.scenario.getConfig().controler().getRunId() + ".visBlockedLinks.csv", this.blockedLinksInfosToVisualize);
		printSignalInfos(this.scenario.getConfig().controler().getOutputDirectory() + "/" + this.scenario.getConfig().controler().getRunId() + ".visTouchedLinksByTrain.csv", this.touchedLinksByTrainInfosToVisualize);
		printSignalInfos(this.scenario.getConfig().controler().getOutputDirectory() + "/" + this.scenario.getConfig().controler().getRunId() + ".visTouchedLinksCount.csv", this.touchedLinksCountToVisualize);

	}

	private void printSignalInfos(String outputFile, List<String> strings) {

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);

		try {
			writer.write("time;fromX;fromY;toX;toY;state");
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

}
