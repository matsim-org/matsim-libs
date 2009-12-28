/* *********************************************************************** *
 * project: org.matsim.*
 * ModeChoiceAlgorithm.java
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

package playground.ciarif.modechoice_old;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;


/**
 * @author ciarif
 *
 */
public class ModeChoiceAlgorithm extends AbstractPersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ModeChoiceAlgorithm() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.matsim.demandmodeling.plans.algorithms.PersonAlgorithm#run(org.matsim.demandmodeling.plans.Person)
	 */
	@Override
	public void run(Person person) {
			UtilityComputer2[] mobilityUtilityComputers;

			final int alternativeCount = 5;

			mobilityUtilityComputers = new UtilityComputer2[alternativeCount*3];

			double[] alternativeProbability = new double[alternativeCount];
			double sumOfProbabilities = 0;

			mobilityUtilityComputers[0] = new WalkToWork();
			mobilityUtilityComputers[1] = new BikeToWork();
			mobilityUtilityComputers[2] = new CarToWork();
			mobilityUtilityComputers[3] = new PublicToWork();
			mobilityUtilityComputers[4] = new OtherToWork();

			mobilityUtilityComputers[5] = new WalkToEducation();
			mobilityUtilityComputers[6] = new BikeToEducation();
			mobilityUtilityComputers[7] = new CarToEducation();
			mobilityUtilityComputers[8] = new PublicToEducation();
			mobilityUtilityComputers[9] = new OtherToEducation();

			mobilityUtilityComputers[10] = new WalkToShop();
			mobilityUtilityComputers[11] = new BikeToShop();
			mobilityUtilityComputers[12] = new CarToShop();
			mobilityUtilityComputers[13] = new PublicToShop();
			mobilityUtilityComputers[14] = new OtherToShop();

			double[] utilities = new double[alternativeCount];
			int b = (5*detectTourMainActivity2(person));

				for (int i = 0; i < alternativeCount; i++) {
					utilities[i] = mobilityUtilityComputers[i+b].computeUtility(person);
				}

				for (int j = 0; j < alternativeCount; j++) {
					alternativeProbability[j] = getLogitProbability(utilities[j], utilities);
				}


			double r = MatsimRandom.getRandom().nextDouble();
			int index = 0;
			sumOfProbabilities = alternativeProbability[index];
			// Pick the right transportation mean for the person according to the logit probability
			while (r >= sumOfProbabilities) {
				index++;
				sumOfProbabilities += alternativeProbability[index];
			}
			addMobilityInformation(person, index);
		}

		private void addMobilityInformation(Person person, int index) {
			/* Index		Mode information
			 * 0			Walk
			 * 1			Bicycle
			 * 2			Car
			 * 3			Public Trasnport
			 * 4			Other means */

			Plan plan = person.getSelectedPlan();
			List<? extends PlanElement> acts_legs = plan.getPlanElements();
			if (index == 0) {
				for (int i=1; i < acts_legs.size()-1; i=i+2) {
					LegImpl leg = (LegImpl)acts_legs.get(i);
					leg.setMode(TransportMode.walk);
				}
			} else if (index == 1) {
				for (int i=1; i < acts_legs.size()-1; i=i+2) {
					LegImpl leg = (LegImpl)acts_legs.get(i);
					leg.setMode(TransportMode.bike);
				}
			} else if (index == 2) {
				for (int i=1; i < acts_legs.size()-1; i=i+2) {
					LegImpl leg = (LegImpl)acts_legs.get(i);
					leg.setMode(TransportMode.car);
					// Modify because now also persons without driving license can have the option car available.
					//which is correct, but they don't produce traffic,since they get a ride from someone else
					//who is already accounted for.
				}
			} else if (index == 3) {
				for (int i=1; i < acts_legs.size()-1; i=i+2) {
					LegImpl leg = (LegImpl)acts_legs.get(i);
					leg.setMode(TransportMode.pt);
				}
			} else if (index == 4) {
				for (int i=1; i < acts_legs.size()-1; i=i+2) {
					LegImpl leg = (LegImpl)acts_legs.get(i);
					leg.setMode(TransportMode.undefined);
				}
			}
		}

	private double getLogitProbability(double referenceUtility,double[] utilities) {
		double expSumOfAlternatives = 0.0;
		for (double utility : utilities) {
			expSumOfAlternatives += Math.exp(utility);
		}
		return Math.exp(referenceUtility) / expSumOfAlternatives;
	}

	private double getAge2(Person p) {
		return getAge(p) * getAge(p);
	}

	protected double getAge(Person p) {
		return ((PersonImpl) p).getAge();
	}


