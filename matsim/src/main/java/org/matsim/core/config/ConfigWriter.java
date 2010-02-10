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

package org.matsim.core.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;

import org.matsim.core.api.internal.MatsimFileWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.MatsimXmlWriter;

public class ConfigWriter extends MatsimXmlWriter implements MatsimFileWriter {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final Config config;
	private ConfigWriterHandler handler = null;
	private String dtd = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ConfigWriter(final Config config) {
		this.config = config;
		// always write the latest version, currently v1
		this.dtd = "http://www.matsim.org/files/dtd/config_v1.dtd";
		this.handler = new ConfigWriterHandlerImplV1();
	}

	//////////////////////////////////////////////////////////////////////
	// write methods
	//////////////////////////////////////////////////////////////////////

	public final void writeStream(final java.io.Writer writer) {
		try {
			this.writer = new BufferedWriter(writer);
			write();
			this.writer.flush();
			this.writer = null;
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	public final void writeFile(final String filename) {
		try {
			openFile(filename);
			write();
			close();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	private final void write() {
		try {
			writeXmlHead();
			writeDoctype("config", this.dtd);

			this.handler.startConfig(this.config, this.writer);
			this.handler.writeSeparator(this.writer);
			Iterator<Module> m_it = this.config.getModules().values().iterator();
			while (m_it.hasNext()) {
				Module m = m_it.next();
				this.handler.writeModule(m, this.writer);
				this.handler.writeSeparator(this.writer);
			}
			this.handler.endConfig(this.writer);
			this.writer.flush();
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
