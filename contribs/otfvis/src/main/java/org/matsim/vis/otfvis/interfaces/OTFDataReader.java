/* *********************************************************************** *
 * project: org.matsim.*
 * OTFDataReader.java
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

package org.matsim.vis.otfvis.interfaces;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataWriter;


/**
 * OTFDataReader is the base class for all Reader classes.
 * The Reader classes retrieve the information from the ByteStream and
 * directly transfer the information onto the receiver classes. The Reader classes have therefore
 * to extract the exact number of bytes from the stream that the Writer class has written.
 *
 * @author dstrippgen
 */
public abstract class  OTFDataReader {

	private OTFDataWriter src;
	public void setSrc(OTFDataWriter src) {
		this.src = src;
	}
	public OTFDataWriter getSrc() {
		return this.src;
	}
	public abstract void readConstData(ByteBuffer in) throws IOException;
	public abstract void readDynData(ByteBuffer in, SceneGraph graph) throws IOException;
	public abstract void invalidate(SceneGraph graph);
}

