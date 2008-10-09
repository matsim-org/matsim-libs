/* *********************************************************************** *
 * project: org.matsim.*
 * FakeTravelTimeCost.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.dressler.ea_flow;


/**
 * Exception class for shortes path algorithems
 * @author Manuel Schneider
 */
public class BFException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1239419376L;

	public BFException() {
		// TODO Auto-generated constructor stub
	}

	public BFException(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public BFException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public BFException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

}
