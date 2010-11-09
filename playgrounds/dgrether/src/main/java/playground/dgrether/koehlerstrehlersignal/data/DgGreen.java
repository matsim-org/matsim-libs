/* *********************************************************************** *
 * project: org.matsim.*
 * DgGreen
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
package playground.dgrether.koehlerstrehlersignal.data;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class DgGreen {

	private Id lightId;
	private int offset;
	private int length;
	
	public DgGreen(Id lightId){
		this.lightId = lightId;
	}
	
	public Id getLightId() {
		return this.lightId;
	}

	
	public int getOffset() {
		return offset;
	}

	
	public void setOffset(int offset) {
		this.offset = offset;
	}

	
	public int getLength() {
		return length;
	}

	
	public void setLength(int length) {
		this.length = length;
	}

}
