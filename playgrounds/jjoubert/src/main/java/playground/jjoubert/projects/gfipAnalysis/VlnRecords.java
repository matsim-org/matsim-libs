/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.jjoubert.projects.gfipAnalysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

public class VlnRecords {
	private final static Logger LOG = Logger.getLogger(VlnRecords.class);
	private final Id id;
	
	public VlnRecords(Id id) {
		this.id = id;
	}
	
	public Id getId(){
		return this.id;
	}
	
	public static int getDayOfYear(){
		return 0;
	}
	
	

}
