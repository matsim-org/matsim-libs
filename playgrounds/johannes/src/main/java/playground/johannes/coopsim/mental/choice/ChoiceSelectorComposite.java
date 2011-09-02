/* *********************************************************************** *
 * project: org.matsim.*
 * ChoiceSelectorComposite.java
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

import org.matsim.contrib.sna.util.Composite;

/**
 * @author illenberger
 *
 */
public class ChoiceSelectorComposite extends Composite<ChoiceSelector> implements ChoiceSelector {

	@Override
	public Map<String, Object> select(Map<String, Object> choices) {
		for(ChoiceSelector selector : components)
			choices = selector.select(choices);
		
		return choices;
	}

}
