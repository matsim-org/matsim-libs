/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package org.matsim.network;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.matsim.basic.v01.BasicNode;
import org.matsim.basic.v01.Id;
import org.matsim.utils.identifiers.IdI;

/**
 * @author dgrether
 *
 */
public class NetworkFactory {

	private Class<? extends Link> linkPrototype = LinkImpl.class;

	private Constructor prototypeContructor;

	private static final Class[] PROTOTYPECONSTRUCTOR = { IdI.class,
			BasicNode.class, BasicNode.class, NetworkLayer.class, double.class,
			double.class, double.class, double.class};

	public NetworkFactory() {
		try {
			this.prototypeContructor = LinkImpl.class
					.getConstructor(PROTOTYPECONSTRUCTOR);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	protected Node newNode(final String id, final String x, final String y,
			final String type) {
		return new Node(new Id(id), x, y, type);
	}

	protected Link newLink(final IdI id, Node from, Node to,
			NetworkLayer network, double length, double freespeedTT, double capacity,
			double lanes) {
		Link ret;
		Exception ex;
		try {
			ret = (Link) this.prototypeContructor.newInstance(new Object[] { id,
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

	public void setLinkPrototype(Class<? extends Link> prototype) {
		try {
			Constructor c = prototype.getConstructor(PROTOTYPECONSTRUCTOR);
			if (null != c) {
				this.prototypeContructor = c;
			}
			else {
				throw new IllegalArgumentException(
						"A prototype must have a public constructor with parameter types: "
								+ PROTOTYPECONSTRUCTOR);
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

	}

}
