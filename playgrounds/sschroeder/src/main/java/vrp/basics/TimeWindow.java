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

/**
 * 
 * @author stefan schroeder
 *
 */

public class TimeWindow {
	private double start;
	private double end;
	public TimeWindow(double start, double end) {
		super();
		this.start = start;
		this.end = end;
	}
	public double getStart() {
		return start;
	}
	
	public void setStart(double start) {
		this.start = start;
	}
	
	public void setEnd(double end) {
		this.end = end;
	}
	
	public double getEnd() {
		return end;
	}
	
	public boolean conflict(){
		if(start > end){
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "[start="+start+"][end="+end+"]";
	}
	
}
