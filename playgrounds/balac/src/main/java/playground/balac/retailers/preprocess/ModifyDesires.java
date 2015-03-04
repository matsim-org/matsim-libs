package playground.balac.retailers.preprocess;

import org.apache.log4j.Logger;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;

import playground.balac.retailers.utils.ActivityDifferentiatorBalac;


public class ModifyDesires {
	
	private final static Logger log = Logger.getLogger(ActivityDifferentiatorBalac.class);
	private ActivityImpl act;
	private PersonImpl person;

	public ModifyDesires(final ActivityImpl act,PersonImpl person) {
		this.act = act;
		this.person = person;
	}
	
	public void run() {
		if (!(this.person.getDesires().getActivityDuration(act.getType()) > 0)) {
			Double duration = this.person.getDesires().getActivityDuration("shop");
			if (duration > 0) {
				this.person.createDesires(act.getType());
				
				this.person.getDesires().putActivityDuration(act.getType(),duration);
				this.person.getDesires().removeActivityDuration("shop");
			}
			else {
				if (act.getType().contains("nongrocery")) {
					this.person.createDesires(act.getType());
					this.person.getDesires().putActivityDuration(act.getType(),this.person.getDesires().getActivityDuration("shopgrocery"));
				}
				else {
					this.person.createDesires(act.getType());
					this.person.getDesires().putActivityDuration(act.getType(),this.person.getDesires().getActivityDuration("nongrocery"));
				}
			}
		}
		
		Double duration = this.person.getDesires().getActivityDuration("shop");
		if (duration > 0) {
			this.person.createDesires(act.getType());
			this.person.getDesires().putActivityDuration(act.getType(),duration);
			this.person.getDesires().removeActivityDuration("shop");
		}
		else {
			
		}
	}
}
