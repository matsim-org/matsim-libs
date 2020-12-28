/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.vsp.flowEfficiency;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency.FlowEfficiencyCalculator;
import org.matsim.lanes.Lane;

/**
 * calculate the situational impact on the flow efficiency of {@code qVehicle}. The result is multiplied with other impacts and
 * the base vlow efficiency value of the vehicle type. See {@link HierarchicalFlowEfficiencyCalculator}.
 *
 * @author tschlenther
**/
public interface SituationalFlowEfficiencyImpact extends FlowEfficiencyCalculator {
	boolean isFinalImpact(QVehicle qVehicle, QVehicle previousQVehicle, Double previousTimeDiff, Link link, Id<Lane> laneId);
}
