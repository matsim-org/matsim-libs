package playground.ciarif.flexibletransports.preprocess;


import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;


public class MembershipModel {
//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	
	private final static Logger log = Logger.getLogger(MembershipModel.class);
	private ScenarioImpl scenario;
	static final double B_Yes_31_45 = 0.495; 
	static final double B_yes_CAR_NEV = 1.61;          
	static final double B_yes_CAR_SOM = 1.03;           
	static final double B_yes_MALE  = 0.142; 
	static final double KONST_NO =  4.91;          
	static final double B_No_18_30 = 0.689;
	static final double B_No_60  =  0.359;  
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public MembershipModel(ScenarioImpl scenario) {
		this.scenario = scenario;
	}


	
	
	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	/**
	 * 
	 * @return values 0 or 1.<BR>
	 * 
	 * the meanings of the values are:<BR>
	 * <code>0: The person is member of carSharing<br>
	 * <code>1: The person is not member of carSharing<br>
	 */
	public final int calcMembership(PersonImpl pi) {
		double[] utils = new double[2];
		utils[0] = this.calcYesUtil(pi);
		utils[1] = this.calcNoUtil(pi);
		double [] probs = this.calcLogitProbability(utils);
		double r = MatsimRandom.getRandom().nextDouble();

		double prob_sum = 0.0;
		for (int i=0; i<probs.length; i++) {
			prob_sum += probs[i];
			if (r < prob_sum) { return i; }
		}
		Gbl.errorMsg("It should never reach this line!");
		return -1;
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods (private)
	//////////////////////////////////////////////////////////////////////

	private final double[] calcLogitProbability(double[] utils) {
		double exp_sum = 0.0;
		for (int i=0; i<utils.length; i++) { exp_sum += Math.exp(utils[i]); }
		double [] probs = new double[utils.length];
		for (int i=0; i<utils.length; i++) { probs[i] = Math.exp(utils[i])/exp_sum; }
		return probs;
	}
	protected final double calcNoUtil(PersonImpl pi) {
		double util = 0.0;
		util += KONST_NO * 1.0;
		if (pi.getAge() <= 30 & pi.getAge() >= 18 ) { util += B_No_18_30 * 1.0; }
		if (pi.getAge() >= 60 ) { util += B_No_60 * 1.0; }
		return util;
	}

	
	protected final double calcYesUtil(PersonImpl pi) {
		double util = 0.0;
		if (pi.getAge() <= 45 & pi.getAge() >= 31 ) { util += B_Yes_31_45 * 1.0; }
		if (pi.getCarAvail()=="Never") { util += B_yes_CAR_NEV * 1.0; }
		if (pi.getCarAvail()=="Sometimes") { util += B_yes_CAR_SOM * 1.0; }
		if (pi.getSex()== "m") { util += B_yes_MALE * 1.0; }
		
		return util;
	}
	//B_ALT_31_45 * ALT_31_45 + B_AUTO_TEILW * AUTO_TEILW + B_AUTO_NIE * AUTO_NIE + B_MALE * MALE

	public void run() {
		//this.init();
		this.modifyPlans();		
	}

	private void modifyPlans() {
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			PersonImpl pi = (PersonImpl)person;
			if (person.getPlans().size() > 1) {
				log.error("More than one plan for person: " + pi.getId());
			}
			if (pi.getLicense().equalsIgnoreCase("yes")) {this.assignCarSharingMembership(pi);}
			else {pi.addTravelcard("unknown");}
			PlanImpl selectedPlan = (PlanImpl)pi.getSelectedPlan();
			final List<? extends PlanElement> actslegs = selectedPlan.getPlanElements();
			for (int j = 1; j < actslegs.size(); j=j+2) {
				final LegImpl leg = (LegImpl)actslegs.get(j);
				if (leg.getMode().startsWith("ride")& pi.getTravelcards().equals("ch-HT-mobility")) {
					leg.setMode("carsharing");
				}
			}
		}
		
	}

	private void assignCarSharingMembership(PersonImpl pi) {
		int choice = calcMembership (pi);
		log.info("Processing person " + pi.getId());
		if (choice == 0) {
			pi.addTravelcard("ch-HT-mobility");
		}
		else {
			pi.addTravelcard("unknown");
		}
		log.info("travelcards = " + pi.getTravelcards());
	}
		

}
