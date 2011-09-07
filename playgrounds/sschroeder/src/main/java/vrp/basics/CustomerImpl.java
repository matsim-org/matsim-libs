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

public class CustomerImpl implements Customer {

	private Node locationNode;
	
	private Relation relationship;
	
	private int demand;
	
	private String id;
	
	private double serviceTime;
	
	private TimeWindow timeWindow = new TimeWindow(0.0, Double.MAX_VALUE);

	public CustomerImpl(String customerId, Node locationNode) {
		super();
		this.locationNode = locationNode;
		this.id = customerId;
	}

	@Override
	public Node getLocation() {
		return locationNode;
	}

	@Override
	public Relation getRelation() {
		return relationship;
	}

	@Override
	public void setRelation(Relation relationship) {
		this.relationship = relationship;
		
	}

	@Override
	public int getDemand() {
		return demand;
	}

	@Override
	public void setDemand(int demand) {
		this.demand = demand;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setServiceTime(double serviceTime) {
		this.serviceTime = serviceTime;
	}

	@Override
	public double getServiceTime() {
		return serviceTime;
	}

	@Override
	public void setTheoreticalTimeWindow(TimeWindow timeWindow) {
		this.timeWindow = timeWindow;
	}

	@Override
	public TimeWindow getTheoreticalTimeWindow() {
		return timeWindow;
	}

	@Override
	public void setTheoreticalTimeWindow(double start, double end) {
		this.timeWindow = VrpUtils.createTimeWindow(start, end);
	}
	
	@Override
	public String toString() {
		return "[id="+id+"][demand="+demand+"]";
	}

	@Override
	public boolean hasRelation() {
		if(relationship != null){
			return true;
		}
		return false;
	}

	@Override
	public void removeRelation() {
		this.relationship = null;
	}
	
	
}
