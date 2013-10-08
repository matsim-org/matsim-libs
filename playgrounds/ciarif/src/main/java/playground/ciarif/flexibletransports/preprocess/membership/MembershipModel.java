package playground.ciarif.flexibletransports.preprocess.membership;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;

import playground.ciarif.flexibletransports.data.FlexTransPersonImpl;

public class MembershipModel {
	
		static final double	B_Yes_Access_Home = 0.138;
		static final double	B_Yes_Access_Work = 0.0531;
		static final double	B_Yes_Density = 0.0;
		//static final double	B_Yes_Density = 0.165;
		static final double B_Yes_31_45 = 0.416; 
		static final double B_yes_CAR_NEV = 1.08;          
		static final double B_yes_CAR_SOM = 2.54;           
		static final double B_yes_MALE  = 0.204; 
		static final double KONST_NO =  5.82;          
		static final double B_No_18_30 = 0.816;
		static final double B_No_60  =  0.423;

		private final static Logger log = Logger.getLogger(MembershipModel.class);
		
	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////
	
	public MembershipModel(Scenario scenario) {
			//this.scenario=scenario;
		}
	
	/**
	* 
	* @return values 0 or 1.<BR>
	* 
	* the meanings of the values are:<BR>
	* <code>0: The person is member of carSharing<br>
	* <code>1: The person is not member of carSharing<br>
	*/
	public final int calcMembership(FlexTransPersonImpl pi) {
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
	throw new RuntimeException("It should never reach this line!");
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
		protected final double calcNoUtil(FlexTransPersonImpl pi) {
			double util = 0.0;
			util += KONST_NO * 1.0;
			//log.info("age = " + pi.getAge());
			if (pi.getAge() <= 30 & pi.getAge() >= 18 ) { util += B_No_18_30 * 1.0;}
			if (pi.getAge() >= 60 ) { util += B_No_60 * 1.0; }
			log.info("no util = " + util);
			return util;
		}
	
		
		protected final double calcYesUtil(FlexTransPersonImpl pi) {
			double util = 0.0;
			util += B_Yes_Access_Home * pi.getAccessHome();
			util += B_Yes_Access_Work * pi.getAccessWork();
			util += B_Yes_Density * pi.getDensityHome();
			//log.info("car av = " + pi.getCarAvail());
			if (pi.getAge() <= 45 & pi.getAge() >= 31 ) { util += B_Yes_31_45 * 1.0; }
			if (pi.getCarAvail()=="never") { util += B_yes_CAR_NEV * 1.0; }
			if (pi.getCarAvail()=="sometimes") { util += B_yes_CAR_SOM * 1.0; }
			if (pi.getSex()== "m") { util += B_yes_MALE * 1.0; }
			log.info("yes util = " + util);
			return util;
		}
		//B_ALT_31_45 * ALT_31_45 + B_AUTO_TEILW * AUTO_TEILW + B_AUTO_NIE * AUTO_NIE + B_MALE * MALE

}
