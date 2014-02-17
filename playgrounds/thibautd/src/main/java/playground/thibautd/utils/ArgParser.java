/* *********************************************************************** *
 * project: org.matsim.*
 * ArgParser.java
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
package playground.thibautd.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author thibautd
 */
public class ArgParser {
	private final Map<String, String> defaultValues = new LinkedHashMap<String, String>();
	private final Set<String> switches = new HashSet<String>();

	private final String[] args;

	public ArgParser(final String[] args) {
		this.args = args;
	}

	public void setDefaultValue( final String name, final String v ) {
		defaultValues.put( name , v );
	}

	public void addSwitch( final String name ) {
		switches.add( name );
	}

	public String getValue(final String name) {
		if ( !defaultValues.containsKey( name ) ) throw new IllegalArgumentException( name+" not in "+defaultValues.keySet() );
		for ( int i=0; i < args.length; i++ ) {
			if ( args[ i ].equals( name ) ) return args[ i + 1 ];
		}
		return defaultValues.get( name );
	}

	public boolean isSwitched(final String name) {
		if ( !switches.contains( name ) ) throw new IllegalArgumentException( name+" not in "+switches );
		for ( String a : args ) {
			if ( a.equals( name ) ) return true;
		}
		return false;
	}

	public String[] getNonSwitchedArgs() {
		final List<String> list = new ArrayList<String>();

		for ( int i=0; i < args.length; i++ ) {
			if ( defaultValues.containsKey( args[ i ] ) ) {
				i++;
			}
			else if ( !switches.contains( args[ i ] ) ) {
				list.add( args[ i ] );
			}
		}

		return list.toArray( new String[ list.size() ] );
	}
}

