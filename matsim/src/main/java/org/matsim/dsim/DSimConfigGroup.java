package org.matsim.dsim;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.utils.misc.Time;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Config group for distributed simulation.
 */
public class DSimConfigGroup extends ReflectiveConfigGroup {

	private final Logger log = LogManager.getLogger(DSimConfigGroup.class);

	public enum Partitioning {none, bisect, metis}

	public final static String CONFIG_MODULE_NAME = "dsim";

	@Parameter
	@Comment("Partitioning strategy for the network. Options: [none, bisect, metis], default: 'bisect'")
	private Partitioning partitioning = Partitioning.bisect;

	public Partitioning getPartitioning() {
		return partitioning;
	}

	public void setPartitioning(Partitioning partitioning) {
		this.partitioning = partitioning;
	}

	@Parameter
	@Comment("Number of threads to use for execution. If <= 0, the number of available processors is used.")
	private int threads = 0;

	public int getThreads() {
		return threads;
	}

	public void setThreads(int threads) {
		this.threads = threads;
	}

	@Parameter
	@Comment("Modes which are simulated on the network. All other modes, expect pt will be teleported. Default: car")
	private Set<String> networkModes = new HashSet<>(Set.of(TransportMode.car));

	public Set<String> getNetworkModes() {
		return networkModes;
	}

	public void setNetworkModes(Set<String> networkModes) {
		this.networkModes = networkModes;
	}

	@Parameter
	@Comment("Stuck time [s] after which a vehicle is pushed onto the next link regardless of available capacity. Default: 30")
	private double stuckTime = 30;

	public double getStuckTime() {
		return stuckTime;
	}

	public void setStuckTime(double stuckTime) {
		this.stuckTime = stuckTime;
	}

	@Parameter
	@Comment("Link dynamics determine how vehicles can overtake. Options: [FIFO, PassingQ, Seepage(not implemented)], Default: PassingQ")
	private QSimConfigGroup.LinkDynamics linkDynamics = QSimConfigGroup.LinkDynamics.PassingQ;

	public QSimConfigGroup.LinkDynamics getLinkDynamics() {
		return linkDynamics;
	}

	public void setLinkDynamics(QSimConfigGroup.LinkDynamics linkDynamics) {
		this.linkDynamics = linkDynamics;
	}

	@Parameter
	@Comment("Traffic dynamics determine how storage capacities and inflow capacities are freed. Options: [queue, kinematicWaves, withHoles (not implemented), Default: kinematicWaves")
	private QSimConfigGroup.TrafficDynamics trafficDynamics = QSimConfigGroup.TrafficDynamics.kinematicWaves;

	public QSimConfigGroup.TrafficDynamics getTrafficDynamics() {
		return trafficDynamics;
	}

	public void setTrafficDynamics(QSimConfigGroup.TrafficDynamics trafficDynamics) {
		this.trafficDynamics = trafficDynamics;
	}

	private double startTime = 0;

	public double getStartTime() {
		return startTime;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	@StringSetter(value = "startTime")
	public void setStartTimeFromString(String value) {
		startTime = Time.parseTime(value);
	}

	@StringGetter(value = "startTime")
	public String getStartTimeAsString() {
		return Time.writeTime(startTime);
	}

	private double endTime = 86400;

	public double getEndTime() {
		return endTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	@StringSetter(value = "endTime")
	public void setEndTimeFromString(String value) {
		endTime = Time.parseTime(value);
	}

	@StringGetter(value = "endTime")
	public String getEndTimeAsString() {
		return Time.writeTime(endTime);
	}

	@Parameter
	@Comment("Determines how agents can access vehicles when starting a trip. Options=[teleport, wait (not implemented), exception], Default: teleport")
	private QSimConfigGroup.VehicleBehavior vehicleBehavior = QSimConfigGroup.VehicleBehavior.teleport;

	public QSimConfigGroup.VehicleBehavior getVehicleBehavior() {
		return vehicleBehavior;
	}

	public void setVehicleBehavior(QSimConfigGroup.VehicleBehavior vehicleBehavior) {
		this.vehicleBehavior = vehicleBehavior;
	}

	public DSimConfigGroup() {
		super(CONFIG_MODULE_NAME);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		if (vehicleBehavior == QSimConfigGroup.VehicleBehavior.wait) {
			throw new IllegalArgumentException("vehicleBehavior='wait' is not implemented for DSim. Available options: [teleport, exception (default)]");
		}
		if (linkDynamics == QSimConfigGroup.LinkDynamics.SeepageQ) {
			throw new IllegalArgumentException("LinkDynamics=SeepageQ is not implemented for DSim. Available options: [FIFO, PassingQ (default)]]");
		}
		if (trafficDynamics == QSimConfigGroup.TrafficDynamics.withHoles) {
			throw new IllegalArgumentException("TrafficDynamics=withHoles is not implemented for DSim. Available options: [queue, kinematicWaves (default)]");
		}
		if (networkModes.isEmpty()) {
			log.warn("No network modes were defined. Most of the times at least car is simulated as network mode. Make sure this is intended.");
		}
	}

	@Override
	public Map<String, String> getComments() {
		var comments = super.getComments();
		comments.put("endTime", "End time of the simulation. Default: 24:00:00");
		comments.put("startTime", "Start time of the simulation. Default: 00:00:00");
		return comments;
	}
}
