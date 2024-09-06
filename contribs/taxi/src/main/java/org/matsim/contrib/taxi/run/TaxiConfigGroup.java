/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.taxi.run;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.router.DvrpModeRoutingNetworkModule;
import org.matsim.contrib.dvrp.run.Modal;
import org.matsim.contrib.taxi.fare.TaxiFareParams;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.fifo.FifoTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.zonal.ZonalTaxiOptimizerParams;
import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.Config;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class TaxiConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets implements Modal {
	private static final Logger log = LogManager.getLogger(TaxiConfigGroup.class);

	public static final String GROUP_NAME = "taxi";

	/**
	 * @param config
	 * @return single-mode taxi config (only if there is exactly 1 taxi config group in {@link MultiModeTaxiConfigGroup}.
	 * Otherwise will fail.
	 */
	public static TaxiConfigGroup getSingleModeTaxiConfig(Config config) {
		Collection<TaxiConfigGroup> taxiConfigGroups = MultiModeTaxiConfigGroup.get(config).getModalElements();
		Preconditions.checkArgument(taxiConfigGroups.size() == 1,
				"Supported for only 1 taxi mode in the config. Number of taxi modes: %s", taxiConfigGroups.size());
		return taxiConfigGroups.iterator().next();
	}

	@Parameter
	@Comment("Mode which will be handled by PassengerEngine and VrpOptimizer (passengers'/customers' perspective)")
	@NotBlank
	public String mode = TransportMode.taxi; // travel mode (passengers'/customers' perspective)

	@Parameter
	@Comment("Limit the operation of vehicles to links (of the 'dvrp_routing'"
			+ " network) with 'allowedModes' containing this 'mode'."
			+ " For backward compatibility, the value is set to false by default"
			+ " -- this means that the vehicles are allowed to operate on all links of the 'dvrp_routing' network."
			+ " The 'dvrp_routing' is defined by DvrpConfigGroup.networkModes)")
	public boolean useModeFilteredSubnetwork = false;

	@Parameter
	@Comment("If false, the drop-off location remains unknown to the optimizer and scheduler"
			+ " until the end of pickup. False by default.")
	public boolean destinationKnown = false;

	@Parameter
	@Comment("If true, vehicles can be diverted during empty trips. Requires online tracking. False by default.")
	public boolean vehicleDiversion = false;

	@Parameter
	@Comment("Pickup duration. Must be positive.")
	@Positive
	public double pickupDuration = Double.NaN;// seconds

	@Parameter
	@Comment("Dropoff duration. Must be positive.")
	@Positive
	public double dropoffDuration = Double.NaN;// seconds

	@Parameter
	@Comment("If true, vehicles are (GPS-like) monitored while moving. This helps in getting more accurate "
			+ "estimates on the time of arrival. Online tracking is necessary for vehicle diversion. "
			+ "False by default.")
	public boolean onlineVehicleTracker = false;

	@Parameter
	@Comment("If true, the startLink is changed to last link in the current schedule, so the taxi starts the next "
			+ "day at the link where it stopped operating the day before. False by default.")
	public boolean changeStartLinkToLastLinkInSchedule = false;

	@Parameter
	@Comment("An XML file specifying the taxi fleet."
			+ " The file format according to dvrp_vehicles_v1.dtd."
			+ " If not provided, the vehicle specifications will be created from matsim vehicle file or provided via a custom binding."
			+ " See FleetModule.")
	@Nullable//it is possible to generate a FleetSpecification (instead of reading it from a file)
	public String taxisFile = null;

	@Parameter
	@Comment("If true, writes time profiles of vehicle statuses (i.e. current task type) and the number of unplanned "
			+ "requests are written to a text file (taxi_status_time_profiles) and saved as plots. "
			+ "False by default.")
	public boolean timeProfiles = false;

	@Parameter
	@Comment("If true, detailed hourly taxi stats are dumped after each iteration. False by default.")
	public boolean detailedStats = false;

	@Parameter("breakIfNotAllRequestsServed")
	@Comment("Specifies whether the simulation should interrupt if not all requests were performed when"
			+ " an interation ends. Otherwise, a warning is given. True by default.")
	public boolean breakSimulationIfNotAllRequestsServed = true;

	@Parameter
	@Comment("Number of threads used for parallel computation of paths (occupied drive tasks)."
			+ " 4-6 threads is usually enough. It's recommended to specify a higher number than that if possible"
			+ " - of course, threads will probably be not 100% busy."
			+ " Default value is the number of cores available to JVM")
	@Positive
	public int numberOfThreads = Runtime.getRuntime().availableProcessors();

	@NotNull
	private AbstractTaxiOptimizerParams taxiOptimizerParams;

	@Nullable
	private TaxiFareParams taxiFareParams;

	public TaxiConfigGroup() {
		super(GROUP_NAME);
		initSingletonParameterSets();
	}

	private void initSingletonParameterSets() {
		//optimiser params (one of: assignment, fifo, rule-based, zonal)
		addOptimizerParamsDefinition(AssignmentTaxiOptimizerParams.SET_NAME, AssignmentTaxiOptimizerParams::new);

		addOptimizerParamsDefinition(FifoTaxiOptimizerParams.SET_NAME, FifoTaxiOptimizerParams::new);

		addOptimizerParamsDefinition(RuleBasedTaxiOptimizerParams.SET_NAME, RuleBasedTaxiOptimizerParams::new);

		addOptimizerParamsDefinition(ZonalTaxiOptimizerParams.SET_NAME, ZonalTaxiOptimizerParams::new);

		//taxi fare
		addDefinition(TaxiFareParams.SET_NAME, TaxiFareParams::new, () -> taxiFareParams,
				params -> taxiFareParams = (TaxiFareParams)params);
	}

	public void addOptimizerParamsDefinition(String name, Supplier<AbstractTaxiOptimizerParams> creator) {
		addDefinition(name, creator, () -> taxiOptimizerParams,
				params -> taxiOptimizerParams = (AbstractTaxiOptimizerParams)params);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		Verify.verify(!vehicleDiversion || onlineVehicleTracker, "vehicle diversion requires online vehicle tracker");

		if (useModeFilteredSubnetwork) {
			DvrpModeRoutingNetworkModule.checkUseModeFilteredSubnetworkAllowed(config, mode);
		}
	}

	@Override
	public String getMode() {
		return mode;
	}

	public AbstractTaxiOptimizerParams getTaxiOptimizerParams() {
		return taxiOptimizerParams;
	}

	public Optional<TaxiFareParams> getTaxiFareParams() {
		return Optional.ofNullable(taxiFareParams);
	}
}
