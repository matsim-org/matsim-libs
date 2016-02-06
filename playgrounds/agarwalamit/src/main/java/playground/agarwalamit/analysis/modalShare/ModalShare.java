/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis.modalShare;

import java.util.SortedMap;
import java.util.SortedSet;

/**
* @author amit
*/

public interface ModalShare {
	
	public SortedSet<String> getUsedModes();
	public SortedMap<String, Integer> getModeToNumberOfLegs();
	public SortedMap<String, Double> getModeToPercentOfLegs();
	public void writeResults(String outputFile);
}


	