/* *********************************************************************** *
 * project: org.matsim.*
 * Intervals.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.dressler.Interval;

import java.util.Iterator;

public interface IntervalsInterface <T extends Interval> {
//------------------------FIELDS----------------------------------//

	 
//------------------------------GETTER-----------------------//
		
		
		/**
		 * Finds the VertexInterval containing t in the collection
		 * @param t time
		 * @return Interval  containing t
		 */
		public T getIntervalAt(int t);
		
		/**
		 * Returns the number of stored intervals
		 * @return the number of stored intervals
		 */
		public int getSize();
		
		public int getMeasure();
		
		/**
		 * Gives the last stored Interval
		 * @return Interval with maximal lowbound
		 */
		public T getLast();
		
		
		/**
		 * Checks whether the given Interval is the last
		 * @param o EgeInterval which it test for 
		 * @return true if getLast.equals(o)
		 */
		public boolean isLast(Interval o);
		
		
		
		/**
		 * gives the next Interval with respect to the order contained 
		 * @param o should be contained
		 * @return next Interval iff o is not last and contained
		 */
		public T getNext(T o);
		
		/**
		 * 
		 * @return high bound of last interval
		 */
		public int getLastTime();

		/**
		 * 
		 */
		public Iterator<T> getIterator();
		public Iterator<T> getIteratorAt(int t);

		
	//------------------------SPLITTING--------------------------------//	
		
		/**
		 * Finds the Interval containing t and splits this at t 
		 * giving it the same flow as the flow as the original 
		 * it inserts the new Interval after the original
		 * @param t time point to split at
		 * @return the new Interval for further modification
	 	 */
		public T splitAt(int t);

		
		/**
		 * Gives a String representation of all stored Intervals
		 * @return String representation
		 */
		
		public String toString();
}
