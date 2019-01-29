/* *********************************************************************** *
 * project: org.matsim.*
 * OTFWriterFactory.java
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

package org.matsim.vis.otfvis.data;


/**
 * OTFWriterFactory is responsible to create Writer instances for an QueueLink or QueueNode object.
 * <p></p>
 * I don't think that this is a "Matsim"Factory and for that reason is not marked by that interface. kai, mar'11
 * 
 * @author dstrippgen
 *
 * @param <SrcClass> should be QueueLink or QueueNode right now.
 */
public interface OTFWriterFactory<SrcClass> {

	public OTFDataWriter<SrcClass> getWriter();
}
