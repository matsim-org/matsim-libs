/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.scenarios;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;

public class ShopLocationAssigner {
	private int numberOfPersons;
	private int left2Assign[];
	private int numberOfCityShoppingLocs;
	private final static Logger log = Logger.getLogger(ShopLocationAssigner.class);
	
	
	public ShopLocationAssigner(int numberOfPersons, int numberOfCityShoppingLocs) {
		this.numberOfPersons = numberOfPersons;
		this.numberOfCityShoppingLocs = numberOfCityShoppingLocs;		
		this.init();		
	}
	
	public void init() {
		this.left2Assign = new int[numberOfCityShoppingLocs];
		for (int i = 0; i < numberOfCityShoppingLocs; i++) {
			this.left2Assign[i] = (int)(numberOfPersons / numberOfCityShoppingLocs);
		}
	}
	
	public int getRandomLocationId() {
		return MatsimRandom.getRandom().nextInt(numberOfCityShoppingLocs);
	}
			
	public int getLocationId() {
		int cnt = 0;
		while (cnt < 10000) {
			int shopIndex = MatsimRandom.getRandom().nextInt(numberOfCityShoppingLocs);
			if (this.left2Assign[shopIndex] > 0) {
				this.left2Assign[shopIndex]--;
				return shopIndex;
			}
			cnt++;
		}
		if (cnt == 1000) {
			log.error("Error with location assigning");
		}
		return -1;
	}	
}
