/* *********************************************************************** *
 * project: org.matsim.*
 * FlexibleCell.java
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

public interface FlexibleCell {

	public static enum CellType {VEHICLE, SPACE};
	
	public double getMinLength();
	
	public void setNextCell(FlexibleCell flexibleCell);
	
	public FlexibleCell getNextCell();
	
	public void resetNextCell();
	
	public void setPreviousCell(FlexibleCell flexibleCell);
	
	public FlexibleCell getPreviousCell();
	
	public void resetPreviousCell();
	
	public void setHeadPosition(double headPosition);
	
	public double getHeadPosition();
	
	public CellType getCellType();
}
