/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.run;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

import java.util.Comparator;
import java.util.List;

import org.matsim.contrib.dvrp.router.AttributeBasedStopFinder;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.misc.Time;

import com.google.common.base.Verify;

/**
 * Container of the time-dependent service configurations of one DRT service. If present, only bookings with a desired
 * departure time covered by one of the configured time windows are served.
 *
 * @author nkuehnel / MOIA
 */
public final class DrtServiceConfigurationsParams extends ReflectiveConfigGroup {
	public static final String SET_NAME = "serviceConfigurations";

	public static final String SERVICE_AREA_FILE = "serviceAreaFile";
	public static final String SERVICE_AREA_ATTRIBUTE = "serviceAreaAttribute";

	@Parameter(SERVICE_AREA_FILE)
	@Comment("Path to a GIS file (shp or gpkg) holding the service areas of the service configurations. Every polygon"
			+ " names the service configurations it belongs to in the attribute given by serviceAreaAttribute"
			+ " (comma-separated, may be empty), and those names are stamped onto the stops as their 'stopNetworks'"
			+ " attribute. With operationalScheme=serviceAreaBased this file replaces drtServiceAreaShapeFile: every"
			+ " link inside any polygon becomes a stop. With stopbased it only tags the stops of the transitStopFile,"
			+ " so they do not have to be attributed by hand. Not supported for door2door.")
	@Nullable
	private String serviceAreaFile = null;

	@Parameter(SERVICE_AREA_ATTRIBUTE)
	@Comment("Name of the attribute of the serviceAreaFile polygons that names the service configurations a polygon"
			+ " belongs to. Note that column names of shapefiles are limited to 10 characters, so the default cannot"
			+ " be used there.")
	@NotBlank
	private String serviceAreaAttribute = AttributeBasedStopFinder.FACILITY_STOP_NETWORKS_ATTRIBUTE;

	public DrtServiceConfigurationsParams() {
		super(SET_NAME);
	}

	@Nullable
	public String getServiceAreaFile() {
		return serviceAreaFile;
	}

	public void setServiceAreaFile(@Nullable String serviceAreaFile) {
		this.serviceAreaFile = serviceAreaFile;
	}

	public String getServiceAreaAttribute() {
		return serviceAreaAttribute;
	}

	public void setServiceAreaAttribute(String serviceAreaAttribute) {
		this.serviceAreaAttribute = serviceAreaAttribute;
	}

	public List<DrtServiceConfigurationParams> getServiceConfigurations() {
		return getParameterSets(DrtServiceConfigurationParams.SET_NAME).stream()
				.filter(DrtServiceConfigurationParams.class::isInstance)
				.map(DrtServiceConfigurationParams.class::cast)
				.toList();
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		if (DrtServiceConfigurationParams.SET_NAME.equals(type)) {
			return new DrtServiceConfigurationParams();
		}
		throw new IllegalArgumentException("unknown set type " + type);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		List<DrtServiceConfigurationParams> serviceConfigurations = getServiceConfigurations();
		Verify.verify(!serviceConfigurations.isEmpty(), "At least one %s is required.",
				DrtServiceConfigurationParams.SET_NAME);
		Verify.verify(serviceConfigurations.stream().map(DrtServiceConfigurationParams::getServiceConfigurationName).distinct().count()
				== serviceConfigurations.size(), "Cannot have several %s with identical names.",
				DrtServiceConfigurationParams.SET_NAME);

		for (DrtServiceConfigurationParams serviceConfiguration : serviceConfigurations) {
			if (serviceConfiguration.getStartTime().isDefined() && serviceConfiguration.getEndTime().isDefined()) {
				Verify.verify(serviceConfiguration.getEndTime().seconds() > serviceConfiguration.getStartTime().seconds(),
						"endTime must be later than startTime in %s '%s'.", DrtServiceConfigurationParams.SET_NAME,
						serviceConfiguration.getServiceConfigurationName());
			}
		}

		Verify.verify(serviceConfigurations.stream().filter(s -> s.getStartTime().isUndefined()).count() <= 1,
				"At most one %s may have an undefined startTime.", DrtServiceConfigurationParams.SET_NAME);
		Verify.verify(serviceConfigurations.stream().filter(s -> s.getEndTime().isUndefined()).count() <= 1,
				"At most one %s may have an undefined endTime.", DrtServiceConfigurationParams.SET_NAME);

		// the time windows are half-open [startTime, endTime), so touching windows are fine, overlapping ones are not
		List<DrtServiceConfigurationParams> sorted = serviceConfigurations.stream()
				.sorted(Comparator.comparingDouble(s -> s.getStartTime().orElse(Double.NEGATIVE_INFINITY)))
				.toList();
		for (int i = 1; i < sorted.size(); i++) {
			DrtServiceConfigurationParams earlier = sorted.get(i - 1);
			DrtServiceConfigurationParams later = sorted.get(i);
			double earlierEnd = earlier.getEndTime().orElse(Double.POSITIVE_INFINITY);
			double laterStart = later.getStartTime().orElse(Double.NEGATIVE_INFINITY);
			Verify.verify(earlierEnd <= laterStart,
					"Time windows of %s '%s' (ending %s) and '%s' (starting %s) overlap.",
					DrtServiceConfigurationParams.SET_NAME, earlier.getServiceConfigurationName(), Time.writeTime(earlier.getEndTime()),
					later.getServiceConfigurationName(), Time.writeTime(later.getStartTime()));
		}
	}
}
