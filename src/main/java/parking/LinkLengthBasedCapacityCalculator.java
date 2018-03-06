/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
  
package parking;

import java.util.Random;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;

public class LinkLengthBasedCapacityCalculator implements LinkParkingCapacityCalculator {

	private final Random random = MatsimRandom.getRandom();
	@Override
	public double getLinkCapacity(Link link) {

		  
          double r =  random.nextDouble();
          double capacity;
          if (link.getFreespeed()>13.8889) {
        	  capacity = 0 ;
        	  return capacity;
          }
          
          if (r <= 0.11) {
       	   capacity = Math.floor(0.65 * Math.floor(((link.getLength()-10)/3))) ;
            }
          //schraeg parken
            
          else if (r > 0.11 && r <= 0.25 ) {
       	   capacity = Math.floor(0.65 * Math.floor(((link.getLength()-10)/2.5))) ;
       	   }
          //senkrecht parken
        
          else {
       	   capacity =  Math.floor(0.65 * Math.floor(((link.getLength()-10)/5.2))) ;
          }
          // laengs parken
          if (capacity < 0) {
  		   capacity = 0 ;
			}
          
   
    
		return capacity;
	}

}
