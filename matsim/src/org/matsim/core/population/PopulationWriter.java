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

package org.matsim.core.population;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.internal.MatsimFileWriter;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.knowledges.ActivitySpace;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.Desires;
import org.matsim.population.algorithms.PersonAlgorithm;

public class PopulationWriter extends MatsimXmlWriter implements MatsimFileWriter, PersonAlgorithm {

	private final double write_person_fraction;
	private boolean fileOpened = false;

	private PopulationWriterHandler handler = new PopulationWriterHandlerImplV4();
	private final Population population;
	private Knowledges knowledges = null;
	
	private final static Logger log = Logger.getLogger(PopulationWriter.class);

	/**
	 * Creates a new PlansWriter to write out the specified plans to the file and with version
	 * as specified in the {@linkplain org.matsim.core.config.groups.PlansConfigGroup configuration}.
	 * If plans-streaming is on, the file will already be opened and the file-header be written.
	 * If plans-streaming is off, the file will not be created until {@link #write()} is called.
	 *
	 * @param population the population to write to file
	 */
	public PopulationWriter(final Population population) {
		this(population, (Gbl.getConfig() == null ? 1.0 : Gbl.getConfig().plans().getOutputSample()));
	}

	/**
	 * Creates a new PlansWriter to write out the specified plans to the specified file and with
	 * the specified version.
	 * If plans-streaming is on, the file will already be opened and the file-header be written.
	 * If plans-streaming is off, the file will not be created until {@link #write()} is called.
	 *
	 * @param population the population to write to file
	 * @param fraction of persons to write to the plans file
	 */
	public PopulationWriter(final Population population, final double fraction) {
		super();
		this.population = population;
		this.write_person_fraction = fraction;
	}

	public PopulationWriter(final Population pop, final Knowledges knowledges2) {
		this(pop);
		this.knowledges = knowledges2;
	}
	
	public void startStreaming(final String filename) {
		if ((this.population instanceof PopulationImpl) && (((PopulationImpl) this.population).isStreaming())) {
			// write the file head if it is used with streaming.
			writeStartPlans(filename);
		} else {
			log.error("Cannot start streaming. Streaming must be activated in the Population.");
		}
	}
	
	public void closeStreaming() {
		if ((this.population instanceof PopulationImpl) && (((PopulationImpl) this.population).isStreaming())) {
			if (this.fileOpened) {
				writeEndPlans();
			} else {
				log.error("Cannot close streaming. File is not open.");
			}
		} else {
			log.error("Cannot close streaming. Streaming must be activated in the Population.");
		}
	}

	public final void setWriterHandler(final PopulationWriterHandler handler) {
		this.handler = handler;
	}

	//////////////////////////////////////////////////////////////////////
	// write methods
	//////////////////////////////////////////////////////////////////////

