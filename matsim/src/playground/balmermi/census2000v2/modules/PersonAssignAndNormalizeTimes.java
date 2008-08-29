/* *********************************************************************** *
 * project: org.matsim.*
 * PersonLicenseModel.java
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

package playground.balmermi.census2000v2.modules;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.population.Act;
import org.matsim.population.Desires;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationReaderMatsimV4;
import org.matsim.population.PopulationWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.misc.Time;

public class PersonAssignAndNormalizeTimes extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PersonAssignAndNormalizeTimes.class);

	private static final double HOME_MIN = 4*3600;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonAssignAndNormalizeTimes() {
		log.info("    init " + this.getClass().getName() + " module...");
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////
	
	private final void moveTravTimeToNextAct(Plan p) {
		double prev_ttime = Time.UNDEFINED_TIME;
		double tod = ((Act)p.getActsLegs().get(0)).getEndTime();
		for (int i=1; i<p.getActsLegs().size(); i++) {
			if (i == p.getActsLegs().size()-1) { // last act
				Act a = (Act)p.getActsLegs().get(i);
				if (prev_ttime == Time.UNDEFINED_TIME) { Gbl.errorMsg("That must not happen!"); }
				double dur = prev_ttime;
				a.setStartTime(tod); a.setDur(dur); a.setEndTime(tod+dur);
				prev_ttime = Time.UNDEFINED_TIME;
			}
			else if (i % 2 == 0) { // in between acts
				Act a = (Act)p.getActsLegs().get(i);
				double dur = a.getDur();
				if (prev_ttime == Time.UNDEFINED_TIME) { Gbl.errorMsg("That must not happen!"); }
				dur += prev_ttime;
				if (dur < 5*60.0) { dur = 5*60.0; } // NOTE: Sometimes the mz act duration is 0 sec.
				a.setStartTime(tod); a.setDur(dur); a.setEndTime(tod+dur);
				tod = a.getEndTime();
				prev_ttime = Time.UNDEFINED_TIME;
			}
			else {
				Leg l = (Leg)p.getActsLegs().get(i);
				prev_ttime = l.getTravTime();
				l.setDepTime(tod); l.setTravTime(0.0); l.setArrTime(tod);
			}
		}
	}
	
	//////////////////////////////////////////////////////////////////////

	private final void normalizeTimes(Plan p) {
		if (p.getActsLegs().size() == 1) {
			Act a = (Act)p.getActsLegs().get(0);
			a.setStartTime(0.0); a.setDur(Time.MIDNIGHT); a.setEndTime(Time.MIDNIGHT);
			return;
		}
		double home_dur = ((Act)p.getActsLegs().get(0)).getEndTime();
		double othr_dur = 0.0;
		for (int i=2; i<p.getActsLegs().size()-2; i=i+2) { othr_dur += ((Act)p.getActsLegs().get(i)).getDur(); }
		if (othr_dur <= (Time.MIDNIGHT - HOME_MIN)) {
			Act a = (Act)p.getActsLegs().get(p.getActsLegs().size()-1);
			a.setDur(Time.UNDEFINED_TIME); a.setEndTime(Time.UNDEFINED_TIME);
			return;
		}
		
		// normalize the other activity durations
		log.info("pid="+p.getPerson().getId()+": normalizing times (othr_dur="+Time.writeTime(othr_dur)+").");
		double tod = home_dur;
		for (int i=1; i<p.getActsLegs().size(); i++) {
			if (i == p.getActsLegs().size()-1) {
				Act a = (Act)p.getActsLegs().get(i);
				a.setStartTime(tod); a.setDur(Time.UNDEFINED_TIME); a.setEndTime(Time.UNDEFINED_TIME);
			}
			else if (i % 2 == 0) {
				Act a = (Act)p.getActsLegs().get(i);
				a.setStartTime(tod);
				a.setDur((Time.MIDNIGHT - HOME_MIN)*a.getDur()/othr_dur);
				a.setEndTime(tod+a.getDur());
				tod = a.getEndTime();
			}
			else {
				Leg l = (Leg)p.getActsLegs().get(i);
				l.setDepTime(tod); l.setTravTime(0.0); l.setArrTime(tod);
			}
		}
	}
	
	//////////////////////////////////////////////////////////////////////

	private final void assignDesires(Plan p) {
		Desires d = p.getPerson().createDesires(null);
		double othr_dur = 0.0;
		for (int i=2; i<p.getActsLegs().size()-2; i=i+2) {
			Act a = (Act)p.getActsLegs().get(i);
			if (a.getDur() <= 0.0) { log.fatal("pid="+p.getPerson().getId()+": That must not happen!"); }
			d.accumulateActivityDuration(a.getType(),a.getDur());
			othr_dur += a.getDur();
		}
		double home_dur = Time.MIDNIGHT - othr_dur;
		if (home_dur <= 0.0) { Gbl.errorMsg("pid="+p.getPerson().getId()+": That must not happen!"); }
		d.accumulateActivityDuration(((Act)p.getActsLegs().get(0)).getType(),home_dur);
	}
	
	//////////////////////////////////////////////////////////////////////

	private final void varyTimes(Plan p) {
		ArrayList<Object> acts_legs = p.getActsLegs();

		// draw a new random number until the new end time >= 0.0
		double bias = MatsimRandom.random.nextInt(3600)-1800.0; // [-1800,1800[
		double first_end_time = ((Act)acts_legs.get(0)).getEndTime();
		while (first_end_time+bias < 0.0) { bias = MatsimRandom.random.nextInt(3600)-1800.0; }
		
		for (int i=0; i<acts_legs.size(); i++) {
			if (i % 2 == 0) {
				Act act = (Act)acts_legs.get(i);
				if (i == 0) { // first act
					act.setStartTime(0.0);
					act.setDur(act.getDur()+bias);
					act.setEndTime(act.getEndTime()+bias);
				}
				else if (i == acts_legs.size()-1) { // last act
					act.setStartTime(act.getStartTime()+bias);
					act.setDur(Time.UNDEFINED_TIME);
					act.setEndTime(Time.UNDEFINED_TIME);
				}
				else { // in between acts
					act.setStartTime(act.getStartTime()+bias);
					act.setEndTime(act.getEndTime()+bias);
				}
			}
			else {
				Leg leg = (Leg)acts_legs.get(i);
				leg.setDepTime(leg.getDepTime()+bias);
				leg.setArrTime(leg.getArrTime()+bias);
			}
		} 
	}
	
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		this.run(person.getSelectedPlan());
	}

	public void run(Plan plan) {
		this.moveTravTimeToNextAct(plan);
		this.normalizeTimes(plan);
		this.assignDesires(plan);
		this.varyTimes(plan);
	}
}
