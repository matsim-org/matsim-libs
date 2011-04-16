/* *********************************************************************** *
 * project: org.matsim.*
 * InundationDataWriter_v2.java
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
package playground.gregor.snapshots.writers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import org.matsim.vis.otfvis.data.OTFDataWriter;

import playground.gregor.otf.readerwriter.InundationData;


public class InundationDataWriter_v2 extends OTFDataWriter<Void> {

	private static final long serialVersionUID = 6693752092591508527L;

	private final InundationData data;

	private final double startTime;

	public InundationDataWriter_v2(InundationData data, double startTime) {
		this.data = data;
		this.startTime = startTime;
	}

	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
		ByteArrayOutputStream a = new ByteArrayOutputStream();

		ObjectOutputStream o = new ObjectOutputStream(a);
		o.writeObject(this.data);

		out.putDouble(this.startTime);
		out.putInt(a.toByteArray().length);

		out.put(a.toByteArray());
	}

	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		// no dyn data
	}

}
