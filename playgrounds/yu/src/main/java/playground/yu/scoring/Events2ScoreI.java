/* *********************************************************************** *
 * project: org.matsim.*
 * Events2ScoreI.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.yu.scoring;

import org.matsim.core.events.handler.EventHandler;

/**
 * Interface for all of my Events2Scores
 * 
 * @author yu
 * 
 */
public interface Events2ScoreI extends EventHandler {

	void finish();

	@Override
	void reset(int iteration);

}
