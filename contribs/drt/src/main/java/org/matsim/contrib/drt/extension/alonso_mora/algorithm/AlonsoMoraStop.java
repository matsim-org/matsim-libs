package org.matsim.contrib.drt.extension.alonso_mora.algorithm;

import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.Time;

/**
 * This class represents a stop in a sequence of instructions to a vehicle.
 * 
 * @author sebhoerl
 */
public class AlonsoMoraStop {
	public enum StopType {
		Pickup, Dropoff, Relocation
	}

	private StopType stopType;
	private Link link;
	private AlonsoMoraRequest request;
	private double time = Double.NaN;

	public AlonsoMoraStop(StopType stopType, Link link, AlonsoMoraRequest request, double time) {
		this.stopType = stopType;
		this.link = link;
		this.request = request;
		this.time = time;
	}

	public AlonsoMoraStop(StopType stopType, Link link, AlonsoMoraRequest request) {
		this.stopType = stopType;
		this.link = link;
		this.request = request;
	}

	public StopType getType() {
		return stopType;
	}

	public Link getLink() {
		return link;
	}

	public AlonsoMoraRequest getRequest() {
		return request;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

	@Override
	public String toString() {
		return String.format("Stop[%s,%s@%s,(%s)]", stopType.toString(), Time.writeTime(time), link.getId().toString(),
				request != null ? request.getDrtRequests().stream().map(r -> r.getId().toString())
						.collect(Collectors.joining(",")) : "");
	}
}
