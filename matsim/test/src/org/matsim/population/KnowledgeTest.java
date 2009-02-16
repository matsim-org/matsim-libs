/* *********************************************************************** *
 * project: org.matsim.*
 * PersonTest.java
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

package org.matsim.population;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;

public class KnowledgeTest extends MatsimTestCase {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(KnowledgeTest.class);
	private static final String H = "h";
	private static final String W = "w";
	private static final String E = "e";
	private static final String S = "s";
	private static final String L = "l";
	
	private static final ArrayList<Activity> actsF1 = new ArrayList<Activity>();
	private static final ArrayList<Activity> actsF2 = new ArrayList<Activity>();

	private static final ArrayList<Activity> actsPrim = new ArrayList<Activity>();
	private static final ArrayList<Activity> actsSec = new ArrayList<Activity>();
	private static final ArrayList<Activity> actsAll = new ArrayList<Activity>();

	private static final ArrayList<Activity> actsHPrim = new ArrayList<Activity>();
	private static final ArrayList<Activity> actsHSec = new ArrayList<Activity>();
	private static final ArrayList<Activity> actsHAll = new ArrayList<Activity>();

	private static final ArrayList<Activity> actsWPrim = new ArrayList<Activity>();
	private static final ArrayList<Activity> actsWSec = new ArrayList<Activity>();
	private static final ArrayList<Activity> actsWAll = new ArrayList<Activity>();

	private static final ArrayList<Activity> actsEPrim = new ArrayList<Activity>();
	private static final ArrayList<Activity> actsESec = new ArrayList<Activity>();
	private static final ArrayList<Activity> actsEAll = new ArrayList<Activity>();

	private static final ArrayList<Activity> actsSPrim = new ArrayList<Activity>();
	private static final ArrayList<Activity> actsSSec = new ArrayList<Activity>();
	private static final ArrayList<Activity> actsSAll = new ArrayList<Activity>();

	private static final ArrayList<Activity> actsLPrim = new ArrayList<Activity>();
	private static final ArrayList<Activity> actsLSec = new ArrayList<Activity>();
	private static final ArrayList<Activity> actsLAll = new ArrayList<Activity>();

	private static final Set<String> typesPrim = new TreeSet<String>();
	private static final Set<String> typesSec = new TreeSet<String>();
	private static final Set<String> typesAll = new TreeSet<String>();

	//////////////////////////////////////////////////////////////////////
	// setUp / tearDown
	//////////////////////////////////////////////////////////////////////

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		super.loadConfig(null);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////
	
	private final void check(Knowledge k, Facility f1, Facility f2) {
		assertTrue(k.getActivities(f1.getId()).containsAll(actsF1));
		assertTrue(k.getActivities(f2.getId()).containsAll(actsF2));
		assertTrue(k.getActivities(true).containsAll(actsPrim));
		assertTrue(k.getActivities(false).containsAll(actsSec));
		assertTrue(k.getActivities().containsAll(actsAll));
		assertTrue(k.getActivities(H,true).containsAll(actsHPrim));
		assertTrue(k.getActivities(H,false).containsAll(actsHSec));
		assertTrue(k.getActivities(H).containsAll(actsHAll));
		assertTrue(k.getActivities(W,true).containsAll(actsWPrim));
		assertTrue(k.getActivities(W,false).containsAll(actsWSec));
		assertTrue(k.getActivities(W).containsAll(actsWAll));
		assertTrue(k.getActivities(E,true).containsAll(actsEPrim));
		assertTrue(k.getActivities(E,false).containsAll(actsESec));
		assertTrue(k.getActivities(E).containsAll(actsEAll));
		assertTrue(k.getActivities(S,true).containsAll(actsSPrim));
		assertTrue(k.getActivities(S,false).containsAll(actsSSec));
		assertTrue(k.getActivities(S).containsAll(actsSAll));
		assertTrue(k.getActivities(L,true).containsAll(actsLPrim));
		assertTrue(k.getActivities(L,false).containsAll(actsLSec));
		assertTrue(k.getActivities(L).containsAll(actsLAll));
		assertEquals(typesPrim,k.getActivityTypes(true));
		assertEquals(typesSec,k.getActivityTypes(false));
		assertEquals(typesAll,k.getActivityTypes());
	}
	
	//////////////////////////////////////////////////////////////////////
	// tests
	//////////////////////////////////////////////////////////////////////

	public void testKnowledgeActivities() {
		log.info("running testKnowledge()...");
		
		log.info("  creating test facilities...");
		Facilities facilities = new Facilities();
		Facility f1 = facilities.createFacility(new IdImpl(1),new CoordImpl(1,1));
		f1.createActivity(H);
		f1.createActivity(W);
		f1.createActivity(E);
		f1.createActivity(S);
		f1.createActivity(L);
		Facility f2 = facilities.createFacility(new IdImpl(2),new CoordImpl(2,2));
		f2.createActivity(H);
		f2.createActivity(W);
		log.info("  done.");
		
		log.info("  creating test knowledge...");
		Knowledge k = new Knowledge();
		log.info("  done.");

		log.info("  ---------- add ----------");

		log.info("  adding all activities of facility 1...");
		assertTrue(k.addActivity(f1.getActivity(H),true));
		actsF1.add(f1.getActivity(H));
		actsPrim.add(f1.getActivity(H));
		actsAll.add(f1.getActivity(H));
		actsHPrim.add(f1.getActivity(H));
		actsHAll.add(f1.getActivity(H));
		typesPrim.add(H);
		typesAll.add(H);
		assertTrue(k.addActivity(f1.getActivity(W),true));
		actsF1.add(f1.getActivity(W));
		actsPrim.add(f1.getActivity(W));
		actsAll.add(f1.getActivity(W));
		actsWPrim.add(f1.getActivity(W));
		actsWAll.add(f1.getActivity(W));
		typesPrim.add(W);
		typesAll.add(W);
		assertTrue(k.addActivity(f1.getActivity(E),false));
		actsF1.add(f1.getActivity(E));
		actsSec.add(f1.getActivity(E));
		actsAll.add(f1.getActivity(E));
		actsESec.add(f1.getActivity(E));
		actsEAll.add(f1.getActivity(E));
		typesSec.add(E);
		typesAll.add(E);
		assertTrue(k.addActivity(f1.getActivity(S),false));
		actsF1.add(f1.getActivity(S));
		actsSec.add(f1.getActivity(S));
		actsAll.add(f1.getActivity(S));
		actsSSec.add(f1.getActivity(S));
		actsSAll.add(f1.getActivity(S));
		typesSec.add(S);
		typesAll.add(S);
		assertTrue(k.addActivity(f1.getActivity(L),false));
		actsF1.add(f1.getActivity(L));
		actsSec.add(f1.getActivity(L));
		actsAll.add(f1.getActivity(L));
		actsLSec.add(f1.getActivity(L));
		actsLAll.add(f1.getActivity(L));
		typesSec.add(L);
		typesAll.add(L);
		check(k,f1,f2);
		log.info("  done. prim(h1,w1); sec(e1,s1,l1)");
		
		log.info("  adding again activity 'h' of facility 1...");
		assertFalse(k.addActivity(f1.getActivity(H),true));
		check(k,f1,f2);
		log.info("  done. prim(h1,w1); sec(e1,s1,l1)");
		
		log.info("  adding again activity 'h' of facility 1 with isPrimary=false...");
		assertFalse(k.addActivity(f1.getActivity(H),false));
		check(k,f1,f2);
		log.info("  done. prim(h1,w1); sec(e1,s1,l1)");
		
		log.info("  adding again activity 'h' of facility 2...");
		assertTrue(k.addActivity(f2.getActivity(H),true));
		actsF2.add(f2.getActivity(H));
		actsPrim.add(f2.getActivity(H));
		actsAll.add(f2.getActivity(H));
		actsHPrim.add(f2.getActivity(H));
		actsHAll.add(f2.getActivity(H));
		typesPrim.add(H);
		typesAll.add(H);
		check(k,f1,f2);
		log.info("  done. prim(h1,w1,h2); sec(e1,s1,l1)");
		
		log.info("  adding again activity 'w' of facility 2 with isPrimary=false...");
		assertTrue(k.addActivity(f2.getActivity(W),false));
		actsF2.add(f2.getActivity(W));
		actsSec.add(f2.getActivity(W));
		actsAll.add(f2.getActivity(W));
		actsWSec.add(f2.getActivity(W));
		actsWAll.add(f2.getActivity(W));
		typesSec.add(W);
		typesAll.add(W);
		check(k,f1,f2);
		log.info("  done. prim(h1,w1,h2); sec(e1,s1,l1,w2)");
		
		log.info("  adding 'null'...");
		assertFalse(k.addActivity(null,false));
		check(k,f1,f2);
		log.info("  done. prim(h1,w1,h2); sec(e1,s1,l1,w2)");
		
		log.info("  ---------- remove ----------");

		log.info("  removing 'H' act of f1 with isPrimary=false...");
		assertFalse(k.removeActivity(f1.getActivity(H),false));
		check(k,f1,f2);
		log.info("  done. prim(h1,w1,h2); sec(e1,s1,l1,w2)");
		
		log.info("  removing 'H' act of f1 with isPrimary=true...");
		assertTrue(k.removeActivity(f1.getActivity(H),true));
		actsF1.remove(f1.getActivity(H));
		actsPrim.remove(f1.getActivity(H));
		actsAll.remove(f1.getActivity(H));
		actsHPrim.remove(f1.getActivity(H));
		actsHAll.remove(f1.getActivity(H));
		check(k,f1,f2);
		log.info("  done. prim(w1,h2); sec(e1,s1,l1,w2)");
		
		log.info("  removing again 'H' act of f1 with isPrimary=true...");
		assertFalse(k.removeActivity(f1.getActivity(H),true));
		check(k,f1,f2);
		log.info("  done. prim(w1,h2); sec(e1,s1,l1,w2)");
		
		log.info("  removing 'null' with isPrimary=true...");
		assertFalse(k.removeActivity(null,true));
		check(k,f1,f2);
		log.info("  done. prim(w1,h2); sec(e1,s1,l1,w2)");
		
		log.info("  removing 'L' act of f1...");
		assertTrue(k.removeActivity(f1.getActivity(L)));
		actsF1.remove(f1.getActivity(L));
		actsSec.remove(f1.getActivity(L));
		actsAll.remove(f1.getActivity(L));
		actsLSec.remove(f1.getActivity(L));
		actsLAll.remove(f1.getActivity(L));
		typesSec.remove(L);
		typesAll.remove(L);
		check(k,f1,f2);
		log.info("  done. prim(w1,h2); sec(e1,s1,w2)");
		
		log.info("  removing 'null'...");
		assertFalse(k.removeActivity(null));
		check(k,f1,f2);
		log.info("  done. prim(w1,h2); sec(e1,s1,w2)");
		
		log.info("  ---------- set ----------");
		
		log.info("  setting 'W' act of f2 to primary...");
		assertTrue(k.setPrimaryFlag(f2.getActivity(W),true));
		actsPrim.add(f2.getActivity(W));
		actsSec.remove(f2.getActivity(W));
		actsWPrim.add(f2.getActivity(W));
		actsWSec.remove(f2.getActivity(W));
		typesSec.remove(W);
		check(k,f1,f2);
		log.info("  done. prim(w1,h2,w2); sec(e1,s1)");

		log.info("  setting again 'W' act of f2 to primary...");
		assertTrue(k.setPrimaryFlag(f2.getActivity(W),true));
		check(k,f1,f2);
		log.info("  done. prim(w1,h2,w2); sec(e1,s1)");

		log.info("  setting (not existing) 'L' act of f1 to secondary...");
		assertFalse(k.setPrimaryFlag(f1.getActivity(L),false));
		check(k,f1,f2);
		log.info("  done. prim(w1,h2,w2); sec(e1,s1)");

		log.info("  setting 'null' to secondary...");
		assertFalse(k.setPrimaryFlag(null,false));
		check(k,f1,f2);
		log.info("  done. prim(w1,h2,w2); sec(e1,s1)");

		log.info("  setting all Activities to secondary...");
		assertTrue(k.setPrimaryFlag(false));
		actsPrim.remove(f1.getActivity(W));
		actsSec.add(f1.getActivity(W));
		actsWPrim.remove(f1.getActivity(W));
		actsWSec.add(f1.getActivity(W));
		typesPrim.remove(W);
		typesSec.add(W);
		actsPrim.remove(f2.getActivity(H));
		actsSec.add(f2.getActivity(H));
		actsHPrim.remove(f2.getActivity(H));
		actsHSec.add(f2.getActivity(H));
		typesPrim.remove(H);
		typesSec.add(H);
		actsPrim.remove(f2.getActivity(W));
		actsSec.add(f2.getActivity(W));
		actsWPrim.remove(f2.getActivity(W));
		actsWSec.add(f2.getActivity(W));
		typesPrim.remove(W);
		typesSec.add(W);
		check(k,f1,f2);
		log.info("  done. prim(); sec(w1,e1,s1,h2,w2)");

		log.info("  ---------- remove ----------");
		
		log.info("  removing all 'W' activities...");
		assertTrue(k.removeActivities(W));
		actsF1.remove(f1.getActivity(W));
		actsSec.remove(f1.getActivity(W));
		actsAll.remove(f1.getActivity(W));
		actsWSec.remove(f1.getActivity(W));
		actsWAll.remove(f1.getActivity(W));
		actsF2.remove(f2.getActivity(W));
		actsSec.remove(f2.getActivity(W));
		actsAll.remove(f2.getActivity(W));
		actsWSec.remove(f2.getActivity(W));
		actsWAll.remove(f2.getActivity(W));
		typesSec.remove(W);
		typesAll.remove(W);
		check(k,f1,f2);
		log.info("  done. prim(); sec(e1,s1,h2)");
		
		log.info("  removing again all 'W' activities...");
		assertFalse(k.removeActivities(W));
		check(k,f1,f2);
		log.info("  done. prim(); sec(e1,s1,h2)");

		log.info("  removing all activities...");
		assertTrue(k.removeAllActivities());
		actsF1.remove(f1.getActivity(E));
		actsSec.remove(f1.getActivity(E));
		actsAll.remove(f1.getActivity(E));
		actsESec.remove(f1.getActivity(E));
		actsEAll.remove(f1.getActivity(E));
		typesSec.remove(E);
		typesAll.remove(E);
		actsF1.remove(f1.getActivity(S));
		actsSec.remove(f1.getActivity(S));
		actsAll.remove(f1.getActivity(S));
		actsSSec.remove(f1.getActivity(S));
		actsSAll.remove(f1.getActivity(S));
		typesSec.remove(S);
		typesAll.remove(S);
		actsF2.remove(f2.getActivity(H));
		actsSec.remove(f2.getActivity(H));
		actsAll.remove(f2.getActivity(H));
		actsHSec.remove(f2.getActivity(H));
		actsHAll.remove(f2.getActivity(H));
		typesSec.remove(H);
		typesAll.remove(H);
		check(k,f1,f2);
		log.info("  done. prim(); sec()");

		log.info("  removing again all activities...");
		assertFalse(k.removeAllActivities());
		check(k,f1,f2);
		log.info("  done. prim(); sec()");

		log.info("done.");
	}
}
