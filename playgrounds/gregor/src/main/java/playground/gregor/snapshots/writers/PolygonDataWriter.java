/* *********************************************************************** *
 * project: org.matsim.*
 * PolygonDataWriter.java
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

import org.geotools.data.FeatureSource;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.opengl.drawer.SimpleBackgroundFeatureDrawer;

public class PolygonDataWriter extends OTFDataWriter<Void> {

	private static final long serialVersionUID = 7454644070258134874L;
	private SimpleBackgroundFeatureDrawer data;

	public PolygonDataWriter(FeatureSource fs, float [] color ) {
		try {
			this.data = new SimpleBackgroundFeatureDrawer(fs,color);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void writeConstData(ByteBuffer out) throws IOException {

	ByteArrayOutputStream a = new ByteArrayOutputStream();

		ObjectOutputStream o = new ObjectOutputStream(a);
		o.writeObject(this.data);

		out.putInt(a.toByteArray().length);
		out.put(a.toByteArray());
	}

	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		// no dyn data
	}

}
