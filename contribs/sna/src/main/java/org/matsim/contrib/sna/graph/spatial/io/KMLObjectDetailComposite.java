/* *********************************************************************** *
 * project: org.matsim.*
 * KMLObjectDetailComposite.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.sna.graph.spatial.io;

import java.util.ArrayList;
import java.util.List;

import net.opengis.kml._2.PlacemarkType;

/**
 * A composite of multiple {@link KMLObjectDetailComposite} objects.
 * 
 * @author jillenberger
 * 
 */
public class KMLObjectDetailComposite<T> implements KMLObjectDetail<T> {

	private final List<KMLObjectDetail<T>> elements = new ArrayList<KMLObjectDetail<T>>();

	/**
	 * Adds a KMLObjectDetail object to the composite.
	 * 
	 * @param objectDetail
	 *            a KMLObjectDetail object
	 */
	public void addObjectDetail(KMLObjectDetail<T> objectDetail) {
		elements.add(objectDetail);
	}

	/**
	 * Calls {@link KMLObjectDetail#addDetail(PlacemarkType, Object)} on each
	 * object in the composite. The composite will be processed in
	 * insertion-order.
	 */
	@Override
	public void addDetail(PlacemarkType kmlPlacemark, T object) {
		for (KMLObjectDetail<T> objectDetail : elements)
			objectDetail.addDetail(kmlPlacemark, object);

	}

}
