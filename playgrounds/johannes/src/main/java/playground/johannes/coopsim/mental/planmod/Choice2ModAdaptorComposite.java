/* *********************************************************************** *
 * project: org.matsim.*
 * Choice2ModAdaptorComposite.java
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
package playground.johannes.coopsim.mental.planmod;

import java.util.Map;

import org.matsim.contrib.sna.util.Composite;

/**
 * @author illenberger
 *
 */
public class Choice2ModAdaptorComposite extends Composite<Choice2ModAdaptor> implements Choice2ModAdaptor {

	@Override
	public PlanModifier convert(Map<String, Object> choices) {
		PlanModifierComposite composite = new PlanModifierComposite();
		for(Choice2ModAdaptor adaptor : components)
			composite.addComponent(adaptor.convert(choices));
		
		return composite;
	}

}
