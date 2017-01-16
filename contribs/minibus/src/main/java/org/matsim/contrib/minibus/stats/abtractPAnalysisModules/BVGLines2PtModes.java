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

package org.matsim.contrib.minibus.stats.abtractPAnalysisModules;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.awt.*;
import java.util.*;
import java.util.Map.Entry;


/**
 * Set a ptMode for each line as specified by the BVG naming scheme.
 * 
 * @author aneumann
 *
 */
public final class BVGLines2PtModes implements LineId2PtMode{
	
	private final static Logger log = Logger.getLogger(BVGLines2PtModes.class);
	
	private final static String BUS = "bvg_bus";
	private final static String TRAM = "bvg_tram";
	private final static String SBAHN = "s-bahn";
	private final static String UBAHN = "u-bahn";
	private final static String OTHER = "other";
	
	private HashMap<Id<TransitLine>, String> lineId2ptMode;
	private String pIdentifier;
	
	public BVGLines2PtModes(){
		log.info("using BVG naming sheme to tag lines");
	}
	
	@Override
	public void setPtModesForEachLine(TransitSchedule transitSchedule, String pIdentifier){
		this.lineId2ptMode = new HashMap<>();
		this.pIdentifier = pIdentifier;
		
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			if (transitLine.getId().toString().contains(this.pIdentifier)) {
				this.lineId2ptMode.put(transitLine.getId(), new String(this.pIdentifier));
			} else if (transitLine.getId().toString().contains("-B-") ) {
				this.lineId2ptMode.put(transitLine.getId(), new String(BVGLines2PtModes.BUS));
			} else if (transitLine.getId().toString().contains("-T-")) {
				this.lineId2ptMode.put(transitLine.getId(), new String(BVGLines2PtModes.TRAM));
			} else if (transitLine.getId().toString().contains("SB_")) {
				this.lineId2ptMode.put(transitLine.getId(), new String(BVGLines2PtModes.SBAHN));
			} else if (transitLine.getId().toString().contains("-U-")) {
				this.lineId2ptMode.put(transitLine.getId(), new String(BVGLines2PtModes.UBAHN));
			} else {
				this.lineId2ptMode.put(transitLine.getId(), new String(BVGLines2PtModes.OTHER));
			}
		}
	}

	
	public LinkedList<String> getTypesPresentInIncreasingImportance(){
		LinkedList<String> types = new LinkedList<String>();
		if(this.lineId2ptMode.containsValue(BVGLines2PtModes.OTHER)) {
			types.add(BVGLines2PtModes.OTHER);
		}
		if(this.lineId2ptMode.containsValue(BVGLines2PtModes.SBAHN)) {
			types.add(BVGLines2PtModes.SBAHN);
		}
		if(this.lineId2ptMode.containsValue(BVGLines2PtModes.UBAHN)) {
			types.add(BVGLines2PtModes.UBAHN);
		}
		if(this.lineId2ptMode.containsValue(BVGLines2PtModes.TRAM)) {
			types.add(BVGLines2PtModes.TRAM);
		}
		if(this.lineId2ptMode.containsValue(BVGLines2PtModes.BUS)) {
			types.add(BVGLines2PtModes.BUS);
		}
		if(this.lineId2ptMode.containsValue(this.pIdentifier)) {
			types.add(this.pIdentifier);
		}
		return types;
	}
	
	@Override
	public HashMap<Id<TransitLine>, String> getLineId2ptModeMap(){
		return this.lineId2ptMode;
	}
	
	public Color getColorForLine(Id<TransitLine> lineId) {
		String lineType = this.lineId2ptMode.get(lineId);
		return this.getColorForType(lineType);
	}
	
	public Color getColorForType(String type) {
		Color color;
		if (type != null) {
			if (type.equalsIgnoreCase(BVGLines2PtModes.BUS)) {
//				color = Color.MAGENTA;
				color = new Color(149, 39, 110);
			} else if (type.equalsIgnoreCase(BVGLines2PtModes.TRAM)) {
//				color = Color.RED;
				color = new Color(190, 20, 20);
			} else if (type.equalsIgnoreCase(BVGLines2PtModes.SBAHN)) {
//				color = Color.GREEN;
				color = new Color(64, 131, 53);
			} else if (type.equalsIgnoreCase(BVGLines2PtModes.UBAHN)) {
//				color = Color.BLUE;
				color = new Color(17, 93, 145);
			} else if (type.equalsIgnoreCase(this.pIdentifier)) {
//				color = Color.CYAN;
				color = new Color(82, 141, 186);
			} else {
//				color = Color.ORANGE;
				color = new Color(243, 121, 29);
			}
		} else {
//			color = Color.YELLOW;
			color = new Color(240, 215, 34);
		}
		
		return color;
	}
	
	@Override
	public String toString() {
		
		HashMap<String, Integer> ptMode2CountMap = new HashMap<>();
		
		for (String ptMode : this.lineId2ptMode.values()) {
			if (ptMode2CountMap.get(ptMode) == null) {
				ptMode2CountMap.put(ptMode, 0);
			}
			ptMode2CountMap.put(ptMode, ptMode2CountMap.get(ptMode) + 1);
		}
		
		StringBuffer strB = new StringBuffer();
		strB.append("LineId2ptMode contains ");
		
		for (Entry<String, Integer> ptModeCountEntry : ptMode2CountMap.entrySet()) {
			strB.append(ptModeCountEntry.getValue() + " " + ptModeCountEntry.getKey() + " entries, ");
		}
		
		return strB.toString();
	}
}
