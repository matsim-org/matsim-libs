/*******************************************************************************
 * Copyright (C) 2011 Stefan Schršder.
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
package vrp.algorithms.ruinAndRecreate.basics;

import vrp.algorithms.ruinAndRecreate.api.TourActivityStatusUpdater;
import vrp.api.Costs;
import vrp.basics.BreakActivity;
import vrp.basics.DepotDelivery;
import vrp.basics.Tour;
import vrp.basics.TourActivity;

/**
 * 
 * @author stefan schroeder
 *
 */

public class TourActivityStatusUpdaterWithTWAndBreakImpl implements TourActivityStatusUpdater{

	private Costs costs;
	
	public TourActivityStatusUpdaterWithTWAndBreakImpl(Costs costs) {
		super();
		this.costs = costs;
	}

	@Override
	public void update(Tour tour) {
		updateTimeWindowsAndLoadsAtTourActivities(tour);
	}
	
	private void updateTimeWindowsAndLoadsAtTourActivities(Tour tour) {
		reset(tour);
		TourActivity nextCustomer = null;
		TourActivity lastCustomer = null;
		double costs = 0.0;
		int loadsAtDepot = getLoadAtDepot(tour);
		int nOfCustomers = tour.getActivities().size(); 
		int j=nOfCustomers-1;
		for(int i=0;i<nOfCustomers;i++){
			if(nextCustomer == null){
				nextCustomer = tour.getActivities().get(j);
			}
			else{
				TourActivity currentAct = tour.getActivities().get(j);
				double late = Math.min(currentAct.getLatestArrTime(), nextCustomer.getLatestArrTime() - currentAct.getServiceTime() - getTime(currentAct,nextCustomer));
				currentAct.setLatestArrTime(late);
				nextCustomer = currentAct;
			}
			if(lastCustomer == null){
				lastCustomer = tour.getActivities().get(i);
				lastCustomer.setCurrentLoad(loadsAtDepot);
			}
			else{
				TourActivity currentAct = tour.getActivities().get(i);	
				double early = Math.max(currentAct.getEarliestArrTime(), lastCustomer.getEarliestArrTime() + lastCustomer.getServiceTime() + getTime(lastCustomer,currentAct));
				currentAct.setEarliestArrTime(early);
				int currentLoad = lastCustomer.getCurrentLoad() + currentAct.getCustomer().getDemand();
				currentAct.setCurrentLoad(currentLoad);
				costs += this.costs.getCost(lastCustomer.getLocation(), currentAct.getCustomer().getLocation());
				tour.costs.generalizedCosts += this.costs.getCost(lastCustomer.getLocation(), currentAct.getCustomer().getLocation());
				tour.costs.distance += this.costs.getDistance(lastCustomer.getLocation(), currentAct.getCustomer().getLocation());
				Double travelTime = this.costs.getTime(lastCustomer.getLocation(), currentAct.getCustomer().getLocation());
				tour.costs.time  += travelTime;
				double activeTravelTime = lastCustomer.getActiveTime() + travelTime;
				if(currentAct instanceof BreakActivity){
					currentAct.setActiveTime(0.0);
				}
				else{
					currentAct.setActiveTime(activeTravelTime);
				}
				lastCustomer = currentAct;
			}
			j--;
		}
	}
	
	private void reset(Tour tour) {
		tour.costs.generalizedCosts = 0.0;
		tour.costs.distance = 0.0;
		tour.costs.time = 0.0;
		
	}

	private double getTime(TourActivity act1, TourActivity act2) {
		return costs.getTime(act1.getLocation(), act2.getLocation());
	}

	private int getLoadAtDepot(Tour tour) {
		int loadAtDepot = 0;
		for(TourActivity tA : tour.getActivities()){
			if(tA instanceof DepotDelivery){
				loadAtDepot += tA.getCustomer().getDemand();
			}
		}
		return loadAtDepot*-1;
	}
}
