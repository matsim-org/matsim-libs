/* *********************************************************************** *
 * project: org.matsim.*
 * FhEmissions.java
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
 *                                                                         
 * *********************************************************************** */
package playground.benjamin.emissions.types;

import java.util.Map;

public class HbefaRoadTypeTrafficSituation {

	private String VISUM_RT_NAME;
	private final int HBEFA_RT_NR;
	private final Map<HbefaTrafficSituation, String> TRAFFIC_SITUATION_MAPPING;
	
	public HbefaRoadTypeTrafficSituation(int hbefaRtNr, Map<HbefaTrafficSituation, String> trafficSitMapping) {
		this.HBEFA_RT_NR = hbefaRtNr;
		this.TRAFFIC_SITUATION_MAPPING = trafficSitMapping;
	}

	public String getVISUM_RT_NAME() {
		return this.VISUM_RT_NAME;
	}
	public void setVISUM_RT_NAME(String visumRtName) {
		this.VISUM_RT_NAME = visumRtName;
	}
	public int getHBEFA_RT_NR() {
		return this.HBEFA_RT_NR;
	}
	public Map<HbefaTrafficSituation, String> getTRAFFIC_SITUATION_MAPPING() {
		return this.TRAFFIC_SITUATION_MAPPING;
	}
}
