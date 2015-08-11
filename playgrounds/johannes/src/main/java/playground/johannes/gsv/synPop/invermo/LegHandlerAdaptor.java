/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.invermo;

import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author johannes
 * 
 */
public class LegHandlerAdaptor implements AttributeHandler<Episode> {

	private List<LegAttributeHandler> delegates = new ArrayList<LegAttributeHandler>();

	public void addHandler(LegAttributeHandler handler) {
		delegates.add(handler);
	}

	@Override
	public void handleAttribute(Episode plan, Map<String, String> attributes) {
		for (Entry<String, String> entry : attributes.entrySet()) {
			if (ColumnKeys.validate(entry.getValue())) {
				String key = entry.getKey();
				if (key.startsWith("e")) {
					int idx = Character.getNumericValue(key.charAt(1));
					idx = idx - 1;
					while (idx > plan.getLegs().size() - 1) {
						plan.addLeg(new PlainSegment());
					}

					for (LegAttributeHandler legHandler : delegates)
						legHandler.handle(plan.getLegs().get(idx), idx, key, entry.getValue());
				}
			}
		}

	}

}
