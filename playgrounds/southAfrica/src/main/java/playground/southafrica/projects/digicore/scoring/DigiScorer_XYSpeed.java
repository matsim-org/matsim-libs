/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.projects.digicore.scoring;

import org.apache.log4j.Logger;

import playground.southafrica.projects.digicore.grid.DigiGrid_XYSpeed;

/**
 * Basic interface to calculate the risk profile/score of a person, all based 
 * on the raw accelerometer records provided. The x and y-dimensions relate to
 * acceleration, while the z-axis relates to speed. To ensure the z-dimension 
 * is scaled in a useful manner, a multiplier is introduced.
 *  
 * @author jwjoubert
 */
public interface DigiScorer_XYSpeed extends DigiScorer{
	final static Logger LOG = Logger.getLogger(DigiScorer_XYZ.class);
	
	public void buildScoringModel(String filename);

	public RISK_GROUP getRiskGroup(String record);

	public void rateIndividuals(String filename, String outputFolder);

	public DigiGrid_XYSpeed getGrid();

	public void setGrid(DigiGrid_XYSpeed grid);
}
