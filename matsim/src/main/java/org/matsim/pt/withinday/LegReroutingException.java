/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * LegReroutingException.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2018 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.pt.withinday;

public class LegReroutingException extends RuntimeException {

	private static final long serialVersionUID = -8850525686348020056L;

	public LegReroutingException(String msg) {
		super(msg);
	}
	
	public LegReroutingException(String msg, Throwable tr) {
		super(msg, tr);
	}
	
}
