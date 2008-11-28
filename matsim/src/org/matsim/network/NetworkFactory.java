/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.network;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.Id;
import org.matsim.interfaces.basic.v01.BasicNode;
import org.matsim.population.routes.NodeCarRouteFactory;
import org.matsim.population.routes.Route;
import org.matsim.population.routes.RouteFactory;
import org.matsim.utils.geometry.Coord;

/**
 * @author dgrether
 */
public class NetworkFactory {

	private Constructor<? extends Link> prototypeContructor;

	private static final Class[] PROTOTYPECONSTRUCTOR = { Id.class,
			BasicNode.class, BasicNode.class, NetworkLayer.class, double.class,
			double.class, double.class, double.class};

	private Map<BasicLeg.Mode, RouteFactory> routeFactories = new HashMap<BasicLeg.Mode, RouteFactory>();
	
	public NetworkFactory() {
		try {
			this.prototypeContructor = LinkImpl.class
					.getConstructor(PROTOTYPECONSTRUCTOR);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		
		routeFactories.put(BasicLeg.Mode.car, new NodeCarRouteFactory());
	}

	protected Node createNode(final Id id, final Coord coord, final String type) {
		return new Node(id, coord, type);
	}

	protected Link createLink(final Id id, Node from, Node to,
			NetworkLayer network, double length, double freespeedTT, double capacity,
			double lanes) {
		Link ret;
		Exception ex;
		try {
			ret = this.prototypeContructor.newInstance(new Object[] { id,
					from, to, network, length, freespeedTT, capacity, lanes });
			return ret;
		} catch (InstantiationException e) {
			e.printStackTrace();
			ex = e;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			ex = e;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			ex = e;
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			ex = e;
		}
		throw new RuntimeException(
				"Cannot instantiate link from prototype, this should never happen, but never say never!",
				ex);
	}
	
	/**
	 * @param mode the transport mode the route should be for
	 * @return a new Route for the specified mode
	 * @throws IllegalArgumentException if no {@link RouteFactory} is registered that creates routes for the specified mode.
	 * 
	 * @see #setRouteFactory(org.matsim.basic.v01.BasicLeg.Mode, RouteFactory)
	 */
	public Route createRoute(final BasicLeg.Mode mode) {
		final RouteFactory factory = this.routeFactories.get(mode);
		if (factory == null) {
			throw new IllegalArgumentException("There is no factory known to create routes for leg-mode " + mode.toString());
		}
		return factory.createRoute();
	}

	/**
	 * Registers a {@link RouteFactory} for the specified mode. If <code>factory</code> is <code>null</code>,
	 * the existing entry for this <code>mode</code> will be deleted.
	 * 
	 * @param mode
	 * @param factory
	 */
	public void setRouteFactory(final BasicLeg.Mode mode, final RouteFactory factory) {
		if (factory == null) {
			this.routeFactories.remove(mode);
		} else {
			this.routeFactories.put(mode, factory);
		}
	}
	
	public void setLinkPrototype(Class<? extends Link> prototype) {
		try {
			Constructor<? extends Link> c = prototype.getConstructor(PROTOTYPECONSTRUCTOR);
			if (null != c) {
				this.prototypeContructor = c;
			}
			else {
				throw new IllegalArgumentException(
						"A prototype must have a public constructor with parameter types: "
								+ Arrays.toString(PROTOTYPECONSTRUCTOR));
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

	}

	public boolean isTimeVariant() {
		return (this.prototypeContructor.getDeclaringClass() == TimeVariantLinkImpl.class);
	}

}
