/* *********************************************************************** *
 * project: org.matsim.*
 * CommonUtilies.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 *
 */
package playground.tnicolai.matsim4opus.utils;

import java.util.Iterator;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;

import playground.tnicolai.matsim4opus.utils.helperObjects.ZoneObject;


/**
 * @author thomas
 *
 */
public class UtilityCollection {

	/**
	 * Initializing an array with zone information like:
	 * zone id, zone coordinate (centroid) and its nearest node 
	 * 
	 * @param network
	 */
	public static ZoneObject[] assertZoneCentroid2NearestNode(final ActivityFacilitiesImpl zones, final NetworkImpl network) {
		
		assert( network != null );
		assert( zones != null );
		int numberOfZones = zones.getFacilities().values().size();
		ZoneObject zoneArray[] = new ZoneObject[numberOfZones];
		Iterator<ActivityFacility> zonesIterator = zones.getFacilities().values().iterator();

		int counter = 0;
		while( zonesIterator.hasNext() ){

			ActivityFacility zone = zonesIterator.next();
			assert (zone != null );
			assert( zone.getCoord() != null );
			Coord zoneCoordinate = zone.getCoord();
			Node networkNode = network.getNearestNode( zoneCoordinate );
			assert( networkNode != null );
				
			zoneArray[counter] = new ZoneObject(zone.getId(), zoneCoordinate, networkNode);
			counter++;
		}
		return zoneArray;
	}
	
	/**
	 * sorts a given array
	 * 
	 * @param array
	 * 
	 * @author thomas
	 */
	public static int[] ArrayQuicksort(int array[]){
	    int i;
	
	    System.out.println("Values Before the sort:\n");
	    for(i = 0; i < array.length; i++)
	      System.out.print( array[i]+"  ");
	    System.out.println();
	    quick_srt(array,0,array.length-1);
	    System.out.print("Values after the sort:\n");
	    for(i = 0; i <array.length; i++)
	      System.out.print(array[i]+"  ");
	    return array;
	}
	
	private static void quick_srt(int array[],int low, int n){
	    int lo = low;
	    int hi = n;
	    if (lo >= n) {
	      return;
	    }
	    int mid = array[(lo + hi) / 2];
	    while (lo < hi) {
	      while (lo<hi && array[lo] < mid) {
	        lo++;
	      }
	      while (lo<hi && array[hi] > mid) {
	        hi--;
	      }
	      if (lo < hi) {
	        int T = array[lo];
	        array[lo] = array[hi];
	        array[hi] = T;
	      }
	    }
	    if (hi < lo) {
	      int T = hi;
	      hi = lo;
	      lo = T;
	    }
	    quick_srt(array, low, lo);
	    quick_srt(array, lo == low ? lo+1 : lo, n);
	  }
}