//	private double getSex(Person p) {
//		if (p.getSex().equals("m")) {
//			return 1;
//		} else {
//			return 0;
//		}
//	}

	private double getHasLicense(Person p) {
		if (((PersonImpl) p).getLicense().equals("yes")) {
			return 1;
		}
		// else...
		return 0;
	}
	private double getTravelcards(Person p) {
		if (((PersonImpl) p).getTravelcards().equals("yes")) {
			return 1;
		}
		// else...
		return 0;
	}

	private double getCarAlternativeAvail(Person p) {
		if (((PersonImpl) p).getCarAvail().equals("always")) {
			return 1;
		}

		if (((PersonImpl) p).getLicense().equals("yes")){
			double r1 = MatsimRandom.getRandom().nextDouble();
			if (r1 < 0.34 ) {
				return 0;
			}
			// else...
			return 1;
		}
		// else...
		return 0;
	}

	private double getCarAvailPerson(Person p) {
		if (((PersonImpl) p).getCarAvail().equals("always")) {
			return 1;
		}
		// else...
		return 0;
	}

	public double calcDist (Person person) {

		double dist=0;
		Plan plan = person.getSelectedPlan();
		if (plan == null) {
			return 0;
		}
		List<? extends PlanElement> acts_legs = plan.getPlanElements();

		for (int i=2; i<acts_legs.size(); i=i+2) {
			Coord coord1 = ((Activity)acts_legs.get(i)).getCoord();
			Coord coord2 = ((Activity)acts_legs.get(i-2)).getCoord();
			dist = dist + CoordUtils.calcDistance(coord1, coord2);
		}
		return dist / 1000;
	}

	public String detectTourMainActivity1 (PersonImpl person){

		String main_type = "o";
		Plan plan = person.getSelectedPlan();
		List<? extends PlanElement> acts_legs = plan.getPlanElements();

		for (int i=2; i<acts_legs.size(); i=i+2) {
			String type = ((ActivityImpl)acts_legs.get(i)).getType();

			if (main_type == "e"){
				if (type == "w") {
					main_type = type;
					break;
				}
			}
			else if (main_type == "s") {
				if ((type == "w") || (type == "e")) {
					main_type = type;
				}
			}
			else if (main_type == "o") {
				if ((type == "w")||(type == "s")||(type == "e")) {
					main_type = type;
				}
			}
		}
		return main_type;
	}

