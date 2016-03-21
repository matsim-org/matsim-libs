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

import java.util.Arrays;

/**
 * Implements a memory-efficient storage for booleans, using bitmasks. Useful when storing huge amounts of booleans
 * in a setting where memory consumption becomes a problem.
 * Should consume around 8 times less memory than a boolean array.
 *
 * @author thibautd
 */
public class BooleanList {
	private int[] integer;

	private int size = 0;

	public BooleanList() {
		this.integer = new int[ 10 ];
	}

	public BooleanList( final boolean[] bs ) {
		integer = new int[ (int) Math.ceil( bs.length / 32.0 ) ];
		for ( boolean b : bs ) add( b );
	}

	public void add( final boolean b ) {
		size++;
		expand();
		set( size - 1 , b );
	}

	public boolean get( int i ) {
		return ( integer[ i / 32 ] & ( 1 << ( i - ( i / 32 ) ) ) ) != 0;
	}

	private void expand() {
		final int nIntegers = size / 32;
		if ( integer.length == nIntegers ) {
			integer = Arrays.copyOf( integer , integer.length * 2 );
		}
	}

	private void set( final int i , boolean b ) {
		if ( b ) {
			integer[ i / 32 ] |= 1 << ( i - ( i / 32 ) );
		}
		else {
			integer[ i / 32 ] &= ~(1 << (i - ( i / 32 ) ));
		}
	}

	public int size() {
		return size;
	}

	int sizeStoringArray() {
		return integer.length;
	}
}
