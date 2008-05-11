/* *********************************************************************** *
 * project: org.matsim.*
 * PlansWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.plans;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.plans.algorithms.PersonAlgorithmI;
import org.matsim.utils.io.IOUtils;
import org.matsim.writer.Writer;

public class PlansWriter extends Writer implements PersonAlgorithmI {

	private final double write_person_percentage;
	private boolean fileOpened = false;
	private boolean useCompression = false;

	private PlansWriterHandler handler = null;
	private final Plans population;
	
	private final static Logger log = Logger.getLogger(PlansWriter.class);

	/**
	 * Creates a new PlansWriter to write out the specified plans to the file and with version
	 * as specified in the {@linkplain org.matsim.config.groups.PlansConfigGroup configuration}.
	 * If plans-streaming is on, the file will already be opened and the file-header be written.
	 * If plans-streaming is off, the file will not be created until {@link #write()} is called.
	 * 
	 * @param population the population to write to file
	 */
	public PlansWriter(final Plans population) {
		this(population, Gbl.getConfig().plans().getOutputFile(), Gbl.getConfig().plans().getOutputVersion());
	}
	
	/**
	 * Creates a new PlansWriter to write out the specified plans to the specified file and with 
	 * the specified version.
	 * If plans-streaming is on, the file will already be opened and the file-header be written.
	 * If plans-streaming is off, the file will not be created until {@link #write()} is called.
	 * 
	 * @param population the population to write to file
	 * @param filename the filename where to write the data
	 * @param version specifies the file-format
	 */
	public PlansWriter(final Plans population, final String filename, final String version) {
		super();
		this.population = population;
		this.outfile = filename;
		this.write_person_percentage = Gbl.getConfig().plans().getOutputSample();
		createHandler(version);
		
		if (this.population.isStreaming()) {
			// write the file head if it is used with streaming.
			writeStartPlans();
		}
	}

	/**
	 * Just a helper method to instantiate the correct handler
	 * @param version
	 */
	private void createHandler(final String version) {
		if (version.equals("v0")) {
			this.dtd = "http://www.matsim.org/files/dtd/plans_v0.dtd";
			this.handler = new PlansWriterHandlerImplV0();
		} else if (version.equals("v4")) {
			this.dtd = "http://www.matsim.org/files/dtd/plans_v4.dtd";
			this.handler = new PlansWriterHandlerImplV4();
		} else {
			throw new IllegalArgumentException("output version \"" + version + "\" not known.");
		}
	}

	public final void setWriterHandler(final PlansWriterHandler handler) {
		this.handler = handler;
	}

	//////////////////////////////////////////////////////////////////////
	// write methods
	//////////////////////////////////////////////////////////////////////

	public final void writeStartPlans() {
		try {
			if (this.useCompression) {
				this.out = IOUtils.getBufferedWriter(this.outfile, true);
			} else {
				this.out = IOUtils.getBufferedWriter(this.outfile);
			}
			this.fileOpened = true;
			this.writeHeader("plans");
			this.handler.startPlans(this.population, this.out);
			this.handler.writeSeparator(this.out);
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	public final void writePerson(final Person p) {
		//	 write only the defined fraction
		if (Gbl.random.nextDouble() >= this.write_person_percentage) {
			return;
		}
		try {
			this.handler.startPerson(p,this.out);
			// travelcards
			Iterator<String> t_it = p.getTravelcards().iterator();
			while (t_it.hasNext()) {
				String t = t_it.next();
				this.handler.startTravelCard(t,this.out);
				this.handler.endTravelCard(this.out);
			}
			// knowledge
			if (p.getKnowledge() != null) {
				Knowledge k = p.getKnowledge();
				this.handler.startKnowledge(k, this.out);
				// activity spaces
				Iterator<ActivitySpace> as_it = k.getActivitySpaces().iterator();
				while (as_it.hasNext()) {
					ActivitySpace as = as_it.next();
					if (!as.isComplete()) {
						Gbl.errorMsg("[person_id="+p.getId()+" holds an incomplete act-space.]");
					}
					this.handler.startActivitySpace(as, this.out);
					// params
					Iterator<String> name_it = as.getParams().keySet().iterator();
					while (name_it.hasNext()) {
						String name = name_it.next();
						Double val = as.getParams().get(name);
						this.handler.startParam(name, val.toString(), this.out);
						this.handler.endParam(this.out);
					}
					this.handler.endActivitySpace(this.out);
				}
				// activities
				Iterator<String> at_it = k.getActivityTypes().iterator();
				while (at_it.hasNext()) {
					String act_type = at_it.next();
					this.handler.startActivity(act_type,this.out);
					// locations
					Iterator<Activity> a_it = k.getActivities(act_type).iterator();
					while (a_it.hasNext()) {
						Facility f = a_it.next().getFacility();
						this.handler.startLocation(f,this.out);
						/* TODO [balmermi] Here, usually capacity and opentimes
						 * are also written. But since it is now already defined by the facilities
						 * there is no need to write it. the act type and the facilitiy id
						 * is enough. (well... i think) */
						this.handler.endLocation(this.out);
					}
					this.handler.endActivity(this.out);
				}
				this.handler.endKnowledge(this.out);
			}
			// plans
			for (int ii = 0; ii < p.getPlans().size(); ii++) {
				Plan plan = p.getPlans().get(ii);
				this.handler.startPlan(plan, this.out);
				// act/leg
				for (int jj = 0; jj < plan.getActsLegs().size(); jj++) {
					if (jj % 2 == 0) {
						Act act = (Act)plan.getActsLegs().get(jj);
						this.handler.startAct(act, this.out);
						this.handler.endAct(this.out);
					}
					else {
						Leg leg = (Leg)plan.getActsLegs().get(jj);
						this.handler.startLeg(leg, this.out);
						// route
						if (leg.getRoute() != null) {
							Route r = leg.getRoute();
							this.handler.startRoute(r, this.out);
							this.handler.endRoute(this.out);
						}
						this.handler.endLeg(this.out);
					}
				}
				this.handler.endPlan(this.out);
			}
			this.handler.endPerson(this.out);
			this.handler.writeSeparator(this.out);
			this.out.flush();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	public final void writePersons() {
		Iterator<Person> p_it = this.population.getPersons().values().iterator();
		while (p_it.hasNext()) {
			Person p = p_it.next();
			writePerson(p);
		}
	}

	public final void writeEndPlans() {
		if (this.fileOpened) {
			try {
				this.handler.endPlans(this.out);
				this.out.flush();
				this.out.close();
			}
			catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}
	}

	/**
	 * Writes all plans to the file. If plans-streaming is on, this will end the writing and close the file.
	 */
	@Override
	public void write() {
		if (!this.population.isStreaming()) {
			this.writeStartPlans();
			this.writePersons();
			this.writeEndPlans();
		} else {
			log.info("PlansStreaming is on -- plans already written, just closing file if it's open.");
			if (this.fileOpened) {
				writeEndPlans();
			}
		}
	}

	public void setUseCompression(final boolean compress) {
		this.useCompression = compress;
	}

	public PlansWriterHandler getHandler() {
		return this.handler;
	}

	@Override
	public final String toString() {
		return super.toString();
	}


	// implementation of PersonAlgorithmI
	// this is primarily to use the PlansWriter with filters and other algorithms.
	public void run(final Person person) {
		writePerson(person);
	}
}
