/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.andreas.utils.ana.plans2gexf;

import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.basic.v01.IdImpl;

/**
 * Stores all information linked to that node
 * 
 * @author aneumann
 *
 */
public class GridNode {
	
	private static final Logger log = Logger.getLogger(GridNode.class);
	private Id id;
	private double xMean;
	private double yMean;
	private double nEntries = 0;
	
	private HashMap<String, Integer> actType2countMap = new HashMap<String, Integer>();
	

	public GridNode(Id id){
		this.id = id;
	}
	
	public void addActivity(Activity act){
		
		// register coord
		if (nEntries == 0) {
			xMean = act.getCoord().getX();
			yMean = act.getCoord().getY();			
		} else {
			xMean =  (this.nEntries * this.xMean + act.getCoord().getX()) / (this.nEntries + 1);
			yMean =  (this.nEntries * this.yMean + act.getCoord().getY()) / (this.nEntries + 1);
		}
		nEntries++;
		
		// register actType
		String actType = act.getType();
		if (this.actType2countMap.get(actType) == null) {
			this.actType2countMap.put(actType, new Integer(0));
		}
		
		this.actType2countMap.put(actType, new Integer(this.actType2countMap.get(actType) + 1));
	}

	public Id getId() {
		return id;
	}
	
	
	
	public double getX() {
		return xMean;
	}

	public double getY() {
		return yMean;
	}

	public static Id createGridNodeId(int x, int y){
		return new IdImpl(x + "_" + y);
	}
	
	
	
	public int getCountForAct(String act) {
		if (this.actType2countMap.get(act) == null) {
			return 0;
		} else {
			return this.actType2countMap.get(act);
		}
	}

	@Override
	public String toString() {
		StringBuffer strB = new StringBuffer();
		strB.append("Acts ");
		for (Entry<String, Integer> actEntry : this.actType2countMap.entrySet()) {
			strB.append(" | " + actEntry.getKey() + " " + actEntry.getValue().toString());
		}
		return strB.toString();
	}
	
	

}
