/* *********************************************************************** *
 * project: org.matsim.*
 * SpaceCell.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.mobsim.flexiblecells;

public class SpaceCell implements FlexibleCell {
	
	private double headPosition;
	private final double minLength;
	private VehicleCell nextCell;
	private VehicleCell previousCell;
	
	public SpaceCell(double headPosition, double minLength) {
		this.headPosition = headPosition;
		this.minLength = minLength;
	}
	
	@Override
	public void setHeadPosition(double headPosition) {
		this.headPosition = headPosition;
	}
	
	@Override
	public double getHeadPosition() {
		return this.headPosition;
	}
	
	public final CellType getCellType() {
		return CellType.SPACE;
	}
	
	@Override
	public double getMinLength() {
		return this.minLength;
	}

	@Override
	public void setNextCell(FlexibleCell flexibleCell) {
		this.nextCell = (VehicleCell) flexibleCell;
	}
	
	@Override
	public VehicleCell getNextCell() {
		return this.nextCell;
	}

	@Override
	public void resetNextCell() {
		this.nextCell = null;
	}

	@Override
	public void setPreviousCell(FlexibleCell flexibleCell) {
		this.previousCell = (VehicleCell) flexibleCell;
	}

	@Override
	public VehicleCell getPreviousCell() {
		return this.previousCell;
	}

	@Override
	public void resetPreviousCell() {
		this.previousCell = null;
	}
	
	@Override
	public String toString() {
		return "[headposition=" + this.headPosition + "]";
	}

}
