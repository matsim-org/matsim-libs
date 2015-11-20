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

import org.apache.log4j.Logger;

/**
 * @author amit
 */

public class ListUtils {

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

	public static double doubleMean(List<Double> doubleList){
		if(doubleList==null || doubleList.isEmpty())	return 0;

		double sum = ListUtils.doubleSum(doubleList);
		return sum/doubleList.size();
	}

	public static List<Double> scalerProduct(List<Double> doubleList, double scalerFactor){
		List<Double> outList = new ArrayList<>();
		if(doubleList==null ) throw new RuntimeException("The list is null. Aborting ...");

		for(double d : doubleList){
			outList.add(scalerFactor*d);
		}
		return outList;
	}

	/**
	 * @param list1
	 * @param list2
	 * @return it will divide all the elements of list1 by the elements of list2.
	 */
	public static List<Double> divide(List<Double> list1, List<Double> list2) {
		List<Double> outList = new ArrayList<>();
		if(list1 == null || list2 == null ) throw new RuntimeException("Either of the lists is null. Aborting ...");
		else if (list1.size() != list2.size()) throw new RuntimeException("Size of the lists are not equla. Aborting ...");
		else if (list1.isEmpty() ) return outList;
		else {
			for(int ii=0; ii<list1.size(); ii++){
				double e =0;
				if(list1.get(ii) == 0. && list2.get(ii)==0.) e = 0.;
				else if(list2.get(ii)==0.) {
					Logger.getLogger(ListUtils.class).warn("Denominator is zero which should result in Inf but setting it zero. If you dont want that, modify the static method.");
					e=0;
				}
				else e = list1.get(ii) / list2.get(ii);
				if(Double.isNaN(e)) {
					System.out.println("prob.");
				}
				outList.add( e );
			}
		}
		return outList;
	}

	/**
	 * @param list1
	 * @param list2
	 * @return it will subtract all the elements of list1 by the elements of list2.
	 */
	public static List<Double> subtract(List<Double> list1, List<Double> list2) {
		List<Double> outList = new ArrayList<>();
		if(list1 == null || list2 == null ) throw new RuntimeException("Either of the lists is null. Aborting ...");
		else if (list1.isEmpty() && list2.isEmpty() ) return outList;
		else if (list1.size() != list2.size()) {
			Logger.getLogger(ListUtils.class).warn("Sizes of the lists are not equal. It will still subtract.");
			if(list1.size() > list2.size()) {
				for(int ii = list2.size(); ii < list1.size(); ii++){
					list2.set(ii, 0.);
				}
			} else {
				for(int ii = list1.size(); ii < list1.size(); ii++){
					list1.set(ii, 0.);
				}
			}
		}

		for(int ii=0; ii<list1.size(); ii++){
			outList.add( list1.get(ii) - list2.get(ii) );
		}
		return outList;
	}
}
