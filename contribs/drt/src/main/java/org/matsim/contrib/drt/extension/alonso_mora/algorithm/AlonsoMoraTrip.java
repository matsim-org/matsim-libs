package org.matsim.contrib.drt.extension.alonso_mora.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.AlonsoMoraFunction;

/**
 * This class represents a "trip" in the sense of Alonso-Mora et al., which is a
 * combination of a vehicle and a sequence of stops to be assigned to that
 * vehicle. Note that trips must be rapidly comparable and that the class below
 * exposes optimized code for that.
 * 
 * The class furthermore holds the trip function result, which defines the cost
 * of a proposed trip.
 * 
 * @author sebhoerl
 */
public class AlonsoMoraTrip implements Comparable<AlonsoMoraTrip> {
	private final AlonsoMoraVehicle vehicle;
	private final List<AlonsoMoraRequest> requests;

	private AlonsoMoraFunction.Result result;

	public AlonsoMoraTrip(AlonsoMoraVehicle vehicle, Collection<AlonsoMoraRequest> requests,
			AlonsoMoraFunction.Result result) {
		this.result = result;
		this.vehicle = vehicle;

		this.requests = new ArrayList<>(requests);
		Collections.sort(this.requests); // Important to make comparable!
	}

	/*-
	 * Comparison is performed in two steps:
	 * (1) Compare by size (length) of the trip
	 * (2) Compare the requests of the trips one by one
	 * 
	 * Note that this assumes that the requests in the list are themselves ordered. 
	 * This means that if we hit two identical trips, we will be able to detect equivalence.
	 */
	@Override
	public int compareTo(AlonsoMoraTrip o) {
		int comparison = Integer.compare(requests.size(), o.requests.size());

		if (comparison != 0) {
			// Sizes are different
			return comparison;
		}

		for (int i = 0; i < requests.size(); i++) {
			comparison = requests.get(i).compareTo(o.requests.get(i));

			if (requests.get(i).compareTo(o.requests.get(i)) != 0) {
				// Request i is not the same
				return comparison;
			}
		}

		return comparison;
	}

	@Override
	public String toString() {
		return String.format("Trip(%s)",
				String.join(",", requests.stream().map(String::valueOf).collect(Collectors.toList())));
	}

	/*
	 * Some general getters
	 */

	public int getLength() {
		return requests.size();
	}

	public List<AlonsoMoraRequest> getRequests() {
		return requests;
	}

	public AlonsoMoraVehicle getVehicle() {
		return vehicle;
	}

	/*
	 * Set and get the trip function result.
	 */

	public void setResult(AlonsoMoraFunction.Result result) {
		this.result = result;
	}

	public AlonsoMoraFunction.Result getResult() {
		return result;
	}
}