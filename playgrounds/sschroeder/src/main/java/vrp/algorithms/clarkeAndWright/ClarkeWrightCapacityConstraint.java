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
package vrp.algorithms.clarkeAndWright;

import vrp.api.Constraints;
import vrp.basics.Tour;
import vrp.basics.TourActivity;
import vrp.basics.Vehicle;

/**
 * 
 * @author stefan schroeder
 *
 */

public class ClarkeWrightCapacityConstraint implements Constraints{

	private int maxCap;
	
	public ClarkeWrightCapacityConstraint(int maxCap) {
		super();
		this.maxCap = maxCap;
	}

	@Override
	public boolean judge(Tour tour) {
		int currentCap = 0;
		for(TourActivity acts : tour.getActivities()){
			currentCap += acts.getCustomer().getDemand();
		}
		if(currentCap<=maxCap && currentCap >= maxCap*-1){
			return true;
		}
		else{
			return false;
		}
	}

	@Override
	public boolean judge(Tour tour, Vehicle vehicle) {
		// TODO Auto-generated method stub
		return false;
	}

}
