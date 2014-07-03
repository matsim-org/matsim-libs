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
package playground.ivt.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author thibautd
 */
public class ArgParser {
	private final Switches switchFactory = new Switches();
	private final Map<Id, String> defaultValues = new LinkedHashMap<Id, String>();
	private final Map<Id, List<String>> defaultMultipleValues = new LinkedHashMap<Id, List<String>>();
	private final Set<Id> switches = new HashSet<Id>();

	private boolean locked = false;

	public void setDefaultValue( final String longName, final String shortName, final String v ) {
		checkLock();
		final Id id = switchFactory.addSwitch( longName , shortName );
		final String old = defaultValues.put( id , v );
		if ( old != null ) throw new IllegalStateException( longName+" "+shortName );
	}

	public void setDefaultValue( final String name, final String v ) {
		checkLock();
		final Id id = switchFactory.addSwitch( name );
		final String old = defaultValues.put( id , v );
		if ( old != null ) throw new IllegalStateException( name );
	}

	public void setDefaultMultipleValue( final String name, final List<String> v ) {
		checkLock();
		final Id id = switchFactory.addSwitch( name );
		final List<String> old = defaultMultipleValues.put( id , v );
		if ( old != null ) throw new IllegalStateException( name );
	}

	public void setDefaultMultipleValue(
			final String longName,
			final String shortName,
			final List<String> v ) {
		checkLock();
		final Id id = switchFactory.addSwitch( longName , shortName );
		final List<String> old = defaultMultipleValues.put( id , v );
		if ( old != null ) throw new IllegalStateException( longName+" "+shortName );
	}

	public void addSwitch( final String... names ) {
		checkLock();
		switches.add( switchFactory.addSwitch( names ) );
	}

	private void checkLock() {
		if ( locked ) {
			throw new IllegalStateException( "Cannot modify an ArgParser once parseArgs() was called" );
		}
	}

	public Args parseArgs( final String... args ) {
		locked = true;
		return new Args( args );
	}

	public class Args {
		private final String[] args;

		private Args(final String[] args) {
			this.args = args;
		}

		public String getValue(final String name) {
			final Id id = switchFactory.getSwitch( name );
			if ( id.equals( Switches.unknown ) ) throw new IllegalArgumentException( name+" not in "+switchFactory.getNames() );

			for ( int i=0; i < args.length; i++ ) {
				if ( switchFactory.getSwitch( args[ i ] ).equals( id ) ) return args[ i + 1 ];
			}
			return defaultValues.get( id );
		}

		public double getDoubleValue(final String name) {
			return Double.parseDouble( getValue( name ) );
		}

		public int getIntegerValue(final String name) {
			return Integer.parseInt( getValue( name ) );
		}

		public boolean getBooleanValue(final String name) {
			return Boolean.parseBoolean( getValue( name ) );
		}

		// "<T extends Enum<T>>" does read odd, but this is the correct phrasing.
		// This stackoverflow answer just explains why nicely:
		// http://stackoverflow.com/a/3061776
		public <T extends Enum<T>> T getEnumValue(
				final String name,
				final Class<T> type) {
			return Enum.valueOf( type , getValue( name ) );
		}

		public List<String> getValues(final String name) {
			final List<String> values = new ArrayList<String>();

			final Id id = switchFactory.getSwitch( name );
			if ( id.equals( Switches.unknown ) ) throw new IllegalArgumentException( name+" not in "+switchFactory.getNames() );

			for ( int i=0; i < args.length; i++ ) {
				if ( switchFactory.getSwitch( args[ i ] ).equals( id ) ) {
					values.add( args[ ++i ] );
				}
			}

			return values.isEmpty() ? defaultMultipleValues.get( id ) : values;
		}

		public boolean isSwitched(final String name) {
			final Id id = switchFactory.getSwitch( name );
			if ( id.equals( Switches.unknown ) ) throw new IllegalArgumentException( name+" not in "+switchFactory.getNames() );

			for ( String a : args ) {
				if ( switchFactory.getSwitch( a ).equals( id ) ) return true;
			}

			return false;
		}

		public String[] getNonSwitchedArgs() {
			final List<String> list = new ArrayList<String>();

			for ( int i=0; i < args.length; i++ ) {
				if ( defaultValues.containsKey( switchFactory.getSwitch( args[ i ] ) ) ) {
					i++;
				}
				else if ( defaultMultipleValues.containsKey( switchFactory.getSwitch( args[ i ] ) ) ) {
					i++;
				}
				else if ( !switches.contains( switchFactory.getSwitch( args[ i ] ) ) ) {
					list.add( args[ i ] );
				}
			}

			return list.toArray( new String[ list.size() ] );
		}

	}
}

class Switches {
	private final Map<String, Id> name2id = new HashMap<String, Id>();
	public static final Id unknown = new IdImpl( "unknown" );
	private int c = 0;

	public Id addSwitch( final String... names ) {
		final Id id = new IdImpl( c++ );
		for ( String n : names ) {
			final Id old = name2id.put( n , id );
			if ( old != null ) throw new IllegalStateException( old.toString() );
		}
		return id;
	}

	public Id getSwitch( final String name ) {
		final Id id = name2id.get( name );
		return id != null ? id : unknown;
	}

	public Set<String> getNames() {
		return name2id.keySet();
	}
}
