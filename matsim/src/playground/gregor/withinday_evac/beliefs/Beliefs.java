/* *********************************************************************** *
 * project: org.matsim.*
 * Beliefs.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.withinday_evac.beliefs;

import java.util.ArrayList;
import java.util.List;

public class Beliefs {
	
	List<Perceptor> perceptors;
	
	public Beliefs() {
		this.perceptors = new ArrayList<Perceptor>();
		this.perceptors.add(new GuidePerceptor());
		this.perceptors.add(new TurnsPerceptor());
	}

}
