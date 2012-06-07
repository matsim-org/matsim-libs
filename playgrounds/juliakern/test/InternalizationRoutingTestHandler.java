/* *********************************************************************** *
 * project: org.matsim.*
 * RoadUsedHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.benjamin.internalization;

import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
/**
 * @author juliakern
 *
 */
public class InternalizationRoutingTestHandler implements LinkLeaveEventHandler{

	int expectedRoad;
	boolean expectedRoadSelected;
	String actualRoadSelected;

	public InternalizationRoutingTestHandler(int expectedRoad) {
		this.expectedRoad = expectedRoad;
		expectedRoadSelected = false;
	}

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getLinkId().equals(new IdImpl(Integer.toString(expectedRoad)))){
			expectedRoadSelected = true;
		} else {
			if(event.getLinkId().equals(new IdImpl("9"))) 			actualRoadSelected = "9,";
			else if(event.getLinkId().equals(new IdImpl("11")))		actualRoadSelected = "11";
			else if(event.getLinkId().equals(new IdImpl("13"))) 	actualRoadSelected = "13";
		}
	}

	public boolean expectedRoadSelected() {
		return expectedRoadSelected;
	}

	public String getActualRoadSelected() {
		return actualRoadSelected;
	}
}
