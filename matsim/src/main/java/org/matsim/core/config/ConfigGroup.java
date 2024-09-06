/* *********************************************************************** *
 * project: org.matsim.*
 * Module.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.config;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import jakarta.validation.Valid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.api.internal.MatsimExtensionPoint;
import org.matsim.core.utils.io.IOUtils;

/**
 * Implements a generic config-group that stores all parameters in a simple Map.
 *
 * @author mrieser
 * @author balmermi
 */
public class ConfigGroup implements MatsimExtensionPoint {
	// this cannot be made final since many actual groups inherit from it

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final String name;
	private final TreeMap<String,String> params;
	private final Map<String, Collection<@Valid ConfigGroup>> parameterSetsPerType = new HashMap<>();
	private boolean locked = false ;

	private final static Logger log = LogManager.getLogger(ConfigGroup.class);

	public ConfigGroup(final String name) {
		this.name = name;
		this.params = new TreeMap<>();
	}

	public void addParam(final String paramName, final String value) {
		if (this.params.containsKey(paramName)) {
			log.info(this.toString() + "[paramName=" + paramName + ",oldValue=" + this.params.get(paramName) + ",value=" + value + " value replaced]");
		}
		this.params.put(paramName, value);
	}

	/**
	 * Little helper for subclasses (i.e. the ConfigGroups). This method adds the value of the parameter
	 * to the given map only if the getValue() method of this Module doesn't return
	 * null (Java null-type) or the String representation of null, i.e. "null" or "NULL".
	 * If the value is null, the string "null" is added to the map to document the parameter.
	 *
	 * @param map
	 * @param paramName
	 */
	protected void addParameterToMap(final Map<String, String> map, final String paramName) {
		String value = this.getValue(paramName);
		if (!((value == null) || value.equalsIgnoreCase("null"))) {
			map.put(paramName, value);
		} else {
			map.put(paramName, "null");
		}
	}

	/** Check if the set values go well together. This method is usually called after reading the
	 * configuration from a file. If an inconsistency is found, a warning or error should be issued
	 * and (optionally) a RuntimeException being thrown.
	 * @param config TODO
	 */
	protected void checkConsistency(Config config) {
		// (I added Config as a parameter, since there are many occasions where the validity of a ConfigGroup can only be checked when other
		// material is known.  Could put all of this in the "global" config consistency checker, but if it conceptually belongs into the
		// ConfigGroup, I think it is easier to have it more local.  Wasn't a big problem, since this method is _only_ called from the global config
		// itself, which obviously can just pass on a "this" pointer.  kai, jan'17)

		// default: just call this method on parameter sets
		for ( Collection<? extends ConfigGroup> sets : getParameterSets().values() ) {
			for ( ConfigGroup set : sets ) set.checkConsistency(config);
		}
	}
	@Deprecated // please try to use the "typed" access structures.  kai, nov'16
	public String getValue(final String paramName) {
		return this.params.get(paramName);
	}

	public final String getName() {
		return this.name;
	}

	/** @return a Map containing all parameters and their values known to this config group. */
	public Map<String, String> getParams() {
		return this.params;
	}

	/**
	 * @return a Map containing description to some or all parameters return in {@link #getParams()}.
	 */
	@SuppressWarnings("static-method")
	public Map<String, String> getComments() {
		return new HashMap<>();
	}

	@Override
	public final String toString() {
		StringBuilder str = new StringBuilder();
		for ( Entry<String, String> entry : this.getParams().entrySet() ) {
			str.append('[');
			str.append(entry.getKey());
			str.append('=');
			str.append(entry.getValue());
			str.append(']');
		}
		return "[name=" + this.getName() + "]" +
				"[nOfParams=" + this.getParams().size() + "]" + str.toString();
	}

