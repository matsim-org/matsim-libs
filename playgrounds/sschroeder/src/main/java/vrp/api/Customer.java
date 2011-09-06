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
package vrp.api;


import vrp.basics.Relation;
import vrp.basics.TimeWindow;


/**
 * 
 * @author stefan schroeder
 *
 */

public interface Customer {
	
	public abstract String getId();
	
	public abstract Node getLocation();
	
	public abstract Relation getRelation();

	public abstract void setRelation(Relation relationship);
	
	public abstract void removeRelation();
	
	public abstract boolean hasRelation();

	public abstract int getDemand();

	public abstract void setDemand(int demand);
	
	public abstract void setServiceTime(double serviceTime);

	public abstract double getServiceTime();

	public abstract void setTheoreticalTimeWindow(TimeWindow timeWindow);
	
	public abstract void setTheoreticalTimeWindow(double start, double end);

	public abstract TimeWindow getTheoreticalTimeWindow();
	
}
