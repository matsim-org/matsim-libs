/* *********************************************************************** *
 * project: org.matsim.*
 * CountsWriter.java
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

package org.matsim.counts;

import java.io.IOException;
import java.util.Iterator;

import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;
import org.matsim.writer.Writer;

public class CountsWriter extends Writer {

	private CountsWriterHandler handler = null;
	private final Counts counts;

	public CountsWriter(final Counts counts) {
		this(counts,
				Gbl.getConfig().counts().getOutputFile());
	}

	public CountsWriter(final Counts counts, final String filename) {
		this.counts = counts;
		this.outfile = filename;
		this.dtd = null;

		// use the newest writer-version by default
		this.handler = new CountsWriterHandlerImplV1();
	}

	@Override
	public final void write() {
		try {

			this.out = IOUtils.getBufferedWriter(this.outfile);

			// write custom header
			this.out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			this.out.flush();

			this.handler.startCounts(this.counts, this.out);
			this.handler.writeSeparator(this.out);

			//counts iterator
			Iterator<Count> c_it = this.counts.getCounts().values().iterator();
			while (c_it.hasNext()) {
				Count c = c_it.next();
				this.handler.startCount(c,this.out);

				// volume iterator
				Iterator<Volume> vol_it = c.getVolumes().values().iterator();
				while (vol_it.hasNext()) {
					Volume v = vol_it.next();
					this.handler.startVolume(v, this.out);
					this.handler.endVolume(this.out);
				}
				this.handler.endCount(this.out);
				this.handler.writeSeparator(this.out);
				this.out.flush();
			}
			this.handler.endCounts(this.out);
			this.out.close();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	public final void writeFile(final String filename) {
		this.outfile = filename;
		write();
	}

	@Override
	public final String toString() {
		return super.toString();
	}
}
