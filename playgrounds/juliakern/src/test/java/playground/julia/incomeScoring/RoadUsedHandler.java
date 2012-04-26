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

package playground.julia.incomeScoring;

import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;

public class RoadUsedHandler implements LinkLeaveEventHandler{

	static boolean mer;
	int i;
	String usedRoadVariant = new String("");
	
	//constructor
public RoadUsedHandler(int j) {
		i= j;
		mer =false;
	}

@Override
public void reset(int iteration) {
	System.out.println("reset...");
}

@Override
public void handleEvent(LinkLeaveEvent event) {
	//System.out.println("LinkId"+event.getLinkId().toString());
	if (event.getLinkId().equals(new IdImpl(Integer.toString(i)))){
		mer = true;
	}
	else{
		if(event.getLinkId().equals(new IdImpl("9"))) 	usedRoadVariant="9";
		if(event.getLinkId().equals(new IdImpl("11"))) 	usedRoadVariant="11";
		if(event.getLinkId().equals(new IdImpl("13"))) 	usedRoadVariant="13";
		
	}
}


public static boolean getWasRoadSelected() {
	return mer;
}

public static boolean getWasRoadSelectedDummyTrue(){
	return true;
}

public String getLink() {
	return usedRoadVariant;
}



}
