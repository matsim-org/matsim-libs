/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.misc;

import java.util.Collection;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.matrices.Matrix;

import playground.johannes.sna.gis.ZoneLayer;

/**
 * @author johannes
 *
 */
public class Plans2Matrix {

	public Matrix run(Collection<Plan> plans, ZoneLayer<?> zones) {
		Matrix m = new Matrix(null, null);
		
		for(Plan plan : plans) {
			for(int i = 0; i < plan.getPlanElements().size(); i += 2) {
				
			}
		}
		
		return m;
	}
}
