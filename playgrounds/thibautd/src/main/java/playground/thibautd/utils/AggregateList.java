/* *********************************************************************** *
 * project: org.matsim.*
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

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;

/**
 * @author thibautd
 */
public class AggregateList<T> extends AbstractList<T> {
	private final Collection<List<T>> values;

	public AggregateList( final Collection<List<T>> values ) {
		this.values = values;
	}

	@Override
	public T get( final int index ) {
		int currIndex = index;
		for ( List<T> l : values ) {
			if ( currIndex < l.size() ) return l.get( currIndex );
			currIndex -= l.size();
		}
		throw new IndexOutOfBoundsException(  );
	}

	@Override
	public int size() {
		return values.stream()
				.mapToInt( List::size )
				.sum();
	}
}
