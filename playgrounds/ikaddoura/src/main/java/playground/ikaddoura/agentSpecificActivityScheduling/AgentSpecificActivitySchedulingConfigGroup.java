/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.agentSpecificActivityScheduling;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
* @author ikaddoura
*/

public class AgentSpecificActivitySchedulingConfigGroup extends ReflectiveConfigGroup {
	
	public static final String GROUP_NAME = "agentSpecificActivityScheduling" ;

	public AgentSpecificActivitySchedulingConfigGroup() {
		super(GROUP_NAME);
	}

	private boolean useAgentSpecificActivityScheduling = true;
	private double activityDurationBin = 3600.;
	private double tolerance = 900.;
	private boolean removeNetworkSpecificInformation = false;
	private boolean adjustPopulation = true;

	@StringGetter( "activityDurationBin" )
	public double getActivityDurationBin() {
		return activityDurationBin;
	}
	
	@StringSetter( "activityDurationBin" )
	public void setActivityDurationBin(double activityDurationBin) {
		this.activityDurationBin = activityDurationBin;
	}
	
	@StringGetter( "tolerance" )
	public double getTolerance() {
		return tolerance;
	}
	
	@StringSetter( "tolerance" )
	public void setTolerance(double tolerance) {
		this.tolerance = tolerance;
	}
	
	@StringGetter( "removeNetworkSpecificInformation" )
	public boolean isRemoveNetworkSpecificInformation() {
		return removeNetworkSpecificInformation;
	}
	
	@StringSetter( "removeNetworkSpecificInformation" )
	public void setRemoveNetworkSpecificInformation(boolean removeNetworkSpecificInformation) {
		this.removeNetworkSpecificInformation = removeNetworkSpecificInformation;
	}

	@StringGetter( "adjustPopulation" )
	public boolean isAdjustPopulation() {
		return adjustPopulation;
	}

	@StringSetter( "adjustPopulation" )
	public void setAdjustPopulation(boolean adjustPopulation) {
		this.adjustPopulation = adjustPopulation;
	}

	@StringGetter( "useAgentSpecificActivityScheduling" )
	public boolean isUseAgentSpecificActivityScheduling() {
		return useAgentSpecificActivityScheduling;
	}

	@StringSetter( "useAgentSpecificActivityScheduling" )
	public void setUseAgentSpecificActivityScheduling(boolean useAgentSpecificActivityScheduling) {
		this.useAgentSpecificActivityScheduling = useAgentSpecificActivityScheduling;
	}
	
}

