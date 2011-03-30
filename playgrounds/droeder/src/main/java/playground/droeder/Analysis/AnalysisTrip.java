/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.droeder.Analysis;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author droeder
 *
 */
public class AnalysisTrip {
		
		private SortedMap<Integer, String> values;
		private int oldSize;
		
		public AnalysisTrip(String[] oldTrip){
			this.oldSize = oldTrip.length;
			this.values = new TreeMap<Integer, String>();
			
			for(int i = 0; i < oldTrip.length; i++){
				this.values.put(i, oldTrip[i]);
			}
		}
		
		public AnalysisTrip(String[] oldTrip, boolean toRoot){
			this.values = new TreeMap<Integer, String>();
			
			for(int i = 0; i < oldTrip.length; i++){
				this.values.put(i, oldTrip[i]);
			}
			this.setPtTime(9999999, 9999999, 9999999);
			this.setCarTime(9999999);
		}
		
		public void setPtTime(double travelTime, double transitTime, double waitingTime){
			this.values.put(this.oldSize, String.valueOf(travelTime));
			this.values.put(this.oldSize+1, String.valueOf(transitTime));
			this.values.put(this.oldSize+2, String.valueOf(waitingTime));
		}
		
		public void setPtTime(String travelTime, String transitTime, String waitingTime){
			this.values.put(this.oldSize, travelTime);
			this.values.put(this.oldSize+1, transitTime);
			this.values.put(this.oldSize+2, waitingTime);
		}
		
		public void setCarTime(double travelTime){
			this.values.put(this.oldSize+3, String.valueOf(travelTime));
		}
		public void setCarTime(String travelTime){
			this.values.put(this.oldSize+3, travelTime);
		}
		
		public String getElement(int index){
			return this.values.get(index);
		}
		
		public SortedMap<Integer, String> getAll(){
			return this.values;
		}
}
