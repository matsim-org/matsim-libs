/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.api.internal;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.network.TimeDependentNetwork;
import org.matsim.core.population.routes.RouteFactory;

/**
 * <p> An interface marking MATSim top level containers.  Also a place where to note design decisions 
 * for these containers. (see below). </p>
 * 
 * <h3> Design aspects </h3>
 * <p>
 * We now assume that {@link Scenario} accepts arbitrary container implementations.  However, a container within itself should
 * be consistent.  
 * </p><p>
 * The most plausible use case for a MATSim container alternative implementation seems to be alternative underlying data structures. 
 * Examples (not necessarily implemented) include: <ul>
 * <li> Alternative representation of routes, see, e.g., {@link CompressedNetworkRouteImpl}.  This is in this case achieved by
 * just replacing {@link RouteFactory}, but the idea is the same.
 * <li> Re-implementation of the {@link TimeDependentNetwork}, with a different lookup data structure.  This would be useful
 * to emulate flow-dependent assignment, where a plausible approach seems to have link travel times in an iteration depend on the flows
 * in the previous iteration.
 * <li> Completely different backings of the data, e.g. by data base, or using JNI for a more strongly managed data backing in C.
 * </ul>
 * In order to enable this, each container implementation comes with its factory.  So in principle one has something like
 * <pre>
 * Person person = population.getFactory().createPerson(...) ;
 * ...
 * population.addPerson(person) ;  // (*)
 * </pre>
 * It is currently undefined what happens if at (*) the wrong person implementation is added ... could throw an exception, or could make a
 * defensive copy.  Given that we have lots of code of type
 * <pre>
 * Person person = population.getFactory().createPerson(...) ;
 * population.addPerson(person) ; 
 * person.setXyz(...); // (**)
 * </pre>
 * throwing an exception seems more reasonable -- because with a defensive copy, the setting  (**) would no longer arrive at the person
 * in the container. 
 * </p>
 * <p>
 * The first level of elements below top level containers should NOT have a back pointer ...
 * ... since, at that level, one should be able to get the relevant container from
 * the context.
 * (This becomes clear if you think about a nodeId/linkId given by person.)  [[I have to say that I don't fully understand this whole argument
 * right now.  kai, aug'16]]
 * </p>
 * 
 * @author nagel
 *
 */
public interface MatsimToplevelContainer {
	
	MatsimFactory getFactory() ;

}
