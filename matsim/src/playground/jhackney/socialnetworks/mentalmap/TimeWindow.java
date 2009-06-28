package playground.jhackney.socialnetworks.mentalmap;

import org.matsim.core.api.population.Person;
import org.matsim.core.population.ActivityImpl;

public class TimeWindow {

		public double startTime;
		public double endTime;
		public Person person;
		public ActivityImpl act;
		/**
		 * A TimeWindow is a single visit to a location by an agent.
		 * 
		 * @author jhackney
		 *
		 */		
		public TimeWindow(double st, double et, Person person, ActivityImpl eventAct){
			//TODO jhackney startTime and endTime are redundant and could be 
			// extracted from the eventAct; they do not need to be here
			this.startTime=st;
			this.endTime=et;
			this.person=person;
			this.act=eventAct;
		}
	}
