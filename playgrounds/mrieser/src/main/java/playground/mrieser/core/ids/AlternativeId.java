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

package playground.mrieser.core.ids;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mrieser / Senozon AG
 */
public abstract class AlternativeId implements Comparable<AlternativeId> {

	// the static cache
	private final static Map<Class<?>, Map<String, AlternativeId>> cache = new ConcurrentHashMap<Class<?>, Map<String, AlternativeId>>();

	private final String id;

	// private constructore, you have to create ids with one of the create methods
	private AlternativeId(final String id) {
		this.id = id;
	}

	// create methods

	public static PersonId createPersonId(final String key) {
		// the simple implementation would be the next line, but this does not cache, so...
		//	return new PersonId(id);

		Map<String, AlternativeId> map = cache.get(PersonId.class);
		if (map == null) {
			map = new ConcurrentHashMap<String, AlternativeId>();
			cache.put(LinkId.class, map);
		}
		PersonId id = (PersonId) map.get(key);
		if (id == null) {
			id = new PersonId(key);
			map.put(key, id);
		}

		return id;
	}

	public static LinkId createLinkId(final String key) {
		// the simple implementation would be the next line, but this does not cache, so...
		//		return new LinkId(id);

		// argh! this caching has to be re-implemented for every supported type!
		// third party ids (when somebody else extends from AlternativeId to create his own special typed id
		// would have to implement it as well... but do they and how?
		
		Map<String, AlternativeId> map = cache.get(LinkId.class);
		if (map == null) {
			map = new ConcurrentHashMap<String, AlternativeId>();
			cache.put(LinkId.class, map);
		}
		LinkId id = (LinkId) map.get(key);
		if (id == null) {
			id = new LinkId(key);
			map.put(key, id);
		}

		return id;
	}

	// other methods

	@Override
	public int compareTo(AlternativeId o) {
		// this will also compare LinkIds with PersonIds
		//		return this.id.compareTo(o.id);

		// so let's make it better
		if (this.getClass() == o.getClass()) {
			return this.id.compareTo(o.id);
		}

		return this.getClass().getName().compareTo(o.getClass().getName()); // ugh....
	}

	// type-specific classes

	public static class PersonId extends AlternativeId {
		public PersonId(final String id) {
			super(id);
		}

		// this cannot be implemented, because AlternativeId already implements compareTo
		// so, either every class extending from AlternativeId has to implement this on its own
		// or it is no longer really type safe as compareTo accepts any AlternativeId, and not only
		// such of type from the calling object.
//		@Override
//		public int compareTo(PersonId o) {
//			// TODO Auto-generated method stub
//			return super.compareTo(o);
//		}
	}

	public static class LinkId extends AlternativeId {
		public LinkId(final String id) {
			super(id);
		}
	}
}
