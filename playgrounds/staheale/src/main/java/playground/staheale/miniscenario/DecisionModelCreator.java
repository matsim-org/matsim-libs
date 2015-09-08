/* *********************************************************************** *
 * project: org.matsim.*
 * DecisionModelCreator.java
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

package playground.staheale.miniscenario;

import org.matsim.api.core.v01.population.Person;

import playground.staheale.preprocess.AgentMemory;
import playground.staheale.preprocess.DecisionModel;

public class DecisionModelCreator {
	
	public DecisionModel createDecisionModelForAgent(Person person, AgentMemory memory) {
		DecisionModel model = new DecisionModel();
		model.setMemory(memory);
		
		model.setFrequency("work", "mon-fri", 1);
		model.setFrequency("shop_retail", "mon-fri", 0.4);
		model.setFrequency("shop_service", "mon-fri", 0.1);
		model.setFrequency("leisure_sports_fun", "mon-fri", 0.1);
		model.setFrequency("leisure_gastro_culture", "mon-fri", 0.1);

		
		model.setFrequency("work", "sat", 0);
		model.setFrequency("shop_retail", "sat", 1);
		model.setFrequency("shop_service", "sat", 0.5);
		model.setFrequency("leisure_sports_fun", "sat", 0.5);
		model.setFrequency("leisure_gastro_culture", "sat", 0.5);
		
		model.setFrequency("work", "sun", 0);
		model.setFrequency("shop_retail", "sun", 0);
		model.setFrequency("shop_service", "sun", 0);
		model.setFrequency("leisure_sports_fun", "sun", 1);
		model.setFrequency("leisure_gastro_culture", "sun", 1);
		
		return model;
	}
}
