/* *********************************************************************** *
 * project: org.matsim.*
 * WorldWriter.java
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

package org.matsim.world;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimFileWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.MatsimXmlWriter;

public class WorldWriter extends MatsimXmlWriter implements MatsimFileWriter {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private WorldWriterHandler writerhandler = null;
	private final World world;
	private String dtd;

	private final static Logger log = Logger.getLogger(WorldWriter.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new WorldWriter to write the specified world to the specified file.
	 *
	 * @param world
	 */
	public WorldWriter(final World world) {
		super();
		this.world = world;
		// always write out latest version, currently v2
		this.dtd = "http://www.matsim.org/files/dtd/world_v2.dtd";
		this.writerhandler = new WorldWriterHandlerImplV2();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void writeLayer(final Layer l, final BufferedWriter out) {
		if (l instanceof ZoneLayer) {
			try {
				this.writerhandler.startLayer((ZoneLayer)l, out);
				Iterator<? extends MappedLocation> z_it = l.getLocations().values().iterator();
				while (z_it.hasNext()) {
					Zone z = (Zone)z_it.next();
					this.writerhandler.startZone(z, out);
					this.writerhandler.endZone(out);
				}
				this.writerhandler.endLayer(out);
				this.writerhandler.writeSeparator(out);
				out.flush();
			}
			catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}
		else {
			log.info("layer_type=" + l.getType() + ": Layer must be explicitly written to a seperate xml file");
		}
	}

	private final void writeRule(final Layer downLayer, final Layer upLayer) {
		if (downLayer instanceof ZoneLayer) {
			try {
				this.writerhandler.startMapping(downLayer, upLayer, this.writer);
				Iterator<? extends MappedLocation> dz_it = downLayer.getLocations().values().iterator();
				while (dz_it.hasNext()) {
					Zone dz = (Zone)dz_it.next();
					Iterator<MappedLocation> uz_it = dz.getUpMapping().values().iterator();
					while (uz_it.hasNext()) {
						Zone uz = (Zone)uz_it.next();
						this.writerhandler.startRef(dz, uz, this.writer);
						this.writerhandler.endRef(this.writer);
					}
				}
				this.writerhandler.endMapping(this.writer);
				this.writerhandler.writeSeparator(this.writer);
				this.writer.flush();
			}
			catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// write methods
	//////////////////////////////////////////////////////////////////////

	public final void writeFile(final String filename) {
		try {
			openFile(filename);
			writeXmlHead();
			writeDoctype("world", this.dtd);
			this.writer.flush();
			this.writerhandler.startWorld(this.world, this.writer);
			this.writerhandler.writeSeparator(this.writer);
			this.writer.flush();
			Layer l = this.world.getBottomLayer();
			if (l != null) {
				this.writeLayer(l, this.writer);
				while (l.getUpLayer() != null) {
					l = l.getUpLayer();
					this.writeLayer(l, this.writer);
				}
			}
			l = this.world.getBottomLayer();
			if (l != null) {
				while (l.getUpLayer() != null) {
					this.writeRule(l, l.getUpLayer());
					l = l.getUpLayer();
				}
			}
			this.writerhandler.endWorld(this.writer);
			this.writer.flush();
			close();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return super.toString();
	}
}
