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
package org.matsim.contrib.socnetsim.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A memory optimized map to store a relation Id to plan.
 * Designed specifically for the case of joint plans.
 *
 * @author thibautd
 */
// TODO: make more generic when language level is java 8 (too much boilerplate in 7)
// would simply need to store a lambda that maps values to Comparable keys.
public class CompactUnmodifiablePlanMap implements Map<Id<Person>,Plan> {
	private final Plan[] array;

	public CompactUnmodifiablePlanMap( final Collection<? extends Plan> plans ) {
		array = plans.toArray( new Plan[ plans.size() ] );
		Arrays.sort( array, new PlanComparator() );
	}

	@Override
	public int size() {
		return array.length;
	}

	@Override
	public boolean isEmpty() {
		return array.length == 0;
	}

	@Override
	public boolean containsKey( final Object key ) {
		if ( !(key instanceof Id) ) return false;
		return binarySearch( (Id<Person>) key ) >= 0;
	}


	@Override
	public boolean containsValue( final Object value ) {
		if ( !(value instanceof Plan) ) return false;
		final Plan plan = get( getId( (Plan) value ) );
		return plan != null && plan.equals( value );
	}

	@Override
	public Plan get( final Object key ) {
		if ( !(key instanceof Id) ) return null;
		final int index = binarySearch( (Id<Person>) key );
		return index < 0 ? null : array[ index ];
	}

	@Override
	public Plan put( final Id<Person> key, final Plan value ) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Plan remove( final Object key ) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll( final Map<? extends Id<Person>, ? extends Plan> m ) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Id<Person>> keySet() {
		return new AbstractSet<Id<Person>>() {
			@Override
			public boolean contains( final Object id ) {
				return containsKey( id );
			}

			@Override
			public Iterator<Id<Person>> iterator() {
				return new Iterator<Id<Person>>() {
					int i = 0;
					@Override
					public boolean hasNext() {
						return i < size();
					}

					@Override
					public Id<Person> next() {
						return getId( array[ i++ ] );
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override
			public int size() {
				return CompactUnmodifiablePlanMap.this.size();
			}
		};
	}

	@Override
	public Collection<Plan> values() {
		return Arrays.asList( array );
	}

	@Override
	public Set<Entry<Id<Person>, Plan>> entrySet() {
		return new AbstractSet<Entry<Id<Person>,Plan>>() {
			@Override
			public Iterator<Entry<Id<Person>, Plan>> iterator() {
				return new Iterator<Entry<Id<Person>, Plan>>() {
					int i = 0;
					@Override
					public boolean hasNext() {
						return i < size();
					}

					@Override
					public Entry<Id<Person>, Plan> next() {
						final int index = i++;
						return new Entry<Id<Person>, Plan>() {
							@Override
							public Id<Person> getKey() {
								return getId( array[ index ] );
							}

							@Override
							public Plan getValue() {
								return array[ index ];
							}

							@Override
							public Plan setValue( final Plan value ) {
								throw new UnsupportedOperationException();
							}
						};
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override
			public int size() {
				return CompactUnmodifiablePlanMap.this.size();
			}
		};
	}

	private int binarySearch( final Id<Person> key ) {
		// adapted from Arrays.binarySearch
		int low = 0;
        int high = array.length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Plan midVal = array[mid];
            int cmp = midVal.getPerson().getId().compareTo( key );
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
	}

	private Id<Person> getId( final Plan plan ) {
		return plan.getPerson().getId();
	}

	private static class PlanComparator implements Comparator<Plan> {
		@Override
		public int compare( final Plan o1, final Plan o2 ) {
			return o1.getPerson().getId().compareTo( o2.getPerson().getId() );
		}
	}

	@Override
	public String toString() {
		return "CompactUnmodifiablePlanMap{" +
					"size="+array.length+", " +
					"plans="+Arrays.asList( array )+"}";
	}
}
