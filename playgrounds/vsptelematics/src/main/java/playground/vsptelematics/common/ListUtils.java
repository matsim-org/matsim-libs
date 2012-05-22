/* *********************************************************************** *
 * project: org.matsim.*
 * ListUtils
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsptelematics.common;

import java.util.List;


/**
 * @author dgrether
 *
 */
public class ListUtils {

	public static double[] toArray(List<Double> list){
		double[] a = new double[list.size()];
		for (int i = 0; i < list.size(); i++){
			a[i] = list.get(i);
		}
		return a;
	}
	
}
