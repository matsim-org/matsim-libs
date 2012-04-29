/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.choiceSetGeneration.io;

import java.util.List;

import org.apache.log4j.Logger;

import playground.anhorni.choiceSetGeneration.helper.ChoiceSet;

public abstract class CSWriter {
		
	private final static Logger log = Logger.getLogger(CSWriter.class);
	
	public abstract void write(String outdir, String name, List<ChoiceSet> choiceSets);
	
	public CSWriter() {
	}
	
	protected boolean checkBeforeWriting(List<ChoiceSet> choiceSets) {
		if (choiceSets == null) {
			log.error("No choice set defined");
			return false;
		}
		if (choiceSets.size() == 0) {
			log.info("Empty choice set");
			return false;
		}		
		return true;
	}
}
