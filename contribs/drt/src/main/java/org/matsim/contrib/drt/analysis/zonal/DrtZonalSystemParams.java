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

import java.net.URL;
import java.util.Map;

import javax.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import com.google.common.base.Preconditions;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtZonalSystemParams extends ReflectiveConfigGroup {
	public static final String SET_NAME = "zonalSystem";

	public DrtZonalSystemParams() {
		super(SET_NAME);
	}

	public enum TargetLinkSelection {random, mostCentral}

	public enum ZoneGeneration {GridFromNetwork, ShapeFile}

	public static final String ZONES_GENERATION = "zonesGeneration";
	private static final String ZONES_GENERATION_EXP = "Logic for generation of zones for the DRT zonal system."
			+ " Value can be: [GridFromNetwork, ShapeFile].";

	public static final String CELL_SIZE = "cellSize";
	private static final String CELL_SIZE_EXP = "size of square cells used for demand aggregation."
			+ " Depends on demand, supply and network. Often used with values in the range of 500 - 2000 m";

	public static final String ZONES_SHAPE_FILE = "zonesShapeFile";
	private static final String ZONES_SHAPE_FILE_EXP = "allows to configure zones."
			+ " Used with zonesGeneration=ShapeFile";

	public static final String ZONE_TARGET_LINK_SELECTION = "zoneTargetLinkSelection";
	static final String ZONE_TARGET_LINK_SELECTION_EXP = "Defines how the target link of a zone is determined (e.g. for rebalancing)."
			+ " Possible values are [random,mostCentral]. Default behavior is mostCentral, where all vehicles are sent to the same link.";

	@NotNull
	private ZoneGeneration zonesGeneration = null;

	@Nullable
	@Positive
	private Double cellSize = null;// [m]

	@Nullable
	private String zonesShapeFile = null;

	@NotNull
	private TargetLinkSelection targetLinkSelection = TargetLinkSelection.mostCentral;

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		Preconditions.checkArgument(zonesGeneration != ZoneGeneration.GridFromNetwork || cellSize != null,
				CELL_SIZE + " must not be null when " + ZONES_GENERATION + " is " + ZoneGeneration.GridFromNetwork);
		Preconditions.checkArgument(zonesGeneration != ZoneGeneration.ShapeFile || zonesShapeFile != null,
				ZONES_SHAPE_FILE + " must not be null when " + ZONES_GENERATION + " is " + ZoneGeneration.ShapeFile);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(ZONES_GENERATION, ZONES_GENERATION_EXP);
		map.put(CELL_SIZE, CELL_SIZE_EXP);
		map.put(ZONES_SHAPE_FILE, ZONES_SHAPE_FILE_EXP);
		return map;
	}

	/**
	 * @return -- {@value #CELL_SIZE_EXP}
	 */
	@StringGetter(CELL_SIZE)
	public Double getCellSize() {
		return cellSize;
	}

	/**
	 * @param cellSize -- {@value #CELL_SIZE_EXP}
	 */
	@StringSetter(CELL_SIZE)
	public void setCellSize(Double cellSize) {
		this.cellSize = cellSize;
	}

	/**
	 * @return -- {@value #ZONES_GENERATION_EXP}
	 */
	@StringGetter(ZONES_GENERATION)
	public ZoneGeneration getZonesGeneration() {
		return zonesGeneration;
	}

	/**
	 * @param zonesGeneration -- {@value #ZONES_GENERATION_EXP}
	 */
	@StringSetter(ZONES_GENERATION)
	public void setZonesGeneration(ZoneGeneration zonesGeneration) {
		this.zonesGeneration = zonesGeneration;
	}

	/**
	 * @return {@link #ZONES_SHAPE_FILE_EXP}
	 */
	@StringGetter(ZONES_SHAPE_FILE)
	public String getZonesShapeFile() {
		return zonesShapeFile;
	}

	public URL getZonesShapeFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, zonesShapeFile);
	}

	/**
	 * @param zonesShapeFile -- {@link #ZONES_SHAPE_FILE_EXP}
	 */
	@StringSetter(ZONES_SHAPE_FILE)
	public void setZonesShapeFile(String zonesShapeFile) {
		this.zonesShapeFile = zonesShapeFile;
	}

	/**
	 * @return -- {@value #ZONE_TARGET_LINK_SELECTION_EXP}
	 */
	@StringGetter(ZONE_TARGET_LINK_SELECTION)
	public TargetLinkSelection getTargetLinkSelection() { return targetLinkSelection; }

	/**
	 * @param targetLinkSelection -- {@value #ZONE_TARGET_LINK_SELECTION_EXP}
	 */
	@StringSetter(ZONE_TARGET_LINK_SELECTION)
	public void setTargetLinkSelection(TargetLinkSelection targetLinkSelection) { this.targetLinkSelection = targetLinkSelection; }
}
