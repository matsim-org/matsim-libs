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
package playground.thibautd.initialdemandgeneration.old.modules;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.Desires;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * Based on the class of the same name from balmermi.
 *
 * <br>
 * this class does the following:
 * <ul>
 * <li> it sets travel times to 0 (the travel time is "moved" to the activity time)
 * <li> it sets the sum of activity time to 24:00, with a "night at home" duration
 * of at least 4:00 (other activity times are changed if it is not the case)
 * <li> it shifts the whole plan by a random duration of max 00:30 toward the past
 * or the future.
 * </ul>
 * @author thibautd
 */
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

	/**
	 * sets leg travel times to 0, and affects the travel time to the next activity
	 */
	private final void moveTravTimeToNextAct(final Plan p) {
		double prev_ttime = Time.UNDEFINED_TIME;
		double tod = ((ActivityImpl) p.getPlanElements().get(0)).getEndTime();

		for (int i=1; i<p.getPlanElements().size(); i++) {
			PlanElement pe = p.getPlanElements().get(i);
			if (i == p.getPlanElements().size()-1) {
				// last act
				Activity a = (Activity) pe;
				if (prev_ttime == Time.UNDEFINED_TIME) {
					throw new RuntimeException("got undefined travel time!");
				}
				double dur = prev_ttime;

				a.setStartTime(tod);
				a.setMaximumDuration(dur);
				a.setEndTime(tod+dur);

				prev_ttime = Time.UNDEFINED_TIME;
			}
			else if (pe instanceof Activity) {
				// in between acts
				Activity a = (Activity) p.getPlanElements().get(i);
				double dur = a.getMaximumDuration();
				if (prev_ttime == Time.UNDEFINED_TIME) {
					throw new RuntimeException("got undefined travel time!");
				}
				dur += prev_ttime;
				if (dur < 5*60.0) {
					dur = 5*60.0;
				}
				// NOTE: Sometimes the mz act duration is 0 sec.

				a.setStartTime(tod);
				a.setMaximumDuration(dur);
				a.setEndTime(tod+dur);

				tod = a.getEndTime();
				prev_ttime = Time.UNDEFINED_TIME;
			}
			else if (pe instanceof Leg) {
				Leg l = (Leg) p.getPlanElements().get(i);
				prev_ttime = l.getTravelTime();

				l.setDepartureTime(tod);
				l.setTravelTime(0.0);
				if (l instanceof LegImpl) {
					((LegImpl) l).setArrivalTime(tod);
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////

	/**
	 * normalizes activity times so that the plan ends at 24:00, and the
	 * "night" home activity (i.e. the combination of the first and last home
	 * activity) has a minimal duration.
	 */
	private final void normalizeTimes(final Plan p) {
		if (p.getPlanElements().size() == 1) {
			ActivityImpl a = (ActivityImpl) p.getPlanElements().get(0);

			a.setStartTime(0.0);
			a.setMaximumDuration(Time.MIDNIGHT);
			a.setEndTime(Time.MIDNIGHT);
			return;
		}

		double home_dur = ((ActivityImpl) p.getPlanElements().get(0)).getEndTime();
		// time between the first and last activity (CAN include home activities)
		double othr_dur = 0.0;

		for (int i=1; i<p.getPlanElements().size()-1; i=i++) {
			PlanElement pe = p.getPlanElements().get(i);
			if (pe instanceof Activity) {
				othr_dur += ((Activity)p.getPlanElements().get(i)).getMaximumDuration();
			}
		}
		if (othr_dur <= (Time.MIDNIGHT - HOME_MIN)) {
			ActivityImpl a = (ActivityImpl) p.getPlanElements().get(p.getPlanElements().size()-1);
			a.setMaximumDuration(Time.UNDEFINED_TIME);
			a.setEndTime(Time.UNDEFINED_TIME);
			return;
		}

		// home activity not long enough
		// normalize the other activity durations
		log.info("pid="+p.getPerson().getId()+": normalizing times (othr_dur="+Time.writeTime(othr_dur)+").");
		double tod = home_dur;
		for (int i=1; i<p.getPlanElements().size(); i++) {
			PlanElement pe = p.getPlanElements().get(i);
			if (i == p.getPlanElements().size()-1) {
				Activity a = (Activity) pe;

				a.setStartTime(tod);
				a.setMaximumDuration(Time.UNDEFINED_TIME);
				a.setEndTime(Time.UNDEFINED_TIME);
			}
			else if (pe instanceof Activity) {
				Activity a = (Activity) pe;

				a.setStartTime(tod);
				a.setMaximumDuration(
						(Time.MIDNIGHT - HOME_MIN) *
						a.getMaximumDuration()/othr_dur);
				a.setEndTime(tod+a.getMaximumDuration());

				tod = a.getEndTime();
			}
			else if (pe instanceof Leg) {
				Leg l = (Leg) pe;

				l.setDepartureTime(tod);
				l.setTravelTime(0.0);
				if (l instanceof LegImpl) {
					((LegImpl) l).setArrivalTime(tod);
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////

	/**
	 * Creates a "desire" structure, where the desired activity times
	 * correspond to the times extracted from the activity chain
	 */
	private final void assignDesires(final Plan p) {
		Desires d = ((PersonImpl) p.getPerson()).createDesires(null);
		// The method .createDesires does create desires ONLY IF no desires
		// exist! Thus need to clean them.
		// The choosen way to do it will work only if the desires return the internal
		// reference to the duration map. This is currently the case, but nothing in
		// the (lack of) documentation indicates that it should not change...
		// => if a bug pops up here, check for a change in this behaviour.
		d.getActivityDurations().clear();
		double othr_dur = 0.0;
		for (int i=2; i<p.getPlanElements().size()-2; i=i+2) {
			ActivityImpl a = (ActivityImpl) p.getPlanElements().get(i);
			if (a.getMaximumDuration() <= 0.0) {
				log.fatal("pid="+p.getPerson().getId()+": negative activity duration!");
			}
			d.accumulateActivityDuration(
					a.getType(),
					a.getMaximumDuration());
			othr_dur += a.getMaximumDuration();
		}
		double home_dur = Time.MIDNIGHT - othr_dur;
		if (home_dur <= 0.0) {
			throw new RuntimeException("pid="+p.getPerson().getId()+": negative home duration!");
		}
		d.accumulateActivityDuration(
				((ActivityImpl) p.getPlanElements().get(0)).getType(),
				home_dur);
	}

	//////////////////////////////////////////////////////////////////////

	/**
	 * shifts the whole chains by a random duration of maximum 30 mins
	 * toward the past or the future
	 */
	private final void varyTimes(final Plan p) {
		List<? extends PlanElement> acts_legs = p.getPlanElements();

		// draw a new random number until the new end time >= 0.0
		double bias = MatsimRandom.getRandom().nextInt(3600)-1800.0; // [-1800,1800[
		double first_end_time = ((ActivityImpl) acts_legs.get(0)).getEndTime();
		while (first_end_time + bias < 0.0) {
			bias = MatsimRandom.getRandom().nextInt(3600)-1800.0;
		}

		for (int i=0; i<acts_legs.size(); i++) {
			PlanElement pe = acts_legs.get(i);
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				if (i == 0) {
					// first act
					act.setStartTime(0.0);
					act.setMaximumDuration(act.getMaximumDuration() + bias);
					act.setEndTime(act.getEndTime() + bias);
				}
				else if (i == acts_legs.size()-1) {
					// last act
					act.setStartTime(act.getStartTime() + bias);
					act.setMaximumDuration(Time.UNDEFINED_TIME);
					act.setEndTime(Time.UNDEFINED_TIME);
				}
				else {
					// in between acts
					act.setStartTime(act.getStartTime() + bias);
					act.setEndTime(act.getEndTime() + bias);
				}
			}
			else if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				leg.setDepartureTime(leg.getDepartureTime() + bias);
				if (leg instanceof LegImpl) {
					((LegImpl) leg).setArrivalTime(((LegImpl) leg).getArrivalTime() + bias);
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		this.run(person.getSelectedPlan());
	}

	@Override
	public void run(final Plan plan) {
		this.moveTravTimeToNextAct( plan );
		this.normalizeTimes( plan );
		this.assignDesires( plan );
		this.varyTimes( plan );
	}
}
