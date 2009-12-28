/* *********************************************************************** *
 * project: org.matsim.*
 * ListUtils
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
package playground.dgrether.utils;

import java.util.ArrayList;
import java.util.List;


/**
 * @author dgrether
 *
 */
public class ListUtils {

	/**
	 * Creates a List from the arguments
	 * @param <T>
	 * @param ts
	 * @return
	 */
	public static <T> List<T> makeList(T...ts ){
		List<T> l = new ArrayList<T>();
		for (T t : ts){
			l.add(t);
		}
		return l;
	}
}
