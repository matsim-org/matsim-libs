package playground.balac.retailers.preprocess;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;

import playground.balac.retailers.utils.ActivityDifferentiatorBalac;


public class ModifyDesires {
	
	private final static Logger log = Logger.getLogger(ActivityDifferentiatorBalac.class);
	private Activity act;
	private Person person;

	public ModifyDesires(final Activity act,Person person) {
		this.act = act;
		this.person = person;
	}
	
	public void run() {
		throw new UnsupportedOperationException( "Desires do not exist anymore. Please use object attributes" );
		//if (!(this.person.getDesires().getActivityDuration(act.getType()) > 0)) {
		//	Double duration = this.person.getDesires().getActivityDuration("shop");
		//	if (duration > 0) {
		//		this.person.createDesires(act.getType());
		//
		//		this.person.getDesires().putActivityDuration(act.getType(),duration);
		//		this.person.getDesires().removeActivityDuration("shop");
		//	}
		//	else {
		//		if (act.getType().contains("nongrocery")) {
		//			this.person.createDesires(act.getType());
		//			this.person.getDesires().putActivityDuration(act.getType(),this.person.getDesires().getActivityDuration("shopgrocery"));
		//		}
		//		else {
		//			this.person.createDesires(act.getType());
		//			this.person.getDesires().putActivityDuration(act.getType(),this.person.getDesires().getActivityDuration("nongrocery"));
		//		}
		//	}
		//}
		//
		//Double duration = this.person.getDesires().getActivityDuration("shop");
		//if (duration > 0) {
		//	this.person.createDesires(act.getType());
		//	this.person.getDesires().putActivityDuration(act.getType(),duration);
		//	this.person.getDesires().removeActivityDuration("shop");
		//}
		//else {
		//
		//}
	}
}
