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
package playground.agarwalamit.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author amit
 */

public class ListUitls {

	public static int intSum(List<Integer> intList){
		if(intList==null)	return 0;

		int sum = 0;
		for(Integer i: intList) {
			sum = sum+i;
		}
		return sum;
	}
	
	public static double doubleSum(List<Double> doubleList){
		if(doubleList==null)	return 0;

		double sum = 0;
		for(Double i: doubleList) {
			sum = sum+i;
		}
		return sum;
	}
	
	/**
	 * @param list1
	 * @param list2
	 * @return it will divide all the elements of list1 by the elements of list2.
	 */
	public static List<Double> divide(List<Double> list1, List<Double> list2) {
		List<Double> outList = new ArrayList<>();
		if(list1 == null || list2 == null ) throw new RuntimeException("Either of the list is null. Aborting ...");
		else if (list1.size() != list2.size()) throw new RuntimeException("Size of the lists are not equla. Aborting ...");
		else if (list1.isEmpty() ) return outList;
		else {
			for(int ii=0; ii<list1.size(); ii++){
				outList.add( list1.get(ii)/list2.get(ii) );
			}
		}
		return null;
	}
}
