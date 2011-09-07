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

import java.util.Random;

import vrp.api.Customer;

public class BreakActivity extends TourActivity{

	private double breakLength;
	
	private int currentLoad;
	
	public BreakActivity(Customer customer, double breakLenght, int currentLoad) {
		super(customer);
		this.breakLength = breakLenght;
		this.currentLoad = currentLoad;
		super.setTimeWindow(0.0, Double.MAX_VALUE);
	}
	
	@Override
	public double getServiceTime(){
		return breakLength;
	}
	
	@Override
	public int getCurrentLoad(){
		return currentLoad;
	}
	
	@Override
	public String getType(){
		return "Break";
		
	}
}
