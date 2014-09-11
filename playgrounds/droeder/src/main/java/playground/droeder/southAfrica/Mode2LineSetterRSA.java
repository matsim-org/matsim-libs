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
package playground.droeder.southAfrica;



/**
 * A helper-class, necessary to use the p2-module implemented by aneumann
 * 
 * @author droeder
 *
 */
class Mode2LineSetterRSA {// implements PtMode2LineSetter {
//
//	private static final Logger log = Logger.getLogger(Mode2LineSetterRSA.class);
//	
//	private HashMap<Id, String> lineId2ptMode;
//
//	protected Mode2LineSetterRSA() {
//		log.info("enabled");
//	}
//
//	@Override
//	public void setPtModesForEachLine(TransitSchedule transitSchedule, String pIdentifier){
//		this.lineId2ptMode = new HashMap<Id, String>();
//		
//		for (TransitLine line : transitSchedule.getTransitLines().values()) {
//			if(line.getRoutes().size() < 1){
//				log.error("no routes for line " + line.getId());
//			}
////			else{
////				for(TransitRoute route: line.getRoutes().values()){
////					log.info(route.getTransportMode());
////				}
////			}
//			// there should beat least one transitRoute and all routes should have the same mode (forced by router used for RSA-scenarios)
//			String mode =line.getRoutes().values().iterator().next().getTransportMode();
//			this.lineId2ptMode.put(line.getId(), mode);
//		}
//	}
//
//	@Override
//	public HashMap<Id, String> getLineId2ptModeMap(){
//		return this.lineId2ptMode;
//	}
//	
//	@Override
//	public String toString() {
//		
//		HashMap<String, Integer> ptMode2CountMap = new HashMap<String, Integer>();
//		
//		for (String ptMode : this.lineId2ptMode.values()) {
//			if (ptMode2CountMap.get(ptMode) == null) {
//				ptMode2CountMap.put(ptMode, new Integer(0));
//			}
//			ptMode2CountMap.put(ptMode, new Integer(ptMode2CountMap.get(ptMode) + 1));
//		}
//		
//		StringBuffer strB = new StringBuffer();
//		strB.append("LineId2ptMode contains ");
//		
//		for (Entry<String, Integer> ptModeCountEntry : ptMode2CountMap.entrySet()) {
//			strB.append(ptModeCountEntry.getValue() + " " + ptModeCountEntry.getKey() + " entries, ");
//		}
//		
//		return strB.toString();
//	}
}

