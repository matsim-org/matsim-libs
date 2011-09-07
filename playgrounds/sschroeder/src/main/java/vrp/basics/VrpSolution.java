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

import java.util.Collection;

public class VrpSolution {
	
	public final Collection<Tour> tours;
	
	private Double transportTime = null;
	
	private Double transportDistance = null;
	
	private Double transportCosts = null;

	public VrpSolution(Collection<Tour> tours) {
		super();
		this.tours = tours;
	}

	public Double getTransportTime() {
		return transportTime;
	}

	public void setTransportTime(double transportTime) {
		this.transportTime = transportTime;
	}

	public Double getTransportDistance() {
		return transportDistance;
	}

	public void setTransportDistance(double transportDistance) {
		this.transportDistance = transportDistance;
	}

	public Double getTransportCosts() {
		return transportCosts;
	}

	public void setTransportCosts(double transportCosts) {
		this.transportCosts = transportCosts;
	}
	
	
}
