package org.matsim.socialnetworks.mentalmap;

import org.matsim.population.Act;
import org.matsim.population.Person;

public class TimeWindow {

		public double startTime;
		public double endTime;
		public Person person;
		public Act act;
		/**
		 * A TimeWindow is a single visit to a location by an agent.
		 * 
		 * @author jhackney
		 *
		 */		
		public TimeWindow(double st, double et, Person person, Act eventAct){
			this.startTime=st;
			this.endTime=et;
			this.person=person;
			this.act=eventAct;
		}
	}
