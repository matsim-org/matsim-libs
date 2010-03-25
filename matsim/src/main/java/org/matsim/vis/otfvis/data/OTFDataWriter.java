/* *********************************************************************** *
 * project: org.matsim.*
 * OTFDataWriter.java
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

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;


/**
 * OTFDataWriter<SrcData> is the base of all Writer classes. SrcData is most likely a
 * QueueLink, QueueNode or likewise.
 * 
 * @author dstrippgen
 *
 * @param <SrcData>
 */
public abstract class OTFDataWriter<SrcData> implements Serializable {

	private static final long serialVersionUID = 7593448140900220038L;
	
	protected transient SrcData src;
	
	abstract public void writeConstData(ByteBuffer out) throws IOException;
	abstract public void writeDynData(ByteBuffer out) throws IOException;
	
	public void setSrc(SrcData src) {
		this.src = src;
	}
	
	public SrcData getSrc() {
		return this.src;
	}

}
