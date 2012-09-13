///* *********************************************************************** *
// * project: org.matsim.*
// * EventIDFilter.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2012 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//package playground.yu.visum.filter;
//
//import org.matsim.api.core.v01.Id;
//import org.matsim.core.api.experimental.events.Event;
//import org.matsim.core.basic.v01.IdImpl;
//import org.matsim.core.events.PersonEventImpl;
//
//public class EventIDFilter extends EventFilterA {
//	private static final Id criterion = new IdImpl(38);
//
//	@Override
//	public boolean judge(Event event) {
//		if (event instanceof PersonEventImpl) {
//			return ((PersonEventImpl) event).getPersonId().equals(criterion);
//		}
//		return false;
//	}
//}