	// /////////////////////////////////////////////////////////////////////////
	// "Parameter sets" are nested sub-modules.
	// They have a "type", which is the equivalent of the name of a Module,
	// except that an arbitrary number of parameter sets per type is allowed.
	// TODO: find a way to associate a type with a specific java type/class
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Override if parameter sets of a certain type need a special implementation
	 */
	@SuppressWarnings("static-method")
	public ConfigGroup createParameterSet(final String type) {
		return new ConfigGroup( type );
	}

	//public final Module createAndAddParameterSet(final String type) {
	//	final Module m = createParameterSet( type );

	//	if ( !m.getName().equals( type ) ) {
	//		throw new IllegalArgumentException( "the \"name\" of parameter sets should correspond to their type."+
	//				" type \""+type+"\" is different from name \""+m.getName()+"\" " );
	//	}

	//	addParameterSet( m );
	//	return m;
	//}

	public void addParameterSet(final ConfigGroup set) {
		checkParameterSet( set );
		Collection<ConfigGroup> parameterSets = parameterSetsPerType.get( set.getName() );

		if ( parameterSets == null ) {
			parameterSets = new ArrayList<>();
			parameterSetsPerType.put( set.getName() ,  parameterSets );
		}

		parameterSets.add( set );
	}

	public boolean removeParameterSet( final ConfigGroup set ) {
		final Collection<ConfigGroup> parameterSets = parameterSetsPerType.get( set.getName() );
		return parameterSets != null ?
			parameterSets.remove( set ) :
			false;
	}

	/**
	 * Method called on parameter sets added by the add methods.
	 * Can be extended if there are consistency checks to makes,
	 * for instance if parameter sets of a given type should be
	 * instances of a particular class.
	 * @param set
	 */
	protected void checkParameterSet(final ConfigGroup set) {
		// empty for inheritance
	}

	/**
	 * Useful for instance if default values are provided but should be cleared if
	 * user provides values.
	 */
	protected final Collection<? extends ConfigGroup> clearParameterSetsForType( final String type ) {
		return parameterSetsPerType.remove( type );
	}

	public final Collection<? extends ConfigGroup> getParameterSets(final String type) {
		final Collection<ConfigGroup> sets = parameterSetsPerType.get( type );
		return sets == null ?
			Collections.<ConfigGroup>emptySet() :
			Collections.unmodifiableCollection( sets );
	}

	public final Map<String, ? extends Collection<? extends ConfigGroup>> getParameterSets() {
		// TODO: immutabilize (including lists)
		// maybe done with what I did below?  kai, sep'16

		//		return parameterSetsPerType;

		Map<String, Collection<ConfigGroup>> parameterSetsPerType2 = new TreeMap<>() ;
		for ( Entry<String, Collection<ConfigGroup>> entry : parameterSetsPerType.entrySet() ) {
			parameterSetsPerType2.put( entry.getKey(), Collections.unmodifiableCollection(entry.getValue()) ) ;
		}
		return Collections.unmodifiableMap( parameterSetsPerType2 ) ;
	}

	public final boolean isLocked() {
		return locked;
	}

	public void setLocked() {
		// need to have this non-final to be able to override in order to set delegates.  kai, jun'15
		this.locked = true ;
		for ( Collection<ConfigGroup> parameterSets : this.parameterSetsPerType.values() ) {
			for ( ConfigGroup parameterSet : parameterSets ) {
				parameterSet.setLocked();
			}
		}
	}

	public final void testForLocked() {
		if ( locked ) {
			throw new RuntimeException( "Too late to change this ...") ;
		}
	}

	public static URL getInputFileURL(URL context, String filename) {
		if (filename.startsWith("~/")) {
			filename = System.getProperty("user.home") + filename.substring(1);
			return IOUtils.getFileUrl(filename) ;
		}
		if ( filename.startsWith("/") ) {
			// (= filename is absolute)
			// (yyyy this may possibly fail on win systems. kai, sep.18)

			// Absolute filename on Windows, when obtained through URL.toURI().getPath() starts with `/`, like on Unix.
			return IOUtils.getFileUrl(filename) ;
		}
		return IOUtils.extendUrl(context, filename);
	}
}
