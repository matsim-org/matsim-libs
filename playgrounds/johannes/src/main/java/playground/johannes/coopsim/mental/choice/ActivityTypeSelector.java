/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityTypeSelector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.mental.choice;

import java.util.Map;

/**
 * @author illenberger
 *
 */
public class ActivityTypeSelector implements ChoiceSelector {

	public static final String KEY = "acttype";
	
	private final ChoiceSet<String> choiceSet;
	
	public ActivityTypeSelector(ChoiceSet<String> choiceSet) {
		this.choiceSet = choiceSet;
	}
	
	@Override
	public Map<String, Object> select(Map<String, Object> choices) {
		choices.put(KEY, choiceSet.randomWeightedChoice());
		return choices;
	}

}
