/* *********************************************************************** *
 * project: org.matsim.*
 * LinkLeaveEventSection.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.linkUtilOffset.secondChoice;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;

/**
 * a simple unit of LinkLeaveEvent containing only timeBin and linkId
 * informations
 * 
 * @author yu
 * 
 */
public class LinkLeaveEventSection {
	private static int timeBin = 3600;
	private int time;
	private Id linkId;

	public LinkLeaveEventSection(LinkLeaveEvent lle) {
		this.time = (int) (lle.getTime() / timeBin);
		this.linkId = lle.getLinkId();
	}

	public static void setTimeBin(int timeBin) {
		LinkLeaveEventSection.timeBin = timeBin;
	}

	public int getTime() {
		return time;
	}

	public Id getLinkId() {
		return linkId;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LinkLeaveEventSection))
			return false;

		LinkLeaveEventSection objSection = (LinkLeaveEventSection) obj;
		if (this.getTime() != objSection.getTime())
			return false;

		return this.getLinkId().equals(objSection.getLinkId());
	}
}
