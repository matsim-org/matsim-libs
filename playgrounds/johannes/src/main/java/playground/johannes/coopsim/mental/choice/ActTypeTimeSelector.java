/* *********************************************************************** *
 * project: org.matsim.*
 * ArrivalTimeTypeSelector.java
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

import java.util.HashMap;
import java.util.Map;

/**
 * @author illenberger
 *
 */
public class ActTypeTimeSelector implements ChoiceSelector {

	private final Map<String, TimeSelector> selectors = new HashMap<String, TimeSelector>();
	
	public void addSelector(String type, TimeSelector selector) {
		selectors.put(type, selector);
	}
	
	@Override
	public Map<String, Object> select(Map<String, Object> choices) {
		String type = (String) choices.get(ActivityTypeSelector.KEY);

		TimeSelector selector = selectors.get(type);
		
		return selector.select(choices);
	}

}
