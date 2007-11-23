/* *********************************************************************** *
 * project: org.matsim.*
 * LinkAttribute.java
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

package org.matsim.utils.vis.routervis;

import org.matsim.utils.identifiers.IdI;

public class LinkAttribute {
	private IdI id;
	private double color;
	private String msg;
	
	public LinkAttribute(IdI id){
		this.id = id;
		this.color = 0;
		this.msg = this.id.toString();
	}
	
	public void setColor(double color){
		this.color = color;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	public double getColor(){
		return this.color;
	}
	public String getMsg(){
		return this.msg;
	}
}
