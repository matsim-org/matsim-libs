/* *********************************************************************** *
 * project: org.matsim.*
 * MyTestXmlObject.java
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

package playground.jjoubert.Utilities;

public class MyTestXmlObject {
	private String type;
	public String getType() {
		return type;
	}

	public int getNumber() {
		return number;
	}

	public boolean isTest() {
		return test;
	}

	private int number;
	private boolean test;
	
	public MyTestXmlObject(String type, int number, boolean test){
		this.type = type;
		this.number = number;
		this.test = test;
	}
	
	@Override
	public String toString(){
		String result = "Object is of type " 
			+ this.type 
			+ ", has number " 
			+ String.valueOf(this.number)
			+ "; with test being " 
			+ String.valueOf(this.test);
		
		return result;
	}

}
