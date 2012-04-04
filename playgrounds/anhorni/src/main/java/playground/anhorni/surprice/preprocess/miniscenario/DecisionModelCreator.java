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

package playground.anhorni.surprice.preprocess.miniscenario;

import org.matsim.core.population.PersonImpl;

import playground.anhorni.surprice.AgentMemory;
import playground.anhorni.surprice.DecisionModel;

public class DecisionModelCreator {
	
	public DecisionModel createDecisionModelForAgent(PersonImpl person, AgentMemory memory) {
		DecisionModel model = new DecisionModel();
		model.setMemory(memory);
		
		model.setFrequency("work", "Mon-Fri", 1);
		model.setFrequency("shop", "Mon-Fri", 0.2);
		model.setFrequency("leisure", "Mon-Fri", 0.2);
		
		model.setFrequency("work", "Sat", 0);
		model.setFrequency("shop", "Sat", 1);
		model.setFrequency("leisure", "Sat", 0);
		
		model.setFrequency("work", "Sun", 0);
		model.setFrequency("shop", "Sun", 0);
		model.setFrequency("leisure", "Sun", 1);
		
		return model;
	}
}
