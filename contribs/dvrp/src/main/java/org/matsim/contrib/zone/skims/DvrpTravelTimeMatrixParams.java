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

package org.matsim.contrib.zone.skims;

import java.util.Map;

import javax.validation.constraints.Positive;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpTravelTimeMatrixParams extends ReflectiveConfigGroup {
	public static final String SET_NAME = "travelTimeMatrix";

	public static final String CELL_SIZE = "cellSize";
	private static final String CELL_SIZE_EXP = "size of square cells (meters) used for computing travel time matrix."
			+ " Default value is 200 m";

	@Positive
	private int cellSize = 200; //[m]

	public DvrpTravelTimeMatrixParams() {
		super(SET_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		var map = super.getComments();
		map.put(CELL_SIZE, CELL_SIZE_EXP);
		return map;
	}

	/**
	 * @return {@value #CELL_SIZE_EXP}
	 */
	@StringGetter(CELL_SIZE)
	public int getCellSize() {
		return cellSize;
	}

	/**
	 * @param cellSize {@value #CELL_SIZE_EXP}
	 */
	@StringSetter(CELL_SIZE)
	public void setCellSize(int cellSize) {
		this.cellSize = cellSize;
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		return super.createParameterSet(type);
	}
}
