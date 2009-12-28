/* *********************************************************************** *
 * project: org.matsim.*
 * TransitConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mrieser.pt.config;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Module;
import org.matsim.core.utils.misc.StringUtils;

/**
 * @author mrieser
 */
public class TransitConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "transit";

	/*package*/ static final String TRANSIT_SCHEDULE_FILE = "transitScheduleFile";
	/*pacakge*/ static final String VEHICLES_FILE = "vehiclesFile";
	/*package*/ static final String TRANSIT_MODES = "transitModes";

	private String transitScheduleFile = null;
	private String vehiclesFile = null;

	private Set<TransportMode> transitModes = Collections.unmodifiableSet(
			EnumSet.of(TransportMode.pt, TransportMode.bus, TransportMode.train, TransportMode.tram));

	public TransitConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public void addParam(final String paramName, final String value) {
		if (TRANSIT_SCHEDULE_FILE.equals(paramName)) {
			setTransitScheduleFile(value);
		} else if (VEHICLES_FILE.equals(paramName)) {
				setVehiclesFile(value);
		} else if (TRANSIT_MODES.equals(paramName)) {
			String[] parts = StringUtils.explode(value, ',');
			Set<TransportMode> tModes = EnumSet.noneOf(TransportMode.class);
			for (String part : parts) {
				String trimmed = part.trim();
				if (trimmed.length() > 0) {
					tModes.add(TransportMode.valueOf(trimmed));
				}
			}
			this.transitModes = Collections.unmodifiableSet(tModes);
		} else {
			throw new IllegalArgumentException(paramName);
		}
	}

	@Override
	public String getValue(final String paramName) {
		if (TRANSIT_SCHEDULE_FILE.equals(paramName)) {
			return getTransitScheduleFile();
		}
		if (VEHICLES_FILE.equals(paramName)) {
			return getVehiclesFile();
		}
		if (TRANSIT_MODES.equals(paramName)) {
			boolean isFirst = true;
			StringBuilder str = new StringBuilder();
			for (TransportMode mode : this.transitModes) {
				if (!isFirst) {
					str.append(',');
				}
				str.append(mode.toString());
				isFirst = false;
			}
			return str.toString();
		}
		throw new IllegalArgumentException(paramName);
	}

	@Override
	protected Map<String, String> getParams() {
		Map<String, String> params = super.getParams();
		addParameterToMap(params, TRANSIT_SCHEDULE_FILE);
		addParameterToMap(params, VEHICLES_FILE);
		addParameterToMap(params, TRANSIT_MODES);
		return params;
	}

	@Override
	protected Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(TRANSIT_SCHEDULE_FILE, "Input file containing the transit schedule to be simulated.");
		comments.put(VEHICLES_FILE, "Input file containing the vehicles used by the departures in the transit schedule.");
		comments.put(TRANSIT_MODES, "Comma-separated list of transportation modes that are handled as transit. Defaults to 'pt,bus,train,tram'.");
		return comments;
	}

	public void setTransitScheduleFile(final String filename) {
		this.transitScheduleFile = filename;
	}

	public String getTransitScheduleFile() {
		return this.transitScheduleFile;
	}

	public void setVehiclesFile(final String filename) {
		this.vehiclesFile = filename;
	}

	public String getVehiclesFile() {
		return this.vehiclesFile;
	}

	public void setTransitModes(final Set<TransportMode> modes) {
		this.transitModes = Collections.unmodifiableSet(EnumSet.copyOf(modes));
	}

	public Set<TransportMode> getTransitModes() {
		return this.transitModes;
	}

}