public int detectTourMainActivity2 (Person person){

		int main_type = 2;
		Plan plan = person.getSelectedPlan();
		List<? extends PlanElement> acts_legs = plan.getPlanElements();

		for (int i=2; i<acts_legs.size(); i=i+2) {
			String type = ((ActivityImpl)acts_legs.get(i)).getType();

			if (main_type == 1){
				if (type == "w") {
					main_type = 0;
					break;
				}
			}
			else if (main_type == 2) {
				if (type == "w") {
					main_type = 0;
					break;
				}
				else if (type == "e") {
					main_type = 1;
				}
			}
//			else if (main_type == 3) {
//				if (type == "w") {
//					main_type = 0;
//					break;
//				}
//				else if (type == "e") {
//					main_type = 1;
//				}
//				else if (type == "s") {
//					main_type = 2;
//				}
//			}
		}
		return main_type;
	}

	class WalkToWork implements UtilityComputer2 {
		static final double B_Dist_w = -3.2773065e-001;
		static final double B_Const_w = +2.4705414e+000;

		/**
		 * Computes for this person the utility of choosing walk as transportation mode
		 * when the tour (plan) has work as main purpose
		 * @param p
		 * @return
		 */
		public double computeUtility(Person p) {
			double T_DIST = calcDist(p);

			return B_Const_w* 1 + B_Dist_w * T_DIST;
		}
	}


	class BikeToWork implements UtilityComputer2 {
		static final double B_Dist_b = -6.7554472e-002;
		static final double B_Const_b = +1.2865056e+000;

		/**
		 * Computes for this person the utility of choosing bicycle as transportation mode
		 * when the tour (plan) has work as main purpose
		 * @param p
		 * @return
		 */
		public double computeUtility(Person p) {
			double T_DIST = calcDist(p);
			double r2 = MatsimRandom.getRandom().nextDouble();

			if (r2 <0.44){
				return B_Const_b * 1 + B_Dist_b * T_DIST;
			}
			// else ...
			return -10000;
		}
	}





	class CarToWork implements UtilityComputer2 {
		static final double B_Dist_c = +1.8278377e-002;
		static final double B_Const_c = +1.8875411e-001;
		static final double B_Lic_c = +3.9967454e-001;
		static final double B_T2_c = +4.0581360e-001;
		static final double B_T3_c = +4.1148877e-001;
		static final double B_T4_c = +6.3737487e-001;
		static final double B_T5_c = +5.9674345e-001;
		static final double B_Car_always = +1.8670932e+000;


		/**
		 * Computes for this person the utility of choosing car as transportation mode
		 * when the tour (plan) has work as main purpose
		 * @param p
		 * @return
		 */
		public double computeUtility(Person p) {
			double T_DIST = calcDist(p);
			double DRIV_LIC = getHasLicense (p);
			double CAR = getCarAlternativeAvail (p);
			double CAR_AV = getCarAvailPerson (p);
			/*int T2 = ;
			int T3 = ;
			int T4 = ;
			int T5 = ;*/

			 if (CAR == 1){
				 return B_Const_c * 1 + B_Dist_c * T_DIST + B_Lic_c * DRIV_LIC + B_Car_always * CAR_AV; // + B_T2_c * T2 + B_T3_c * T3 + B_T4_c * T4 + B_T5_c * T5;
			 }
			 return -10000;
		}
	}

	class PublicToWork implements UtilityComputer2 {
		static final double B_Dist_pt = +2.2111449e-002;
		static final double B_Season_pt = +2.4361779e+000;
		static final double B_Age_sq = -1.0945597e-004;
		static final double B_T2_pt = -3.3640439e-001;
		static final double B_T3_pt = -7.5685136e-002;
		static final double B_T4_pt = -2.8762912e-001;
		static final double B_T5_pt = -9.6655080e-001;
		static final double B_pt_car_never = +5.3410438e-001;


		/**
		 * Computes for this person the utility of choosing public transport as transportation mode
		 * when the tour (plan) has work as main purpose
		 * @param p
		 * @return
		 */
		public double computeUtility(Person p) {
			double SEASON_T = getTravelcards(p);
			double T_DIST = calcDist(p);
			double r4 = MatsimRandom.getRandom().nextDouble();

			if ( r4 < 0.92) {
				return B_Dist_pt * T_DIST + B_Season_pt * SEASON_T ;
			}
			return -10000;
		}
	}

	class OtherToWork implements UtilityComputer2 {

		static final double B_Const_ot = +1.4143329e-001;

		/**
		 * Computes for this person the utility of choosing a transportation mode different
		 * than the previous four (walk, bicycle, car, public transport)
		 * when the tour (plan) has work as main purpose
		 * @param p
		 * @return
		 */
		public double computeUtility(Person p) {
			return B_Const_ot * 1;
		}
	}

	class WalkToEducation implements UtilityComputer2 {
		static final double B_Dist_w = -3.4622068e-002;
		static final double B_Const_w = +1.1371760e+000;

		/**
		 * Computes for this person the utility of choosing walk as transportation mode
		 * when the tour (plan) has education as main purpose
		 * @param p
		 * @return
		 */
		public double computeUtility(Person p) {
			double T_DIST = calcDist(p);

			return B_Const_w * 1 + B_Dist_w * T_DIST;
		}
	}
	class BikeToEducation implements UtilityComputer2 {
		static final double B_Dist_b = -8.2977921e-004;
		static final double B_Const_b = -1.1032706e-001;

		/**
		 * Computes for this person the utility of choosing bicycle as transportation mode
		 * when the tour (plan) has education as main purpose
		 * @param p
		 * @return
		 */
		public double computeUtility(Person p) {
			double T_DIST = calcDist(p);
			double r3 = MatsimRandom.getRandom().nextDouble();

			if (r3 <0.44){
				return B_Const_b * 1 + B_Dist_b * T_DIST;
			 }
			 return -10000;
		}
	}

	class CarToEducation implements UtilityComputer2 {
		static final double B_Dist_c = +1.6640945e-002;
		static final double B_Const_c = -9.3205115e-001;
		static final double B_Lic_c = +8.0324356e-001;
		static final double B_T2_c = -7.2619376e-001;
		static final double B_T3_c = -1.1043757e-000;
		static final double B_T4_c = +1.0049912e-001;
		static final double B_T5_c = +2.2958927e-002;
		static final double B_Car_always = +2.2057535e+000;

		/**
		 * Computes for this person the utility of choosing car as transportation mode
		 * when the tour (plan) has education as main purpose
		 * @param p
		 * @return
		 */
		public double computeUtility(Person p) {
			double T_DIST = calcDist(p);
			double DRIV_LIC = getHasLicense (p);
			double CAR = getCarAlternativeAvail (p);
			double CAR_AV = getCarAvailPerson (p);
			/*int T2 = ;
			int T3 = ;
			int T4 = ;
			int T5 = ;*/

			 if (CAR == 1){
				 return B_Const_c * 1 + B_Dist_c * T_DIST + B_Lic_c * DRIV_LIC + B_Car_always * CAR_AV; // + B_T2_c * T2 + B_T3_c * T3 + B_T4_c * T4 + B_T5_c * T5;
			 }
			 return -10000;
		}
	}

	class PublicToEducation implements UtilityComputer2 {
		static final double B_Dist_pt = +1.8457565e-002;
		static final double B_Season_pt = +1.6100989e+000;
		static final double B_Age_sq = +2.2835746e-004;
		static final double B_T2_pt = -4.4615663e-001;
		static final double B_T3_pt = -4.9524897e-001;
		static final double B_T4_pt = -1.2490401e-001;
		static final double B_T5_pt = -2.5164752e-001;
		static final double B_pt_car_never = -5.3451279e-001;

		/**
		 * Computes for this person the utility of choosing public transport as transportation mode
		 * when the tour (plan) has education as main purpose
		 * @param p
		 * @return
		 */
		public double computeUtility(Person p) {
			double T_DIST = calcDist(p);
			double SEASON_T = getTravelcards(p);
			double AGE_SQ = getAge2 (p);
			double r4 = MatsimRandom.getRandom().nextDouble();

			if ( r4 < 0.92) {
				return B_Dist_pt * T_DIST + B_Season_pt * SEASON_T + B_Age_sq * AGE_SQ;
			}
			return -10000;
		}
	}

	class OtherToEducation implements UtilityComputer2 {

		static final double B_Const_ot = -1.4406206e-000;

		/**
		 * Computes for this person the utility of choosing a transportation mode different
		 * than the previous four (walk, bicycle, car, public transport)
		 * when the tour (plan) has education as main purpose
		 * @param p
		 * @return
		 */
		public double computeUtility(Person p) {

			return B_Const_ot * 1;
		}
	}

	class WalkToShop implements UtilityComputer2 {
		static final double B_Dist_w = -4.1026300e-001;
		static final double B_Const_w = +3.4582248e+000;

		/**
		 * Computes for this person the utility of choosing walk as transportation mode
		 * when the tour (plan) has shop or leisure as main purpose
		 * @param p
		 * @return
		 */
		public double computeUtility(Person p) {
			double T_DIST = calcDist(p);

			return B_Const_w * 1 + B_Dist_w * T_DIST;
		}
	}

	class BikeToShop implements UtilityComputer2 {
		static final double B_Dist_b = -1.22347514e-001;
		static final double B_Const_b = +1.1708104e+000;

		/**
		 * Computes for this person the utility of choosing bicycle as transportation mode
		 * when the tour (plan) has shop or leisure as main purpose
		 * @param p
		 * @return
		 */
		public double computeUtility(Person p) {
			double T_DIST = calcDist(p);
			double r4 = MatsimRandom.getRandom().nextDouble();

			if (r4 <0.44){
				return B_Const_b * 1 + B_Dist_b * T_DIST;
			}
			return -10000;
		}
	}

	class CarToShop implements UtilityComputer2 {
		static final double B_Dist_c = +1.7435190e-002;
		static final double B_Const_c = +7.7357251e-001;
		static final double B_Lic_c = +3.0736545e-001;
		static final double B_T2_c = +3.2010590e-001;
		static final double B_T3_c = +3.0765084e-001;
		static final double B_T4_c = +4.0390676e-001;
		static final double B_T5_c = +3.6820671e-001;
		static final double B_Car_always = +1.0021152e+000;
		static final double B_HH_Dim = +1.1019932e-001;


		/**
		 * Computes for this person the utility of choosing car as transportation mode
		 * when the tour (plan) has shop or leisure as main purpose
		 * @param p
		 * @return
		 */
		public double computeUtility(Person p) {
			double T_DIST = calcDist(p);
			double DRIV_LIC = getHasLicense (p);
			double CAR = getCarAlternativeAvail (p);
			double CAR_AV = getCarAvailPerson (p);
			/*int T2 = ;
			int T3 = ;
			int T4 = ;
			int T5 = ;
			int HH_Dim = ;*/

			 if (CAR == 1){
				 return B_Const_c * 1 + B_Dist_c * T_DIST + B_Lic_c * DRIV_LIC + B_Car_always * CAR_AV; // + B_T2_c * T2 + B_T3_c * T3 + B_T4_c * T4 + B_T5_c * T5 + B_HH_Dim * HH_Dim;
			 }
			 return -10000;
		}
	}

	class PublicToShop implements UtilityComputer2 {
		static final double B_Age_sq = +5.5715851e-005;
		static final double B_Season_pt = +1.3486953e+000;
		static final double B_Dist_pt = +1.8348214e-002;
		static final double B_T2_pt = -7.6207183e-001;
		static final double B_T3_pt = -6.4683092e-001;
		static final double B_T4_pt = -9.4348474e-001;
		static final double B_T5_pt = -1.5534112e+000;
		static final double B_pt_car_never = +8.0390793e-001;

		/**
		 * Computes for this person the utility of choosing public transport as transportation mode
		 * when the tour (plan) has shop or leisure as main purpose
		 * @param p
		 * @return
		 */
		public double computeUtility(Person p) {
			double T_DIST = calcDist(p);
			double SEASON_T = getTravelcards(p);
			double AGE_SQ = getAge2 (p);
			double r4 = MatsimRandom.getRandom().nextDouble();
			/*int T2 = ;
			int T3 = ;
			int T4 = ;
			int T5 = ;*/
			double CAR_NEVER = Math.pow ((getCarAvailPerson (p)-1),2);

			if ( r4 < 0.92) {
				return B_Dist_pt * T_DIST + B_Season_pt * SEASON_T + B_Age_sq * AGE_SQ + B_pt_car_never * CAR_NEVER; //B_T2_pt * T2 + B_T3_pt * T3 + B_T4_pt * T4 + B_T5_pt * T5;
			}
			return -10000;
		}
	}





	class OtherToShop implements UtilityComputer2 {
		static final double B_Const_ot = -7.6196412e-001;

		/**
		 * Computes for this person the utility of choosing a transportation mode different
		 * than the previous four (walk, bicycle, car, public transport)
		 * when the tour (plan) has shop or leisure as main purpose
		 * @param p
		 * @return
		 */
		public double computeUtility(Person p) {

			return B_Const_ot * 1 ;
		}
	}


}

