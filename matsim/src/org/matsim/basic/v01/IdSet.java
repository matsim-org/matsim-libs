/* *********************************************************************** *
 * project: org.matsim.*
 * IdSet.java
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

package org.matsim.basic.v01;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.identifiers.IdentifiedI;
import org.matsim.utils.identifiers.IdentifiedSetI;

// TODO [DS] Merge this class with labeIdentifiedSet, from which i copied everything
// (Do we really want to do this?  kai)

// TODO [kn] this is a bit of a misnomer, should be called IdentifiedSet instead of IdSet.  It is not a set of Ids, but a set
// of identified objects.

public class IdSet extends AbstractSet implements IdentifiedSetI
{
	   private final Map<IdI, IdentifiedI> map = new LinkedHashMap<IdI, IdentifiedI>();
	   
	   public IdSet() {
		   // super() ;
		   // FIXME [kn] there was originally no ctor here at all.  I included it so I can search for where it is called.
		   // The ctor should call the super ctor.  I do not know, however, if this has any side effects, so it is commented
		   // out ...
	   }

	@Override
	public Iterator<IdentifiedI> iterator() {
        return this.map.values().iterator();
	}

	@Override
	public int size() {
        return this.map.size();
	}

	@Override
	public boolean add(final Object element) {
		if (element instanceof IdentifiedI) {
			return add((IdentifiedI)element);
		}
		return false;
	}

	public boolean add(final IdentifiedI element) {
        if (element == null)
            throw new IllegalArgumentException("Attempt to add a null-element.");

        this.map.put(element.getId(), element);
        return true;
	}

	public IdentifiedI get(final IdI id) {
		Object erg = this.map.get(id);
        return (IdentifiedI)erg;
	}

	public IdentifiedI get(final String label) {
		if (label == null) {
			return null;
		}
		return get(new Id(label));
	}

	public boolean containsId(final IdI id) {
        return this.map.containsKey(id);
	}

	public boolean containsLabel(final String label) {
        return (get(label) != null);
	}

}