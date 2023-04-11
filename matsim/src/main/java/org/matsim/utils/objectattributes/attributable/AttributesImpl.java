
/* *********************************************************************** *
 * project: org.matsim.*
 * Attributes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.utils.objectattributes.attributable;

import java.util.*;

/**
 * This class is optimized for memory footprint and query time, at the expense of insertion time.
 *
 * @author thibautd
 */
public final class AttributesImpl implements Attributes {
	// there are potentially lots of instance of this class, containing typically few mappings each.
	// to minimize memory footprint, values are stored in arrays, kept as short as possible.
	// This makes insertion costly, but query can be kept efficient even when the number of mappings
	// increases, using binary search. This should be fine, as the typical usage is to set once and
	// access often. Replacing a value is also efficient.
	//
	// In addition, as lots of classes implement Attributable, there might be a large number of empty attributes,
	// which would result in unnecessary memory overhead if each attribute would use new instances of empty arrays
	// (Which are essentially immutable objects), hence the two "empty" constants (idea from Marcel Rieser, see MATSIM-811)
	private static final String[] EMPTY_KEYS = new String[0];
	private static final Object[] EMPTY_VALUES = new Object[0];

	private String[] keys = EMPTY_KEYS;
	private Object[] values = EMPTY_VALUES;

	@Override
	public String toString() {
		StringBuilder stb = new StringBuilder() ;
		for ( int i=0; i < keys.length; i++ ) {
			String subkey = keys[ i ];
			stb.append("{ key=").append(subkey);
			stb.append("; object=").append( values[ i ].toString());
			stb.append( " }" );
		}
		return stb.toString() ;
	}

	public Object putAttribute( final String attribute, final Object value) {
		final int insertion = Arrays.binarySearch( keys , attribute );

		if ( insertion >= 0 ) {
			final Object prev = values[ insertion ];
			values[ insertion ] = value;
			return prev;
		}

		final int newIndex = -insertion - 1;

		keys = Arrays.copyOf( keys , keys.length + 1 );
		values = Arrays.copyOf( values , values.length + 1 );

		for ( int i=keys.length - 2; i >= newIndex; i-- ) {
			keys[ i + 1 ] = keys[ i ];
			values[ i + 1 ] = values[ i ];
		}

		keys[newIndex] = attribute;
		values[newIndex] = value;

		return null;
	}

	public Object getAttribute( final String attribute) {
		final int insertion = Arrays.binarySearch( keys , attribute );

		if ( insertion < 0 ) return null;

		return values[ insertion ];
	}

	public Object removeAttribute( final String attribute ) {
		final int insertion = Arrays.binarySearch( keys , attribute );

		if ( insertion < 0 ) return null;

		final Object prev = values[ insertion ];

		for ( int i=insertion; i < keys.length - 1; i++ ) {
			keys[ i ] = keys[ i + 1 ];
			values[ i ] = values[ i + 1 ];
		}

		keys = Arrays.copyOf( keys , keys.length - 1 );
		values = Arrays.copyOf( values , values.length - 1 );

		return prev;
	}

	public void clear() {
		keys = EMPTY_KEYS;
		values = EMPTY_VALUES;
	}

	/**
	 * Returns a view of the mappings stored by this object as an immutable Map. Behavior is undefined if the mappings
	 * are modified after this method was called.
	 *
	 * It is mostly provided to allow iterating through all attributes for writing to file.
	 *
	 * @return a map that represents the mappings stored in this object
	 */
	public Map<String, Object> getAsMap() {
		return new AbstractMap<String, Object>() {
			@Override
			public Set<Entry<String, Object>> entrySet() {
				return new AbstractSet<Entry<String, Object>>() {
					@Override
					public Iterator<Entry<String, Object>> iterator() {
						return new EntryIterator();
					}

					@Override
					public int size() {
						return keys.length;
					}
				};
			}
		};
	}

	public int size() {
		return keys.length;
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	private class EntryIterator implements Iterator<Map.Entry<String, Object>> {
		private int index = 0;

		@Override
		public boolean hasNext() {
			return index < keys.length;
		}

		@Override
		public Map.Entry<String, Object> next() {
			if (index >= keys.length) {
				throw new NoSuchElementException();
			}
			Map.Entry<String, Object> entry = new AbstractMap.SimpleEntry<>(keys[index], values[index]) ;
			index++;
			return entry;
		}
	}
}
