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

package playground.andreas.P2.stats.abtractPAnalysisModules.lineSetter;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;


/**
 * Set a ptMode for each line as specified by the BVG naming scheme.
 * 
 * @author aneumann
 *
 */
public class BVGLines2PtModes implements PtMode2LineSetter{
	
	private final static Logger log = Logger.getLogger(BVGLines2PtModes.class);
	private HashMap<Id, String> lineId2ptMode;
	private String pIdentifier;
	
	public BVGLines2PtModes(){
		log.info("using BVG naming sheme to tag lines");
	}
	
	public void setPtModesForEachLine(TransitSchedule transitSchedule, String pIdentifier){
		this.lineId2ptMode = new HashMap<Id, String>();
		this.pIdentifier = pIdentifier;
		
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			if (transitLine.getId().toString().contains(this.pIdentifier)) {
				this.lineId2ptMode.put(transitLine.getId(), new String(this.pIdentifier));
			} else if (transitLine.getId().toString().contains("-B-") ) {
				this.lineId2ptMode.put(transitLine.getId(), new String("bvg_bus"));
			} else if (transitLine.getId().toString().contains("-T-")) {
				this.lineId2ptMode.put(transitLine.getId(), new String("bvg_tram"));
			} else if (transitLine.getId().toString().contains("SB_")) {
				this.lineId2ptMode.put(transitLine.getId(), new String("s-bahn"));
			} else if (transitLine.getId().toString().contains("-U-")) {
				this.lineId2ptMode.put(transitLine.getId(), new String("u-bahn"));
			} else {
				this.lineId2ptMode.put(transitLine.getId(), new String("other"));
			}
		}
	}

	
	public HashMap<Id, String> getLineId2ptModeMap(){
		return this.lineId2ptMode;
	}
	
	public Color getColorForLine(Id lineId) {
		String lineType = this.lineId2ptMode.get(lineId);
		Color color;
		if (lineType != null) {
			if (lineType.equalsIgnoreCase("bvg_bus")) {
//				color = Color.MAGENTA;
				color = new Color(149, 39, 110);
			} else if (lineType.equalsIgnoreCase("bvg_tram")) {
//				color = Color.RED;
				color = new Color(190, 20, 20);
			} else if (lineType.equalsIgnoreCase("s-bahn")) {
//				color = Color.GREEN;
				color = new Color(64, 131, 53);
			} else if (lineType.equalsIgnoreCase("u-bahn")) {
//				color = Color.BLUE;
				color = new Color(17, 93, 145);
			} else if (lineType.equalsIgnoreCase(this.pIdentifier)) {
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
		
		HashMap<String, Integer> ptMode2CountMap = new HashMap<String, Integer>();
		
		for (String ptMode : this.lineId2ptMode.values()) {
			if (ptMode2CountMap.get(ptMode) == null) {
				ptMode2CountMap.put(ptMode, new Integer(0));
			}
			ptMode2CountMap.put(ptMode, new Integer(ptMode2CountMap.get(ptMode) + 1));
		}
		
		StringBuffer strB = new StringBuffer();
		strB.append("LineId2ptMode contains ");
		
		for (Entry<String, Integer> ptModeCountEntry : ptMode2CountMap.entrySet()) {
			strB.append(ptModeCountEntry.getValue() + " " + ptModeCountEntry.getKey() + " entries, ");
		}
		
		return strB.toString();
	}
}
