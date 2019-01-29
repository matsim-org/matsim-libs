/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vehicles;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 */
public interface VehicleType {
	
	public enum DoorOperationMode {serial, parallel}

	public void setDescription(String desc);

	public void setLength(double length);

	public void setWidth(double width);

	public void setMaximumVelocity(double meterPerSecond);

	public void setEngineInformation(EngineInformation currentEngineInfo);

	public void setCapacity(VehicleCapacity capacity);

	public double getWidth();

	public double getMaximumVelocity();
	
	public double getLength();
	
	public EngineInformation getEngineInformation();
	
	public String getDescription();
	
	/**
	 * Comments:<ul>
	 * <li> What happens with multi-vehicle trains?  I assume they need to be defined as a single vehicle?  kai, jul'11
	 * </ul>
	 */
	public VehicleCapacity getCapacity();
	
	public Id<VehicleType> getId();
	
	/**
	 * Comments:<ul>
	 * <li> In my understanding, ``access'' time is the time to reach the public transit system (e.g. by walking).  
	 * See, e.g., http://en.wikipedia.org/wiki/Public_Transport_Accessibility_Level .  It is thus not the same as
	 * the time for one person to enter the vehicle.  Or is there an alternative definition somewhere? kai, jul'11
	 * <li> Even if this is understood as the time per person to enter the vehicle, it needs to be clear that this is, at
	 * best, the emtpy vehicle entering time, and there may be additional functions computing longer vehicle entering times 
	 * when the vehicle is full. kai, jul'11
	 * <li> Finally, the time needs to be understood as capacitated.  I.e. when there are enough doors so that 9 passengers
	 * can enter the vehicle per second, then this time should be 1/9=0.111. kai, jul'11
	 * </ul>
	 */
	@Deprecated
	public double getAccessTime();
	
	/**
	 * See comments under getter.
	 */
	@Deprecated
	public void setAccessTime(double seconds);
	
	/**
	 * Comments:<ul>
	 * <li> In my understanding, ``egress'' time is either the time from the vehicle to the final destination
	 * (e.g. http://www.worldtransitresearch.info/research/1507/ ), or 
	 * the time that is necessary to evacuate the vehicle.  Both definitions are decidedly not the same as the 
	 * time for one person to leave the vehicle.  kai, jul'11  
	 * <li> Also see remarks under getAccessTime() .
	 * </ul>
	 */
	@Deprecated
	public double getEgressTime();
	
	/**
	 * See comments under getter.
	 */
	@Deprecated
	public void setEgressTime(double seconds);
	
	public DoorOperationMode getDoorOperationMode();
	
	public void setDoorOperationMode(DoorOperationMode mode);
	
	public double getPcuEquivalents();

	public void setPcuEquivalents(double pcuEquivalents);

    public double getFlowEfficiencyFactor();

	public void setFlowEfficiencyFactor(double flowEfficiencyFactor);
}
