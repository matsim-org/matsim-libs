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

import org.matsim.api.core.v01.Id;

import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgProgram;


/**
 * Data structure for the solution of a KS2010 traffic signal optimization
 * 
 * @author dgrether
 */
public class KS2010CrossingSolution {

	private Id<DgCrossing> id = null;
	private Map<Id<DgProgram>, Integer> programIdOffsetMap = new HashMap<>();
	
	public KS2010CrossingSolution(Id<DgCrossing> crossingId) {
		this.id = crossingId;
	}

	public void addOffset4Program(Id<DgProgram> programId, int offsetSeconds) {
		this.programIdOffsetMap.put(programId, offsetSeconds);
	}
	
	public Map<Id<DgProgram>, Integer> getProgramIdOffsetMap(){
		return this.programIdOffsetMap;
	}

	public Id<DgCrossing> getCrossingId() {
		return this.id;
	}
	
	public void setCrossingId(Id<DgCrossing> id){
		this.id = id;
	}

}
