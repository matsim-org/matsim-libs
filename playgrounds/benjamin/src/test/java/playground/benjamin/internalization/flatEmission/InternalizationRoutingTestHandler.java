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

package playground.benjamin.internalization.flatEmission;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
/**
 * @author juliakern
 *
 */
public class InternalizationRoutingTestHandler implements LinkLeaveEventHandler{

	int expectedRoad;
	boolean expectedRoadSelected;
	int actualRoadSelected;

	public InternalizationRoutingTestHandler(int expectedRoad) {
		this.expectedRoad = expectedRoad;
		this.reset(0);
	}

	@Override
	public void reset(int iteration) {
		expectedRoadSelected = false;
		actualRoadSelected = 0;

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getLinkId().equals(Id.create(expectedRoad, Link.class))){
			expectedRoadSelected = true;
			actualRoadSelected = expectedRoad;
		}
		else {
			if(event.getLinkId().equals(Id.create("9", Link.class))) 			actualRoadSelected = 9;
			else if(event.getLinkId().equals(Id.create("11", Link.class)))		actualRoadSelected = 11;
			else if(event.getLinkId().equals(Id.create("13", Link.class))) 		actualRoadSelected = 13;
			else if(event.getLinkId().equals(Id.create("14", Link.class)))		actualRoadSelected = 14;
		}
	}

	public boolean expectedRoadSelected() {
		return expectedRoadSelected;
	}

	public int getActualRoadSelected() {
		return actualRoadSelected;
	}
}
