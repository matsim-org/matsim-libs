package org.matsim.socialnetworks.mentalmap;

import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;

public class TimeWindow {

		public double startTime;
		public double endTime;
		public Person person;
		public Activity act;
		/**
		 * A TimeWindow is a single visit to a location by an agent.
		 * 
		 * @author jhackney
		 *
		 */		
		public TimeWindow(double st, double et, Person person, Activity eventAct){
			this.startTime=st;
			this.endTime=et;
			this.person=person;
			this.act=eventAct;
		}
	}
