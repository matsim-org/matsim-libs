package org.matsim.contrib.ev.withinday;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import com.google.common.base.Verify;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Configuration options for within-day electric vehicle charging
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class WithinDayEvConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "withinDayEv";

	public static WithinDayEvConfigGroup get(Config config) {
		return (WithinDayEvConfigGroup) config.getModules().get(GROUP_NAME);
	}

	public WithinDayEvConfigGroup() {
		super(GROUP_NAME);
	}

	@Parameter
	@Comment("Mode for which charging of electric vehicles is simulated. Persons need to have vehicles of that type.")
	@NotBlank
	public String carMode = TransportMode.car;

	@Parameter
	@Comment("Mode that is used to move between chargers and main activities")
	@NotBlank
	public String walkMode = TransportMode.walk;

	@Parameter
	@Comment("Defines whether agents abort if no charger can be found (scoring is event is generated in any case)")
	public boolean abortAgents = false;

	@Parameter
	@Comment("Defines how long an agent will wait during a charging attempt for the vehicle to be plugged, otherwise find new charger")
	@PositiveOrZero
	public double maximumQueueTime = 300.0;

	@Parameter
	@Comment("Defines whether spontaneous charging is allowed when going on a car leg that is neither followed by an activity-based charging slot, nor already has a leg-based charging slot.")
	public boolean allowSpoantaneousCharging = false;

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		Verify.verify(config.getModules().containsKey(EvConfigGroup.GROUP_NAME),
				"Config group '" + EvConfigGroup.GROUP_NAME + "'' must be defined when using within-day ev charging.");

		EvConfigGroup evConfig = EvConfigGroup.get(config);
		Verify.verify(maximumQueueTime > evConfig.chargeTimeStep,
				"Maximum queue time should be longer than the charging engine time step, to make sure we catch an event where a vehicle gets queued or plugged");
	}
}
