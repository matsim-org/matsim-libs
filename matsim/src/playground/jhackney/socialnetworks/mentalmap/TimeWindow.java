package playground.jhackney.socialnetworks.mentalmap;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;


public class TimeWindow {

		public double startTime;
		public double endTime;
		public PersonImpl person;
		public ActivityImpl act;
		/**
		 * A TimeWindow is a single visit to a location by an agent.
		 * 
		 * @author jhackney
		 *
		 */		
		public TimeWindow(double st, double et, Person person, Activity eventAct){
			//TODO jhackney startTime and endTime are redundant and could be 
			// extracted from the eventAct; they do not need to be here
			this.startTime=st;
			this.endTime=et;
			this.person=(PersonImpl) person;
			this.act=(ActivityImpl) eventAct;
		}
	}
