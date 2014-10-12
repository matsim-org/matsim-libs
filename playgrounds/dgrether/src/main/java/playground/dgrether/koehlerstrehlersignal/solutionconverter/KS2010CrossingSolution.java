/* *********************************************************************** *
 * project: org.matsim.*
 * DgSolutionCrossing
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
package playground.dgrether.koehlerstrehlersignal.solutionconverter;

import java.util.HashMap;
import java.util.Map;


/**
 * Data structure for the solution of a KS2010 traffic signal optimization
 * 
 * @author dgrether
 */
public class KS2010CrossingSolution {

	private String id = null;
	private Map<String, Integer> programIdOffsetMap = new HashMap<>();
	
	public KS2010CrossingSolution(String crossingId) {
		this.id = crossingId;
	}

	public void addOffset4Program(String programId, int offsetSeconds) {
		this.programIdOffsetMap.put(programId, offsetSeconds);
	}
	
	public Map<String, Integer> getProgramIdOffsetMap(){
		return this.programIdOffsetMap;
	}

	public String getCrossingId() {
		return this.id;
	}
	
	public void setCrossingId(String id){
		this.id = id;
	}

}