	public final void writeStartPlans(final String filename) {
		try {
			openFile(filename);
			this.fileOpened = true;
			this.handler.writeHeaderAndStartElement(this.writer);
			this.handler.startPlans(this.population, this.writer);
			this.handler.writeSeparator(this.writer);
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	public final void writePerson(final Person person) {
		//	 write only the defined fraction
		if ((this.write_person_fraction < 1.0) && (MatsimRandom.getRandom().nextDouble() >= this.write_person_fraction)) {
			return;
		}
		try {
			this.handler.startPerson(person,this.writer);
			if (person instanceof PersonImpl) {
				PersonImpl p = (PersonImpl)person;
				// travelcards
				if (p.getTravelcards() != null) {
					Iterator<String> t_it = p.getTravelcards().iterator();
					while (t_it.hasNext()) {
						String t = t_it.next();
						this.handler.startTravelCard(t,this.writer);
						this.handler.endTravelCard(this.writer);
					}
				}
				// desires
				if (p.getDesires() != null) {
					Desires d = p.getDesires();
					this.handler.startDesires(d,this.writer);
					if (d.getActivityDurations() != null) {
						for (String act_type : d.getActivityDurations().keySet()) {
							this.handler.startActDur(act_type,d.getActivityDurations().get(act_type),this.writer);
							this.handler.endActDur(this.writer);
						}
					}
					this.handler.endDesires(this.writer);
				}
				// knowledge
				if ((this.knowledges != null) && (this.knowledges.getKnowledgesByPersonId().get(p.getId()) != null)) {
					KnowledgeImpl k = this.knowledges.getKnowledgesByPersonId().get(p.getId());
					this.handler.startKnowledge(k, this.writer);
					// activity spaces
					if (k.getActivitySpaces() != null) {
						Iterator<ActivitySpace> as_it = k.getActivitySpaces().iterator();
						while (as_it.hasNext()) {
							ActivitySpace as = as_it.next();
							if (!as.isComplete()) {
								Gbl.errorMsg("[person_id="+p.getId()+" holds an incomplete act-space.]");
							}
							this.handler.startActivitySpace(as, this.writer);
							// params
							Iterator<String> name_it = as.getParams().keySet().iterator();
							while (name_it.hasNext()) {
								String name = name_it.next();
								Double val = as.getParams().get(name);
								this.handler.startParam(name, val.toString(), this.writer);
								this.handler.endParam(this.writer);
							}
							this.handler.endActivitySpace(this.writer);
						}
					}
					// activities
					Iterator<String> at_it = k.getActivityTypes().iterator();
					while (at_it.hasNext()) {
						String act_type = at_it.next();
						this.handler.startActivity(act_type,this.writer);
						// locations (primary)
						for (ActivityOptionImpl a : k.getActivities(act_type,true)) {
							this.handler.startPrimaryLocation(a,this.writer);
							this.handler.endPrimaryLocation(this.writer);
						}
						// locations (secondary)
						for (ActivityOptionImpl a : k.getActivities(act_type,false)) {
							this.handler.startSecondaryLocation(a,this.writer);
							this.handler.endSecondaryLocation(this.writer);
						}
//					Iterator<Activity> a_it = k.getActivities(act_type).iterator();
//					while (a_it.hasNext()) {
//						Facility f = a_it.next().getFacility();
//						this.handler.startLocation(f,this.writer);
//						/* TODOx [balmermi] Here, usually capacity and opentimes
//						 * are also written. But since it is now already defined by the facilities
//						 * there is no need to write it. the act type and the facilitiy id
//						 * is enough. (well... i think) */
//						this.handler.endLocation(this.writer);
//					}
						this.handler.endActivity(this.writer);
					}
					this.handler.endKnowledge(this.writer);
				}



			}
			// plans
			for (Plan plan : person.getPlans()) {
				this.handler.startPlan(plan, this.writer);
				// act/leg
				for (Object pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						this.handler.startAct(act, this.writer);
						this.handler.endAct(this.writer);
					}
					else if (pe instanceof Leg) {
						Leg leg = (Leg) pe;
						this.handler.startLeg(leg, this.writer);
						// route
						Route route = leg.getRoute();
						if (route != null) {
							this.handler.startRoute(route, this.writer);
							this.handler.endRoute(this.writer);
						}
						this.handler.endLeg(this.writer);
					}
				}
				this.handler.endPlan(this.writer);
			}
			this.handler.endPerson(this.writer);
			this.handler.writeSeparator(this.writer);
			this.writer.flush();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	public final void writePersons() {
		for (Person p : this.population.getPersons().values()) {
			writePerson(p);
		}
	}

	public final void writeEndPlans() {
		if (this.fileOpened) {
			try {
				this.handler.endPlans(this.writer);
				this.writer.flush();
				this.writer.close();
			}
			catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}
	}



	/**
	 * Writes all plans to the file. If plans-streaming is on, this will end the writing and close the file.
	 */
	private void write(final String filename) {
//		if ((this.population instanceof PopulationImpl) && (((PopulationImpl) this.population).isStreaming())) {
//			log.info("PlansStreaming is on -- plans already written, just closing file if it's open.");
//			if (this.fileOpened) {
//				writeEndPlans();
//			}
//		} else {
			this.writeStartPlans(filename);
			this.writePersons();
			this.writeEndPlans();
//		}
	}

	public void writeFileV0(final String filename) {
		this.handler = new PopulationWriterHandlerImplV0();
		write(filename);
	}
	
	public void writeFileV4(final String filename) {
		this.handler = new PopulationWriterHandlerImplV4();
		write(filename);
	}

	/**
	 * Writes all plans to the file.
	 * 
	 * @param filename path to the file.
	 */
	public void writeFile(final String filename){
		write(filename);
		log.info("Population written to: " + filename);
	}

	public PopulationWriterHandler getHandler() {
		return this.handler;
	}

	public BufferedWriter getWriter() {
		return this.writer;
	}
	
	// implementation of PersonAlgorithm
	// this is primarily to use the PlansWriter with filters and other algorithms.
	public void run(final Person person) {
		writePerson(person);
	}
}
