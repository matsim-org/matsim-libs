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

package org.matsim.contrib.etaxi.util;

import com.google.common.collect.ImmutableList;
import one.util.streamex.StreamEx;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.etaxi.util.ETaxiStats.ETaxiState;
import org.matsim.contrib.ev.charging.ChargingEventSequenceCollector.ChargingSequence;
import org.matsim.contrib.taxi.util.stats.DurationStats;
import org.matsim.contrib.taxi.util.stats.DurationStats.State;
import org.matsim.contrib.taxi.util.stats.TaxiStatsCalculator;
import org.matsim.contrib.taxi.util.stats.TimeBinSample;
import org.matsim.contrib.taxi.util.stats.TimeBinSamples;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.matsim.contrib.taxi.util.stats.DurationStats.stateDurationByTimeBinAndState;

public class ETaxiStatsCalculator {
	private final SortedMap<Integer, ETaxiStats> hourlyEStats = new TreeMap<>();
	private final ETaxiStats dailyEStats = new ETaxiStats(TaxiStatsCalculator.DAILY_STATS_ID);

	public ETaxiStatsCalculator(List<ChargingSequence> chargingSequences, FleetSpecification fleetSpecification) {
		for (ChargingSequence sequence : chargingSequences) {
			//only calculate stats for EVs belonging to a given mode/fleet
			var vehicleId = Id.create(sequence.getChargingStart().orElseThrow().getVehicleId(), DvrpVehicle.class);
			if (!fleetSpecification.getVehicleSpecifications().containsKey(vehicleId)) {
				continue;
			}

			stateDurationByTimeBinAndState(getStateSampleStream(sequence, 3600), 3600).forEach(
					(hour, stateDurations) -> updateStateDurations(getHourlyStats(hour), stateDurations));

			Map<ETaxiState, Double> dailyStateDuration = DurationStats.stateDurationByTimeBinAndState(
							getStateSampleStream(sequence, Integer.MAX_VALUE), Integer.MAX_VALUE)
					.entrySet()
					.iterator()
					.next()
					.getValue();
			updateStateDurations(dailyEStats, dailyStateDuration);
		}
	}

	public ImmutableList<ETaxiStats> getETaxiStats() {
		return ImmutableList.<ETaxiStats>builder().addAll(hourlyEStats.values()).add(dailyEStats).build();
	}

	public ETaxiStats getDailyEStats() {
		return dailyEStats;
	}

	private Stream<TimeBinSample<State<ETaxiState>>> getStateSampleStream(ChargingSequence seq, int binSize) {
		var pluggedState = new State<>(ETaxiState.PLUGGED, seq.getChargingStart().orElseThrow().getTime(),
				seq.getChargingEnd().orElseThrow().getTime());
		var states = StreamEx.of(pluggedState);

		//optionally (only if queueing occurred)
		seq.getQueuedAtCharger()
				.map(e -> new State<>(ETaxiState.QUEUED, e.getTime(), pluggedState.beginTime()))
				.ifPresent(states::prepend);

		return states.flatMap(eTaxiStateState -> TimeBinSamples.stateSamples(eTaxiStateState, binSize));
	}

	private ETaxiStats getHourlyStats(int hour) {
		return hourlyEStats.computeIfAbsent(hour, h -> new ETaxiStats(h + ""));
	}

	private static void updateStateDurations(ETaxiStats stats, Map<ETaxiState, Double> stateDurations) {
		stateDurations.forEach((state, duration) -> stats.stateDurations.merge(state, duration, Double::sum));
	}
}
