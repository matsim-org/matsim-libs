/*******************************************************************************
 * Copyright (C) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
/**
 * 
 */
package vrp.algorithms.ruinAndRecreate.constraints;

import org.apache.log4j.Logger;

import vrp.api.Constraints;
import vrp.basics.DeliveryFromDepot;
import vrp.basics.PickupToDepot;
import vrp.basics.Tour;
import vrp.basics.TourActivity;
import vrp.basics.Vehicle;


/**
 * @author stefan schroeder
 *
 */
public class TWCapacityAndBackhaulConstraint implements Constraints {

	private Logger logger = Logger.getLogger(TWCapacityAndBackhaulConstraint.class);
	
	private int maxCap;
	
	public TWCapacityAndBackhaulConstraint(int maxCap) {
		super();
		this.maxCap = maxCap;
	}

	@Override
	public boolean judge(Tour tour) {
		int currentCap = 0;
		boolean pickupOccured = false;
		for(TourActivity tourAct : tour.getActivities()){
			if(tourAct.getCurrentLoad() > maxCap || tourAct.getCurrentLoad() < 0){
				logger.debug("capacity-conflict (maxCap=" + maxCap + ";currentCap=" + currentCap + " on tour " + tour);
				return false;
			}
			if(tourAct.hasTimeWindowConflict()){
				logger.debug("timeWindow-conflic on tour " + tour);
				return false;
			}
			if(tourAct instanceof PickupToDepot){
				pickupOccured = true;
			}
			if(tourAct instanceof DeliveryFromDepot){
				if(pickupOccured){
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean judge(Tour tour, Vehicle vehicle) {
		int currentCap = 0;
		maxCap = vehicle.getCapacity();
		boolean pickupOccured = false;
		for(TourActivity tourAct : tour.getActivities()){
			if(tourAct.getCurrentLoad() > maxCap || tourAct.getCurrentLoad() < 0){
				logger.debug("capacity-conflict (maxCap=" + maxCap + ";currentCap=" + currentCap + " on tour " + tour);
				return false;
			}
			if(tourAct.hasTimeWindowConflict()){
				logger.debug("timeWindow-conflic on tour " + tour);
				return false;
			}
			if(tourAct instanceof PickupToDepot){
				pickupOccured = true;
			}
			if(tourAct instanceof DeliveryFromDepot){
				if(pickupOccured){
					return false;
				}
			}
		}
		return true;
	}

}
