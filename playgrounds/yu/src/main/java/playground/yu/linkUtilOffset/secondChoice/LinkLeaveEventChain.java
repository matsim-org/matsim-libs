/* *********************************************************************** *
 * project: org.matsim.*
 * LinkLeaveEventChain.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author yu
 * 
 */
public class LinkLeaveEventChain {
	/**
	 * @param List
	 *            {@code LinkLeaveEventSection}
	 */
	private List<LinkLeaveEventSection> chain = new ArrayList<LinkLeaveEventSection>();
	private boolean afterEventsHandling = false;

	public void setAfterEventsHandling(boolean afterEventsHandling) {
		this.afterEventsHandling = afterEventsHandling;
	}

	// public void setTimeBin(int timeBin) {
	// LinkLeaveEventSection.setTimeBin(timeBin);
	// }

	public void addSection(LinkLeaveEventSection section) {
		this.chain.add(section);
	}

	@Override
	public boolean equals(Object obj) {
		if (this.afterEventsHandling) {
			if (!(obj instanceof LinkLeaveEventChain))
				return false;
			LinkLeaveEventChain llec = (LinkLeaveEventChain) obj;
			int size = this.chain.size();
			if (size != llec.chain.size())
				return false;
			else {
				for (int i = 0; i < size; i++)
					if (!this.chain.get(i).equals(llec.chain.get(i)))
						return false;

				return true;
			}
		} else {
			Logger
					.getLogger("afterEventsHandling")
					.fine(
							"The eventhandling is not finished, so that the LinkLeaveEventChain could not be complete!");
			throw new RuntimeException();
		}
	}
}
