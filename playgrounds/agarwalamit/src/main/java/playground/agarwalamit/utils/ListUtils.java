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

public final class ListUtils {
	
	private ListUtils(){}

	public static int intSum(final List<Integer> intList){
		if(intList==null) throw new NullPointerException("The list is null. Aborting ...");
		return intList.parallelStream().reduce(0,Integer::sum);
	}
	
	public static double intMean(final List<Integer> intList){
		if(intList.isEmpty()) return 0.;
		return intList.stream().mapToInt(i -> i).average().orElse(0.);
	}

	public static double doubleSum(final List<Double> doubleList){
		if(doubleList==null) throw new NullPointerException("The list is null. Aborting ...");
		return doubleList.parallelStream().reduce(0.0,Double::sum);
	}

	public static double doubleMean(final List<Double> doubleList){
		if(doubleList.isEmpty()) return 0.;
		return doubleList.stream().mapToDouble(i -> i).average().orElse(0.0);
	}

	public static List<Double> scalerProduct(final List<Double> doubleList, final double scalerFactor){
		if(doubleList==null ) throw new NullPointerException("The list is null. Aborting ...");

		List<Double> outList = new ArrayList<>();
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
	public static List<Double> divide(final List<Double> list1, final List<Double> list2) {
		List<Double> outList = new ArrayList<>();
		if(list1 == null || list2 == null ) throw new NullPointerException("Either of the lists is null. Aborting ...");
		else if (list1.size() != list2.size()) throw new RuntimeException("Sizes of the lists are not equal. Aborting ...");
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
	public static List<Double> subtract(final List<Double> list1, final List<Double> list2) {
		List<Double> outList = new ArrayList<>();
		if(list1 == null || list2 == null ) throw new NullPointerException("Either of the lists is null. Aborting ...");
		else if (list1.isEmpty() && list2.isEmpty() ) ;
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
