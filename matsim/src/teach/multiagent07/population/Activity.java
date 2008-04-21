/* *********************************************************************** *
 * project: org.matsim.*
 * Activity.java
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

package teach.multiagent07.population;

import org.matsim.basic.v01.BasicActImpl;

import teach.multiagent07.net.CALink;

public class Activity extends BasicActImpl{

	public static final String HOME_TYPE = "h";
	public static final String WORK_TYPE = "w";
	public static final String LEISURE_TYPE = "l";


	public Activity(final CALink link, final String type) {
		super();
		this.link = link;
		this.type = type;
	}


	public Activity(final Activity act) {
		this.link = act.getLink();
		this.type = act.getType();
		this.endTime = act.getEndTime();
	}
}
