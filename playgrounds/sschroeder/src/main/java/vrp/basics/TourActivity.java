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
package vrp.basics;

import vrp.api.Customer;
import vrp.api.Node;

/**
 * 
 * @author stefan schroeder
 *
 */

public class TourActivity {
		
	private Customer customer;

	private double practical_earliestArrivalTime = 0.0;
	
	private double practical_latestArrivalTime = Double.MAX_VALUE;
	
	private int currentLoad = 0; //after this activity has occured
	
	private double activeTime = 0;
	
	public String getType(){
		return "";
	}
	
	public double getActiveTime() {
		return activeTime;
	}

	public void setActiveTime(double activeTime) {
		this.activeTime = activeTime;
	}

	public TourActivity(Customer customer) {
		super();
		this.customer = customer;
	}

	public int getCurrentLoad() {
		return currentLoad;
	}

	public void setCurrentLoad(int currentLoad) {
		this.currentLoad = currentLoad;
	}
	
	public Node getLocation() {
		return customer.getLocation();
	}
	
	public Customer getCustomer(){
		return customer;
	}
	
	
	public double getServiceTime(){
		return customer.getServiceTime();
	}
	
	public void setEarliestArrTime(double early){
		practical_earliestArrivalTime = early;
	}
	
	public void setTimeWindow(double start, double end){
		practical_earliestArrivalTime = start;
		practical_latestArrivalTime = end;
	}
	
	public double getEarliestArrTime(){
		return practical_earliestArrivalTime;
	}
	
	public double getLatestArrTime(){
		return practical_latestArrivalTime;
	}
	
	public void setLatestArrTime(double late){
		practical_latestArrivalTime = late;
	}
	
	public boolean hasTimeWindowConflict(){
		if(practical_earliestArrivalTime > practical_latestArrivalTime){
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "[customer=" + customer.getId() + "][currentLoad="+currentLoad+"][theoTimeWindow=" + customer.getTheoreticalTimeWindow() + 
			"][practTimeWindow=[start=" + practical_earliestArrivalTime  + "][end=" + practical_latestArrivalTime + "]]";
	}
}
