package org.matsim.utils.objectattributes.attributable;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * This class is optimized for memory footprint and query time, at the expense of insertion time.
 *
 * @author thibautd
 */
public final class Attributes {
	// there are potentially lots of instance of this class, containing typically few mappings each.
	// to minimize memory footprint, values are stored in arrays, kept as short as possible.
	// This makes insertion costly, but query can be kept efficient even when the number of mappings
	// increases, using binary search. This should be fine, as the typical usage is to set once and
	// access often. Replacing a value is also efficient.
	String[] keys = new String[0];
	Object[] values = new Object[0];

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
		keys = new String[ 0 ];
		values = new Object[ 0 ];
	}

	int size() {
		return keys.length;
	}

	public String[] getKeys() {
		return keys;
	}
}
