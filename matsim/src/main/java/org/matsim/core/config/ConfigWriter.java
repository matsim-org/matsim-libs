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
import java.io.UncheckedIOException;

import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.io.MatsimXmlWriter;

public final class ConfigWriter extends MatsimXmlWriter implements MatsimWriter {

	public static enum Verbosity {all, minimal }

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final Config config;
	private ConfigWriterHandler handler = null;
	private String dtd = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	public ConfigWriter(final Config config, final Verbosity verbosity ) {
		this.config = config;
		// always write the latest version, currently v2
		this.dtd = "http://www.matsim.org/files/dtd/config_v2.dtd";
		this.handler = new ConfigWriterHandlerImplV2(verbosity);
	}
	public ConfigWriter(final Config config) {
		this( config, Verbosity.all ) ;
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
			throw new UncheckedIOException(e);
		}
	}

	public final void writeStream(final java.io.Writer writer, final String newline) {
		try {
			final String formerNewLine = this.handler.setNewline(newline);
			this.writer = new BufferedWriter(writer);
			write();
			this.writer.flush();
			this.writer = null;
			this.handler.setNewline( formerNewLine );
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public final void write(final String filename) throws UncheckedIOException {
		openFile(filename);
		write();
		close();
	}

	public final void writeFileV1(final String filename) {
		this.dtd = "http://www.matsim.org/files/dtd/config_v1.dtd";
		this.handler = new ConfigWriterHandlerImplV1();
		write( filename );
	}

	public final void writeFileV2(final String filename) {
		this.dtd = "http://www.matsim.org/files/dtd/config_v2.dtd";
		this.handler = new ConfigWriterHandlerImplV2(Verbosity.all);
		write( filename );
	}

	private void write() {
		try {
			writeXmlHead();
			writeDoctype("config", this.dtd);

			this.handler.startConfig(this.config, this.writer);
			this.handler.writeSeparator(this.writer);
			for (ConfigGroup m : this.config.getModules().values()) {
				this.handler.writeModule(m, this.writer);
				this.handler.writeSeparator(this.writer);
			}
			this.handler.endConfig(this.writer);
			this.writer.flush();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
