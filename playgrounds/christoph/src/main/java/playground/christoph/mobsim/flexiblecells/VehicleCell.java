/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleCell.java
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

import org.matsim.core.mobsim.framework.MobsimAgent;

public class VehicleCell implements FlexibleCell {
	
	private final MobsimAgent mobsimAgent;
	private double headPosition;
	private final double minLength;
	private double speed;
	private double acceleration;
	private SpaceCell previousCell;
	private SpaceCell nextCell;
		
	public VehicleCell(MobsimAgent mobsimAgent, double headPosition, double minLength, double speed) {
		this.mobsimAgent = mobsimAgent;
		this.headPosition = headPosition;
		this.minLength = minLength;
		this.speed = speed;
	}
	
	public MobsimAgent getMobsimAgent() {
		return this.mobsimAgent;
	}
	
	public void setSpeed(double speed) {
		this.speed = speed;
	}
	
	public double getSpeed() {
		return this.speed;
	}
	
	public void setAcceleration(double acceleration) {
		this.acceleration = acceleration;
	}
	
	public double getAcceleration() {
		return this.acceleration;
	}
	
	@Override
	public void setHeadPosition(double headPosition) {
		this.headPosition = headPosition;
	}
	
	@Override
	public double getHeadPosition() {
		return this.headPosition;
	}
	
	@Override
	public final CellType getCellType() {
		return CellType.VEHICLE;
	}
	
	@Override
	public double getMinLength() {
		return this.minLength;
	}
	
	@Override
	public void setNextCell(FlexibleCell flexibleCell) {
		this.nextCell = (SpaceCell) flexibleCell;
	}

	@Override
	public SpaceCell getNextCell() {
		return this.nextCell;
	}

	@Override
	public void resetNextCell() {
		this.nextCell = null;
	}

	@Override
	public void setPreviousCell(FlexibleCell flexibleCell) {
		this.previousCell = (SpaceCell) flexibleCell;
	}

	@Override
	public SpaceCell getPreviousCell() {
		return this.previousCell;
	}

	@Override
	public void resetPreviousCell() {
		this.previousCell = null;
	}

	@Override
	public String toString() {
		return "[id=" + this.mobsimAgent.getId() + "]" +
				"[headposition=" + this.headPosition + "]" +
				"[speed=" + this.speed + "]" +
				"[acceleration=" + this.acceleration + "]";
	}
}
