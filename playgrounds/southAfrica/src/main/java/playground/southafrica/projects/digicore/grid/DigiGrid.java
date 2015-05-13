/* *********************************************************************** *
 * project: org.matsim.*
 * DigiGrid.java
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

package playground.southafrica.projects.digicore.grid;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jzy3d.colors.Color;

public abstract class DigiGrid {
	final Logger LOG = Logger.getLogger(DigiGrid.class);

	/* Specify colours */
	final static Color DIGI_GREEN = new Color(147, 214, 83, 255);
	final static Color DIGI_YELLOW = new Color(248, 215, 85, 255);
	final static Color DIGI_ORANGE = new Color(250, 164, 54, 255);
	final static Color DIGI_RED = new Color(246, 43, 32, 255);
	final static Color DIGI_GRAY = new Color(100, 100, 100, 255);


	private List<Double> riskThresholds = new ArrayList<>();
	private boolean isPopulated = false;
	private boolean isRanked = false;

	public abstract void setupGrid(String filename);

	public abstract void rankGridCells();
	
	public void setRiskThresholds(List<Double> riskThresholds){
		this.riskThresholds = riskThresholds;
	}
	
	public boolean isRanked(){
		return this.isRanked;
	}
	
	public List<Double> getRiskThresholds(){
		return this.riskThresholds;
	}
	
	protected void setRanked(boolean ranked){
		this.isRanked = ranked;
	}
	
	public boolean isPopulated(){
		return this.isPopulated;
	}
	
	protected void setPopulated(boolean populated){
		this.isPopulated = populated;
	}
	
	
}
