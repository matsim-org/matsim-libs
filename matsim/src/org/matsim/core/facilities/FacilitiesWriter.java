/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesWriter.java
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

package org.matsim.core.facilities;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.SortedSet;

import org.matsim.core.api.internal.MatsimFileWriter;
import org.matsim.core.utils.io.MatsimXmlWriter;

public class FacilitiesWriter extends MatsimXmlWriter implements MatsimFileWriter {

	private FacilitiesWriterHandler handler = null;
	private final ActivityFacilitiesImpl facilities;
	private String dtd;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new FacilitiesWriter to write the specified facilities to the file.
	 *
	 * @param facilities
	 */
	public FacilitiesWriter(final ActivityFacilitiesImpl facilities) {
		super();
		this.facilities = facilities;
		// always use newest version, currently v1
		this.dtd = "http://www.matsim.org/files/dtd/facilities_v1.dtd";
		this.handler = new FacilitiesWriterHandlerImplV1();
	}

	//////////////////////////////////////////////////////////////////////
	// write methods
	//////////////////////////////////////////////////////////////////////

	public final void writeFile(final String filename) {
		this.writeOpenAndInit(filename);
		for (ActivityFacilityImpl f : this.facilities.getFacilities().values()) {
			this.writeFacility(f);
		}
		this.writeFinish();
	}

	public final void writeOpenAndInit(final String filename) {
		try {
			openFile(filename);
			this.writeXmlHead();
			this.writeDoctype("facilities", this.dtd);
			this.handler.startFacilities(this.facilities, this.writer);
			this.handler.writeSeparator(this.writer);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public final void writeFacility(final ActivityFacilityImpl f) {
		try {
			this.handler.startFacility(f, this.writer);
			Iterator<ActivityOptionImpl> a_it = f.getActivityOptions().values().iterator();
			while (a_it.hasNext()) {
				ActivityOptionImpl a = a_it.next();
				this.handler.startActivity(a, this.writer);
				this.handler.startCapacity(a, this.writer);
				this.handler.endCapacity(this.writer);
				Iterator<SortedSet<OpeningTime>> o_set_it = a.getOpeningTimes().values().iterator();
				while (o_set_it.hasNext()) {
					SortedSet<OpeningTime> o_set = o_set_it.next();
					Iterator<OpeningTime> o_it = o_set.iterator();
					while (o_it.hasNext()) {
						OpeningTime o = o_it.next();
						this.handler.startOpentime(o, this.writer);
						this.handler.endOpentime(this.writer);
					}
				}
				this.handler.endActivity(this.writer);
			}
			this.handler.endFacility(this.writer);
			this.handler.writeSeparator(this.writer);
			this.writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public final void writeFinish() {
		try {
			this.handler.endFacilities(this.writer);
			this.writer.flush();
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
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
