/* *********************************************************************** *
 * project: org.matsim.*
 * WbUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jjoubert.projects.wb;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

/**
 * Utility methods for the World Bank project.
 * 
 * @author jwjoubert
 */
public class WbUtils {
	final private static Logger LOG = Logger.getLogger(WbUtils.class);
	
	
	protected static boolean isTravelling(Plan plan){
		boolean isTravelling = false;
		
		Iterator<PlanElement> iterator = plan.getPlanElements().iterator();
		while(!isTravelling && iterator.hasNext()){
			PlanElement pe = iterator.next();
			if(pe instanceof Leg){
				isTravelling = true;
			}
		}
		
		return isTravelling;
	}
	
	
	
	
	
	
}
