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

import java.util.LinkedList;

/**
 * 
 * @author stefan schroeder
 *
 */

public class Tour {
	
	public static class Costs {
		public double time;
		public double distance;
		public double generalizedCosts;
	}
	
	private LinkedList<TourActivity> tourActivities = new LinkedList<TourActivity>();

	public Costs costs = new Costs();
	
	public LinkedList<TourActivity> getActivities() {
		return tourActivities;
	}
	
	@Override
	public String toString() {
		String tour = null;
		for(TourActivity c : tourActivities){
			if(tour == null){
				tour = "[" + c.getCustomer() + "]";
			}
			else{
				tour += "[" + c.getCustomer() + "]";
			}
		}
		return tour;
	}

	public Costs getCosts() {
		return costs;
	}

}
