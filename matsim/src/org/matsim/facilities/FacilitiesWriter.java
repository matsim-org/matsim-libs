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

package org.matsim.facilities;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;

import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.Location;
import org.matsim.writer.Writer;

public class FacilitiesWriter extends Writer {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private FacilitiesWriterHandler handler = null;
	private final Facilities facilities;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new FacilitiesWriter to write the specified facilities to the file
	 * specified as output-file in the current configuration.
	 *
	 * @param facilities
	 */
	public FacilitiesWriter(final Facilities facilities) {
		this(facilities, Gbl.getConfig().facilities().getOutputFile());
	}

	/**
	 * Creates a new FacilitiesWriter to write the specified facilities to the specified file.
	 *
	 * @param facilities
	 * @param filename
	 */
	public FacilitiesWriter(final Facilities facilities, final String filename) {
		super();
		this.facilities = facilities;
		this.outfile = filename;
		// always use newest version, currently v1
		this.dtd = "http://www.matsim.org/files/dtd/facilities_v1.dtd";
		this.handler = new FacilitiesWriterHandlerImplV1();
	}

	//////////////////////////////////////////////////////////////////////
	// write methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final void write() {
		this.writeOpenAndInit();
		Iterator<? extends Location> f_it = this.facilities.getLocations().values().iterator();
		while (f_it.hasNext()) {
			Facility f = (Facility)f_it.next();
			this.writeFacility(f);
		}
		this.writeFinish();
	}

	public final void writeOpenAndInit() {
		try {
			this.out = IOUtils.getBufferedWriter(this.outfile);
			writeHeader("facilities");
			this.handler.startFacilities(this.facilities, this.out);
			this.handler.writeSeparator(this.out);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public final void writeFacility(Facility f) {
		try {
			this.handler.startFacility(f, this.out);
			Iterator<Activity> a_it = f.getActivities().values().iterator();
			while (a_it.hasNext()) {
				Activity a = a_it.next();
				this.handler.startActivity(a, this.out);
				this.handler.startCapacity(a, this.out);
				this.handler.endCapacity(this.out);
				Iterator<TreeSet<OpeningTime>> o_set_it = a.getOpentimes().values().iterator();
				while (o_set_it.hasNext()) {
					TreeSet<OpeningTime> o_set = o_set_it.next();
					Iterator<OpeningTime> o_it = o_set.iterator();
					while (o_it.hasNext()) {
						OpeningTime o = o_it.next();
						this.handler.startOpentime(o, this.out);
						this.handler.endOpentime(this.out);
					}
				}
				this.handler.endActivity(this.out);
			}
			this.handler.endFacility(this.out);
			this.handler.writeSeparator(this.out);
			this.out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public final void writeFinish() {
		try {
			this.handler.endFacilities(this.out);
			this.out.flush();
			this.out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
