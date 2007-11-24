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

import java.io.IOException;
import java.util.Iterator;

import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;
import org.matsim.writer.Writer;

public class WorldWriter extends Writer {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private WorldWriterHandler writerhandler = null;
	private final World world;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public WorldWriter(final World world) {
		super();
		this.world = world;
		this.outfile = Gbl.getConfig().world().getOutputFile();
		this.version = null;
		// always write out latest version, currently v2
		this.dtd = "http://www.matsim.org/files/dtd/world_v2.dtd";
		this.writerhandler = new WorldWriterHandlerImplV2();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void writeLayer(Layer l) {
		if (l instanceof ZoneLayer) {
			try {
				this.writerhandler.startLayer((ZoneLayer)l, this.out);
				Iterator<? extends Location> z_it = l.getLocations().values().iterator();
				while (z_it.hasNext()) {
					Zone z = (Zone)z_it.next();
					this.writerhandler.startZone(z, this.out);
					this.writerhandler.endZone(this.out);
				}
				this.writerhandler.endLayer(this.out);
				this.writerhandler.writeSeparator(this.out);
				this.out.flush();
			}
			catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}
		else {
			Gbl.noteMsg(this.getClass(),"writeLayer(...)","[layer_type=" + l.getType() + ": Layer must be explicitly written to a seperate xml file]");
		}
	}

	private final void writeRule(MappingRule m) {
		if ((m.getDownLayer() instanceof ZoneLayer)) {
			try {
				this.writerhandler.startMapping(m, this.out);
				Iterator<? extends Location> dz_it = m.getDownLayer().getLocations().values().iterator();
				while (dz_it.hasNext()) {
					Zone dz = (Zone)dz_it.next();
					Iterator<Location> uz_it = dz.getUpMapping().values().iterator();
					while (uz_it.hasNext()) {
						Zone uz = (Zone)uz_it.next();
						this.writerhandler.startRef(dz, uz, this.out);
						this.writerhandler.endRef(this.out);
						this.out.flush();
					}
				}
				this.writerhandler.endMapping(this.out);
				this.writerhandler.writeSeparator(this.out);
				this.out.flush();
			}
			catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}
		else {
			Gbl.noteMsg(this.getClass(),"writeRule(...)","[m=" + m + "," + "downLayer_type=" + m.getDownLayer().getType() + ": Layer not written. Therefore, rule is not written]");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// write methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final void write() {
		try {
			this.out = IOUtils.getBufferedWriter(this.outfile);
			writeHeader("world");
			this.out.flush();
			this.writerhandler.startWorld(this.world, this.out);
			this.writerhandler.writeSeparator(this.out);
			this.out.flush();
			Layer l = this.world.getBottomLayer();
			if (l != null) {
				this.writeLayer(l);
				while (l.getUpRule() != null) {
					l = l.getUpRule().getUpLayer();
					this.writeLayer(l);
				}
			}
			l = this.world.getBottomLayer();
			if (l != null) {
				if (l.getUpRule() != null) {
					MappingRule m = l.getUpRule();
					this.writeRule(m);
					while (m.getUpLayer().getUpRule() != null) {
						m = m.getUpLayer().getUpRule();
						this.writeRule(m);
					}
				}
			}
			this.writerhandler.endWorld(this.out);
			this.out.flush();
			this.out.close();
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
