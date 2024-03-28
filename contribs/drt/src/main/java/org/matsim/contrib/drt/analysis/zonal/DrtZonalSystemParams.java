/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.analysis.zonal;

import javax.annotation.Nullable;

import org.matsim.contrib.common.zones.h3.H3Utils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import com.google.common.base.Preconditions;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtZonalSystemParams extends ReflectiveConfigGroup {
	public static final String SET_NAME = "zonalSystem";

	public DrtZonalSystemParams() {
		super(SET_NAME);
	}

	public enum ZoneGeneration {GridFromNetwork, ShapeFile, H3}

	@Parameter
	@Comment("Logic for generation of zones for the DRT zonal system. Value can be: [GridFromNetwork, ShapeFile, H3].")
	@NotNull
	public ZoneGeneration zonesGeneration = null;

	@Parameter
	@Comment("size of square cells used for demand aggregation."
			+ " Depends on demand, supply and network. Often used with values in the range of 500 - 2000 m")
	@Nullable
	@Positive
	public Double cellSize = null;// [m]

	@Parameter
	@Comment("allows to configure zones. Used with zonesGeneration=ShapeFile")
	@Nullable
	public String zonesShapeFile = null;

	@Parameter
	@Comment("allows to configure H3 hexagonal zones. Used with zonesGeneration=H3. " +
		"Range from 0 (122 cells worldwide) to 15 (569 E^12 cells). " +
		"Usually meaningful between resolution 6 (3.7 km avg edge length) " +
		"and 10 (70 m avg edge length). ")
	@Nullable
	public Integer h3Resolution = null;

	public enum TargetLinkSelection {random, mostCentral}

	@Parameter("zoneTargetLinkSelection")
	@Comment("Defines how the target link of a zone is determined (e.g. for rebalancing)."
			+ " Possible values are [random,mostCentral]. Default behavior is mostCentral, where all vehicles are sent to the same link.")
	@NotNull
	public TargetLinkSelection targetLinkSelection = TargetLinkSelection.mostCentral;

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		Preconditions.checkArgument(zonesGeneration != ZoneGeneration.GridFromNetwork || cellSize != null,
				"cellSize must not be null when zonesGeneration is " + ZoneGeneration.GridFromNetwork);
		Preconditions.checkArgument(zonesGeneration != ZoneGeneration.ShapeFile || zonesShapeFile != null,
				"zonesShapeFile must not be null when zonesGeneration is " + ZoneGeneration.ShapeFile);
		Preconditions.checkArgument(zonesGeneration != ZoneGeneration.H3 || h3Resolution != null,
				"H3 resolution must not be null when zonesGeneration is " + ZoneGeneration.H3);
		Preconditions.checkArgument(h3Resolution == null || h3Resolution >= 0 && h3Resolution <= H3Utils.MAX_RES,
				"H3 resolution must not be null when zonesGeneration is " + ZoneGeneration.H3);
	}
}
