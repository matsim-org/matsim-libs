/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigWriter.java
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

package org.matsim.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;

import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;
import org.matsim.writer.Writer;

public class ConfigWriter extends Writer {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private java.io.Writer outstream = null;

	private final Config config;
	private ConfigWriterHandler handler = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ConfigWriter(final Config config) {
		this(config, config.config().getOutputFile());
	}

	public ConfigWriter(final Config config, final java.io.Writer writer) {
		super();
		this.config = config;
		this.outstream = writer;
		this.handler = new ConfigWriterHandlerImplV1();
	}

	public ConfigWriter(final Config config, final String filename) {
		this.config = config;
		this.outfile = filename;
		// always write the latest version, currently v1
		this.dtd = "http://www.matsim.org/files/dtd/config_v1.dtd";
		this.handler = new ConfigWriterHandlerImplV1();
	}

	//////////////////////////////////////////////////////////////////////
	// write methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final void write() {
		try {
			if (this.outstream == null) {
				this.out = IOUtils.getBufferedWriter(this.outfile);
			} else {
				this.out = new BufferedWriter(this.outstream);
			}
			writeDtdHeader("config");
			this.out.flush();

			this.handler.startConfig(this.config, this.out);
			this.handler.writeSeparator(this.out);
			Iterator<Module> m_it = this.config.getModules().values().iterator();
			while (m_it.hasNext()) {
				Module m = m_it.next();
				this.handler.writeModule(m, this.out);
				this.handler.writeSeparator(this.out);
			}
			this.handler.endConfig(this.out);
			this.out.flush();
			if (this.outstream == null) {
				this.out.close();
			}
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
