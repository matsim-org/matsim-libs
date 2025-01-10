package org.matsim.dsim;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;

import java.util.HashSet;
import java.util.Set;

/**
 * Config group for distributed simulation.
 */
@Getter
@Setter
@Log4j2
public class DSimConfigGroup extends ReflectiveConfigGroup {

	public enum Partitioning {none, bisect, metis}

	public final static String CONFIG_MODULE_NAME = "dsim";

	/**
	 * Create a new config group with the given number of threads.
	 */
	public static DSimConfigGroup ofThreads(int threads) {
		DSimConfigGroup config = new DSimConfigGroup();
		config.threads = threads;
		return config;
	}

	@Parameter
	@Comment("Partitioning strategy for the network. Options: [none, bisect, metis], default: 'bisect'")
	private Partitioning partitioning = Partitioning.bisect;

	@Parameter
	@Comment("Number of threads to use for execution. If <= 0, the number of available processors is used.")
	private int threads = 0;

	@Parameter
	@Comment("Modes which are simulated on the network. All other modes, expect pt will be teleported. default: Empty collection")
	private Set<String> networkModes = new HashSet<>();

	@Parameter
	@Comment("Stuck time [s] after which a vehicle is pushed onto the next link regardless of available capacity. Default: 30")
	private double stuckTime = 30;

	@Parameter
	@Comment("Link dynamics determine how vehicles can overtake. Options: [FIFO, PassingQ, Seepage(not implemented)], Default: PassingQ")
	private QSimConfigGroup.LinkDynamics linkDynamics = QSimConfigGroup.LinkDynamics.PassingQ;

	@Parameter
	@Comment("Traffic dynamics determine how storage capacities and inflow capacities are freed. Options: [queue, kinematicWaves, withHoles (not implemented), Default: kinematicWaves")
	private QSimConfigGroup.TrafficDynamics trafficDynamics = QSimConfigGroup.TrafficDynamics.kinematicWaves;

	@Comment("Start time of the simulation. Default: 00:00:00")
	private OptionalTime startTime = OptionalTime.zeroSeconds();

	@StringSetter(value = "startTime")
	public void setStartTimeFromString(String startTime) {
		this.startTime = Time.parseOptionalTime(startTime);
	}

	@StringGetter(value = "startTime")
	public String getStartTimeString() {
		return startTime.toString();
	}

	@Comment("End time of the simulation. Default: 24:00:00")
	private OptionalTime endTime = OptionalTime.defined(86400);

	@StringSetter(value = "endTime")
	public void setEndTimeFromString(String endTime) {
		this.endTime = Time.parseOptionalTime(endTime);
	}

	@StringGetter(value = "endTime")
	public String getEndTimeString() {
		return endTime.toString();
	}

	@Parameter
	@Comment("Determines how agents can access vehicles when starting a trip. Options=[teleport, wait (not implemented), exception], Default: exception")
	private QSimConfigGroup.VehicleBehavior vehicleBehavior = QSimConfigGroup.VehicleBehavior.exception;

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
}
