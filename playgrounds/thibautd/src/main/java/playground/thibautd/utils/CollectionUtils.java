/* *********************************************************************** *
 * project: org.matsim.*
 * CollectionUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author thibautd
 */
public class CollectionUtils {
	public static <T> List<T> getRandomDistinctElements(
			final Random random,
			final List<T> list,
			final int nElements ) {
		if ( list.size() < nElements ) throw new IllegalArgumentException( "cannot sample "+nElements+" elements from "+list.size() );

		// TODO: avoid creating "bowl" collection
		final List<T> bowl = new ArrayList<T>( list );
		final List<T> sample = new ArrayList<T>( nElements );

		for ( int i=0; i < nElements; i++ ) {
			sample.add(
					bowl.remove(
						random.nextInt(
							bowl.size() ) ) );
		}

		return sample;
	}
}

