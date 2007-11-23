/* *********************************************************************** *
 * project: org.matsim.*
 * IdentifiedSetI.java
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

package org.matsim.utils.identifiers;

import java.util.Set;

/**
 * A slight extension of <code>Set</code> for elements of type
 * <code>IdentifiableI</code>.
 * 
 * @see java.util.Set
 */
public interface IdentifiedSetI extends Set {
// public interface IdentifiedSetI<T extends IdentifiedI> extends Set<T> { // GENERICS-Variante

	
    /**
     * Adds <code>o</code> (which must not be <code>null</code>) to this
     * set, if it is an instance of <code>IdentifiedI</code>.
     * 
     * @param o
     *            the <code>IdentifiedI</code> to be added
     * 
     * @return <code>true</code> if <code>o</code> has been added and
     *         <code>false</code> otherwise
     * 
     * @throws IllegalArgumentException
     *             if <code>o</code> is <code>null</code> or not an instance
     *             of <code>IdentifiedI</code>
     * 
     * @see java.util.Collection#add(java.lang.Object)
     */
    public boolean add(Object o);	// remove for GENERICS

    /**
     * Adds <code>o</code> (which must not be <code>null</code>) to this
     * set.
     * 
     * @param o
     *            the <code>IdentifiedI</code> to be added
     * 
     * @return <code>true</code> if <code>o</code> has been added and
     *         <code>false</code> otherwise
     * 
     * @throws IllegalArgumentException
     *             if <code>o</code> is <code>null</code>
     * 
     */
    public boolean add(IdentifiedI o);
//    public boolean add(T o);	// GENERICS-Variante

    /**
     * If there is an entry in this set with its identifier being equal to
     * <code>id</code>, it is returned. Otherwise, <code>null</code> is
     * returned.
     * 
     * @param id
     *            the identifier of the requested entry
     * 
     * @return the entry with identifier <code>id</code>
     */
    public IdentifiedI get(IdI id);

    /**
     * If there is an entry in this set that has an identifer with a
     * <code>String</code> representation equal to <code>label</code>, it
     * is returned. Otherwise, <code>null</code> is returned.
     * 
     * @param label
     *            the requested id's <code>String</code> representation
     * 
     * @return the entry with <code>label</code> as its identifier's
     *         <code>String</code> representation
     */
    public IdentifiedI get(String label);

    /**
     * Indicates if an element with identifier equal to <code>id</code> is
     * contained in this set.
     * 
     * @param id
     *            the inquired identifier
     * 
     * @return <code>true</code> if there is a element with an identifier
     *         equal to <code>id</code> is contained in this set, and
     *         <code>false</code> otherwise
     */
    public boolean containsId(IdI id);

    /**
     * Indicates if an element with its identifier's <code>String</code>
     * representation being equal to <code>id</code> is contained in this set.
     * 
     * @param label
     *            the inquired identifier's <code>String</code> representation
     * 
     * @return <code>true</code> if there is a element with its identifier's
     *         <code>String</code> representation being equal to
     *         <code>label</code> is contained in this set, and
     *         <code>false</code> otherwise
     */
    public boolean containsLabel(String label);

}
