/* *********************************************************************** *
 * project: org.matsim.*
 * OnlyEgo.java
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

import playground.johannes.socialnetworks.graph.social.SocialVertex;

import java.util.ArrayList;
import java.util.List;

/**
 * @author illenberger
 *
 */
public class OnlyEgo implements ActivityGroupGenerator {

	@Override
	public List<SocialVertex> generate(SocialVertex ego) {
		List<SocialVertex> list = new ArrayList<SocialVertex>(1);
		list.add(ego);
		return list;
	}

}
