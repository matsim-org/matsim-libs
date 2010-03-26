/* *********************************************************************** *
 * project: org.matsim.*
 * JohScoringFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C)2008 by the members listed in the COPYING,  *
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

package playground.mfeil;

import java.util.TreeMap;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRoute;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.core.population.PersonImpl;

import playground.mfeil.attributes.AgentsAttributesAdder;

/**
 * New scoring function following Joh's dissertation:
 * <blockquote>
 *  <p>Joh, C.-H. (2004) <br>
 *  Measuring and Predicting Adaptation in Multidimensional Activity-Travel Patterns,<br>
 *  Bouwstenen 79, Eindhoven University Press, Eindhoven.</p>
 * </blockquote>
 *
 *
 * @author mfeil
 */

public class JohScoringFunctionEstimation implements ScoringFunction {

	protected final Person person;
	protected final Plan plan;
	protected final Id id;

	protected double score;
	private double lastTime;
	private int index; // the current position in plan.actslegs
	private double firstActTime;
	private final int lastActIndex;

	private static final double INITIAL_LAST_TIME = 0.0;
	private static final int INITIAL_INDEX = 0;
	private static final double INITIAL_FIRST_ACT_TIME = Time.UNDEFINED_TIME;
	private static final double INITIAL_SCORE = 0.0;
	
	private final String seasonTicket;
	private final double income_averageIncome_ratio;
	private final int female;
	private final int age;
	private final int license;

	/* TODO [MR] the following field should not be public, but I need a way to reset the initialized state
	 * for the test cases.  Once we have the better config-objects, where we do not need to parse the
	 * values each time from a string, this whole init() concept can be removed and with this
	 * also this public member.  -marcel, 07aug07
	 */
	public static boolean initialized = false;

	/** True if one at least one of marginal utilities for performing, waiting, being late or leaving early is not equal to 0. */
	private static boolean scoreActs = true;

	private static final Logger log = Logger.getLogger(JohScoringFunctionEstimation.class);
	private static boolean parametersLogged = false;
	
	private static final TreeMap<String, JohActUtilityParametersExtended> utilParams = new TreeMap<String, JohActUtilityParametersExtended>();
	private static double factorOfLateArrival = 3; 
	private static double marginalUtilityOfEarlyDeparture = 0; 
	
	// Settings of 82
	private static double beta_time_car = -6; 
	private static double beta_time_pt = -1.0; 
	private static double beta_time_bike = -1.07;
	private static double beta_time_walk = -2.5; 
	
	private static double constantPt = -0.578;
	private static double constantBike = -0.4;
	private static double constantWalk = 0.4;
	
	private static double beta_cost_car = 1; 
	private static double beta_cost_pt = -0.117;
	
	private static double lambda_cost_income_car = 0.185;
	private static double lambda_cost_income_pt = -0.27;
	
	private static double beta_female_act = -0.0577;
	private static double beta_female_travel = 0.0797;
	
	private static double travelCostCar = 0.5;	// CHF/km
	private static double travelCostPt_None = 0.28;	// CHF/km
	private static double travelCostPt_Halbtax = 0.15;	// CHF/km
	private static double travelCostPt_GA = 0.08;	// CHF/km;
	
	private static double licenseCar = -0.25;	
	private static double licensePt = 0.0;	
	private static double licenseBike = 0.0;	
	private static double licenseWalk = 0.0;	
	
	private static double repeat = -0.5;
	
	private static final double uMin_home = 0;
	private static final double uMin_innerHome = 0;
	private static final double uMin_work = 0;
	private static final double uMin_education = 0;
	private static final double uMin_shopping = 0;
	private static final double uMin_leisure = 0;
	
	private static final double uMax_home = 5.41; 
	private static final double uMax_innerHome = 1.0; 
	private static final double uMax_work= 6.0;  
	private static final double uMax_education = 4.0;
	private static final double uMax_shopping = 0.2; 
	private static final double uMax_leisure = 2.34;  
	
	private static final double alpha_home = 12;
	private static final double alpha_innerHome = 2.0;
	private static final double alpha_work = 8;
	private static final double alpha_education = 6;
	private static final double alpha_shopping = 0.7;
	private static final double alpha_leisure = 2;
	
	private static final double beta_home = 0.429;
	private static final double beta_innerHome = 17.8;
	private static final double beta_work = 0.568;
	private static final double beta_education = 2.5;
	private static final double beta_shopping = 5;
	private static final double beta_leisure = 5;
	
	private static final double gamma_home = 1;
	private static final double gamma_innerHome = 1;
	private static final double gamma_work = 1;
	private static final double gamma_education = 1;
	private static final double gamma_shopping = 1;
	private static final double gamma_leisure = 1;
	
	private static final double beta_age_home = 0;
	private static final double beta_age_innerHome = 0; 
	private static final double beta_age_work = -0.00609;
	private static final double beta_age_education = -0.0153;
	private static final double beta_age_shopping = 0;
	private static final double beta_age_leisure = 0;
	
	private final Network network;
	
	
		
	public JohScoringFunctionEstimation(final Plan plan, final Network network) {
		this.network = network;
		
		init();
		this.reset();
		
		// check seasonTicket
		PersonImpl person = (PersonImpl) plan.getPerson();
		if (person.getTravelcards()!=null){
			if (person.getTravelcards().contains("ch-GA")) this.seasonTicket = "ch-GA";
			else if (person.getTravelcards().contains("ch-HT")) this.seasonTicket = "ch-HT";
			else {
				this.seasonTicket = "none";
				log.warn("Unknown travel card type "+person.getTravelcards().first()+" for person "+person.getId()+". " +
					"Using travel cost as if the person had no travel card.");
			}
		}
		else this.seasonTicket = "none";
		
		// check income and divide by average income
		if (person.getCustomAttributes()!=null && person.getCustomAttributes().containsKey("income")) {
			double income =Double.parseDouble(person.getCustomAttributes().get("income").toString());
			this.income_averageIncome_ratio = income / AgentsAttributesAdder.AVERAGE_INCOME;
		}
		else this.income_averageIncome_ratio = 1; // assign average income otherwise
		
		// check gender
		if (person.getSex()!=null){
			if(person.getSex().equals("m") || person.getSex().equals("male")){
				this.female = 0;
			}
			else if (person.getSex().equals("f") || person.getSex().equals("female")){
				this.female = 1;
			}
			else {
				log.warn("Unknown gender "+person.getAge()+" for person "+person.getId()+". " +
				"Setting gender to default \"male\".");
				this.female = 0;
			}
		}
		else this.female = 0; 
		
		// check age
		this.age=person.getAge();
		
		// check license
		if (person.getLicense()!=null){
			if (person.getLicense().equals("yes")) this.license=1;
			else if (person.getLicense().equals("no")) this.license=0;
			else {
				log.warn("Unknown license "+person.getLicense()+" for person "+person.getId()+". " +
				"Setting license to default \"no\".");
				this.license=0;
			}
		}
		else {
		//	log.warn("No license information for person "+person.getId()+". " +
		//	"Setting license to default \"no\".");
			this.license=0;
		}
		
		
		this.plan = plan;
		this.person = this.plan.getPerson();
		this.lastActIndex = this.plan.getPlanElements().size() - 1;
		this.id = plan.getPerson().getId();
		
		if (!parametersLogged) {
			this.writeParametersToLog();
			parametersLogged = true;
		}
	}

	public void reset() {
		this.lastTime = INITIAL_LAST_TIME;
		this.index = INITIAL_INDEX;
		this.firstActTime = INITIAL_FIRST_ACT_TIME;
		this.score = INITIAL_SCORE;

	}
	
	
	// the activity is currently handled by startLeg()
	public void startActivity(final double time, final Activity act) {
	}

	public void endActivity(final double time) {
	}
	
	
	public void startLeg(final double time, final Leg leg) {
		
		if (this.index % 2 == 0) {
			handleAct(time);
		}
		this.lastTime = time;
	}
	
	public void addMoney(final double amount){
		this.score+=amount; //linear mapping of money to utility
	}

	public void endLeg(final double time) {
		handleLeg(time);
		this.lastTime = time;
	}

	public void agentStuck(final double time) {
		this.lastTime = time;
	}

	public void finish() {
		if (this.index == this.lastActIndex) {
			handleAct(24*3600); // handle the last act
		}
	}

	public double getScore() {
		return this.score;
	}

	private static void init() {
		if (initialized) return;
		utilParams.clear();
		readUtilityValues();
		scoreActs = ((factorOfLateArrival != 0) || (marginalUtilityOfEarlyDeparture != 0));
		initialized = true;
	}

	private final double calcActScore(final double arrivalTime, final double departureTime, final ActivityImpl act) {
		
		JohActUtilityParametersExtended params = null;
		if (!act.getType().equals("home")) params = utilParams.get(act.getType());
		else {
			if (this.index==0 || this.index==this.lastActIndex) params = utilParams.get("home");
			else if (this.index==2 || (this.lastActIndex>=5 && this.index==this.lastActIndex-2)) {
				params = utilParams.get("innerHome");
				log.warn("In person's "+this.person.getId()+" plans, an home act is at 2nd or 2nd last position. This must not happen!");
			}
			else params = utilParams.get("innerHome");
		}
		if (params == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters.");
		}

		double tmpScore = 0.0;

		/* Calculate the times the agent actually performs the
		 * activity.  The facility must be open for the agent to
		 * perform the activity.  If it's closed, but the agent is
		 * there, the agent must wait instead of performing the
		 * activity (until it opens).
		 *
		 *                                             Interval during which
		 * Relationship between times:                 activity is performed:
		 *
		 *      O________C A~~D  ( 0 <= C <= A <= D )   D...D (not performed)
		 * A~~D O________C       ( A <= D <= O <= C )   D...D (not performed)
		 *      O__A+++++C~~D    ( O <= A <= C <= D )   A...C
		 *      O__A++D__C       ( O <= A <= D <= C )   A...D
		 *   A~~O++++++++C~~D    ( A <= O <= C <= D )   O...C
		 *   A~~O+++++D__C       ( A <= O <= D <= C )   O...D
		 *
		 * Legend:
		 *  A = arrivalTime    (when agent gets to the facility)
		 *  D = departureTime  (when agent leaves the facility)
		 *  O = openingTime    (when facility opens)
		 *  C = closingTime    (when facility closes)
		 *  + = agent performs activity
		 *  ~ = agent waits (agent at facility, but not performing activity)
		 *  _ = facility open, but agent not there
		 *
		 * assume O <= C
		 * assume A <= D
		 */

		double[] openingInterval = this.getOpeningInterval(act);
		double openingTime = openingInterval[0];
		double closingTime = openingInterval[1];

		double activityStart = arrivalTime;
		double activityEnd = departureTime;
		
		if ((openingTime >=  0) && (arrivalTime < openingTime)) {
			activityStart = openingTime;
		}
		if ((closingTime >= 0) && (closingTime < departureTime)) {
			activityEnd = closingTime;
		}
		if ((openingTime >= 0) && (closingTime >= 0)
				&& ((openingTime > departureTime) || (closingTime < arrivalTime))) {
			// agent could not perform action
			activityStart = departureTime;
			activityEnd = departureTime;
		}
		double duration = activityEnd - activityStart;

		// disutility if too late: multiplicate utility of activity duration with penalty factor
		double latestStartTime = params.getLatestStartTime();
		if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
			log.warn("In late loop - this must not happen! (Person "+plan.getPerson().getId()+" at act position "+this.index);
			// Maximum function since, in theory, utility may be negative and thus increase overall utility
			tmpScore -= Math.max(0, factorOfLateArrival * (1 + beta_female_act * this.female + params.getBetaAge() * this.age) * (params.getUMin() + (params.getUMax()-params.getUMin())/(java.lang.Math.pow(1+params.getGamma()*java.lang.Math.exp(params.getBeta()*(params.getAlpha()-((activityStart - latestStartTime)/3600))),1/params.getGamma()))));
		}

		// utility of performing an action
		if (duration>=0) {
			int gamma = 0;
			if (this.index!=0 && this.index!=this.lastActIndex && ((ActivityImpl)(this.plan.getPlanElements().get(this.index))).getType().startsWith(((ActivityImpl)(this.plan.getPlanElements().get(this.index-2))).getType().substring(0, 1))) gamma = 1;
			double interScore = (1 + beta_female_act * this.female + params.getBetaAge() * this.age + repeat * gamma) * (params.getUMin() + (params.getUMax()-params.getUMin())/(java.lang.Math.pow(1+params.getGamma()*java.lang.Math.exp(params.getBeta()*(params.getAlpha()-(duration/3600))),1/params.getGamma())));
			tmpScore += interScore;
		} else {
	/*		int gamma = 0;
			if (this.index!=0 && this.index!=this.lastActIndex && ((ActivityImpl)(this.plan.getPlanElements().get(this.index))).getType().startsWith(((ActivityImpl)(this.plan.getPlanElements().get(this.index-2))).getType().substring(0, 1))) gamma = 1;
			double interScore = Math.max(0, factorOfLateArrival * (1 + beta_female_act * this.female + params.getBetaAge() * this.age + repeat * gamma) * (params.getUMin() + (params.getUMax()-params.getUMin())/(java.lang.Math.pow(1+params.getGamma()*java.lang.Math.exp(params.getBeta()*(params.getAlpha()-(Math.abs(duration)/3600))),1/params.getGamma()))));
			tmpScore -= interScore;
			log.warn("In duration<0 loop - this must not happen! (Person "+plan.getPerson().getId()+" at act position "+this.index+" with duration "+duration+" and utility "+interScore);
	*/
			tmpScore -= 100;
		}

		// disutility if stopping too early
		double earliestEndTime = params.getEarliestEndTime();
		if ((earliestEndTime >= 0) && (activityEnd < earliestEndTime)) {
			log.warn("In early loop - this must not happen! (Person "+plan.getPerson().getId()+" at act position "+this.index);
			tmpScore += marginalUtilityOfEarlyDeparture / 3600 * (earliestEndTime - activityEnd);
		}
		return tmpScore;
	}

	protected double[] getOpeningInterval(final ActivityImpl act) {

		JohActUtilityParametersExtended params = utilParams.get(act.getType());
		if (params == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters.");
		}

		double openingTime = params.getOpeningTime();
		double closingTime = params.getClosingTime();

		// openInterval has two values
		// openInterval[0] will be the opening time
		// openInterval[1] will be the closing time
		double[] openInterval = new double[]{openingTime, closingTime};

		return openInterval;
	}

	protected double calcLegScore(final double departureTime, final double arrivalTime, final LegImpl leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in seconds
		double dist;
		if (!leg.getMode().toString().equals(TransportMode.car.toString())) dist = leg.getRoute().getDistance()/1000; // distance in kilometers
		else dist = RouteUtils.calcDistance((NetworkRoute) leg.getRoute(), this.network)/1000;
		if ((dist+"").equalsIgnoreCase("nan")) log.warn("dist von person "+plan.getPerson().getId()+" mit mode "+leg.getMode()+" und route "+leg.getRoute().getDistance()+", "+leg.getRoute().getStartLinkId()+", "+leg.getRoute().getEndLinkId()+" ist NaN.");
		
		
		if (TransportMode.car.equals(leg.getMode())) {
			// income_averageIncome_ratio needs to be non-zero if lambda_cost_income_car is negative, potential division by 0 otherwise
			double incomeRatio;
			if (this.income_averageIncome_ratio==0.0 && lambda_cost_income_car<0) incomeRatio = 0.001;
			else incomeRatio = this.income_averageIncome_ratio;
			tmpScore += (1 + beta_female_travel*this.female + licenseCar*this.license) * beta_time_car * travelTime/3600 + travelCostCar * beta_cost_car * dist/1000 * Math.pow(incomeRatio, lambda_cost_income_car);
		} 
		else if (TransportMode.pt.equals(leg.getMode())) {
			// income_averageIncome_ratio needs to be non-zero if lambda_cost_income_car is negative, potential division by 0 otherwise
			double incomeRatio;
			if (this.income_averageIncome_ratio==0.0 && lambda_cost_income_pt<0) incomeRatio = 0.001;
			else incomeRatio = this.income_averageIncome_ratio;
			// cost dependent from season ticket ownership
			double cost = 0;
			if (this.seasonTicket.equals("ch-GA")) cost = travelCostPt_GA;
			else if (this.seasonTicket.equals("ch-HT")) cost = travelCostPt_Halbtax; 
			else cost = travelCostPt_None; 
			tmpScore += (1+beta_female_travel*this.female + licensePt*this.license) * beta_time_pt * travelTime/3600 + beta_cost_pt * cost * dist/1000 * Math.pow(incomeRatio, lambda_cost_income_pt) + constantPt;
		} 
		else if (TransportMode.walk.equals(leg.getMode())) {
			tmpScore += (1+licenseBike*this.license) * beta_time_walk * travelTime/3600 + constantWalk;
		} 
		else if (TransportMode.bike.equals(leg.getMode())) {
			tmpScore += (1+licenseWalk*this.license) * beta_time_bike * travelTime/3600 + constantBike;
		}
		else {
			// use the same values as for "car"
			tmpScore += (1+beta_female_travel*this.female) * beta_time_car * travelTime/3600 + travelCostCar * beta_cost_car * dist/1000;
		}
		return tmpScore;
	}
	

	/**
	 * reads all activity utility values from the config-file
	 */
	private static final void readUtilityValues() {
		
		/* TODO @MF To be replaced by config file*/
		String type;
		JohActUtilityParametersExtended actParams;
			
		type = "home";
		actParams = new JohActUtilityParametersExtended("home", uMin_home, uMax_home, alpha_home, beta_home, gamma_home, beta_age_home);
		utilParams.put(type, actParams);
		
		type = "innerHome";
		actParams = new JohActUtilityParametersExtended("innerHome", uMin_innerHome, uMax_innerHome, alpha_innerHome, beta_innerHome, gamma_innerHome, beta_age_innerHome);
		utilParams.put(type, actParams);
		
		type = "work";
		actParams = new JohActUtilityParametersExtended("work", uMin_work, uMax_work, alpha_work, beta_work, gamma_work, beta_age_work);
		actParams.setOpeningTime(8*3600);
		actParams.setClosingTime(18*3600);
	//	actParams.setLatestStartTime(10*3600);
	//	actParams.setEarliestEndTime(15*3600);
		utilParams.put(type, actParams);

		type = "shopping";
		actParams = new JohActUtilityParametersExtended("shopping", uMin_shopping, uMax_shopping, alpha_shopping, beta_shopping, gamma_shopping, beta_age_shopping);
		actParams.setOpeningTime(10*3600);
		actParams.setClosingTime(18*3600);
		utilParams.put(type, actParams);

		type = "leisure";
		actParams = new JohActUtilityParametersExtended("leisure", uMin_leisure, uMax_leisure, alpha_leisure, beta_leisure, gamma_leisure, beta_age_leisure);
		actParams.setOpeningTime(18*3600);
		actParams.setClosingTime(21*3600);			
		utilParams.put(type, actParams);
		
		
		//TODO @ mfeil: bad programming style, I know...
		type = "education_higher";
		actParams = new JohActUtilityParametersExtended("education_higher", uMin_education, uMax_education, alpha_education, beta_education, gamma_education, beta_age_education);
		actParams.setOpeningTime(8*3600);
		actParams.setClosingTime(16*3600);
	//	actParams.setLatestStartTime(9*3600);
	//	actParams.setEarliestEndTime(12*3600);
		utilParams.put(type, actParams);
		
		type = "education_kindergarten";
		actParams = new JohActUtilityParametersExtended("education_kindergarten", uMin_education, uMax_education, alpha_education, beta_education, gamma_education, beta_age_education);
		actParams.setOpeningTime(8*3600);
		actParams.setClosingTime(16*3600);
	//	actParams.setLatestStartTime(9*3600);
	//	actParams.setEarliestEndTime(12*3600);
		utilParams.put(type, actParams);
		
		type = "education_other";
		actParams = new JohActUtilityParametersExtended("education_other", uMin_education, uMax_education, alpha_education, beta_education, gamma_education, beta_age_education);
		actParams.setOpeningTime(8*3600);
		actParams.setClosingTime(16*3600);
	//	actParams.setLatestStartTime(9*3600);
	//	actParams.setEarliestEndTime(12*3600);
		utilParams.put(type, actParams);
		
		type = "education_primary";
		actParams = new JohActUtilityParametersExtended("education_primary", uMin_education, uMax_education, alpha_education, beta_education, gamma_education, beta_age_education);
		actParams.setOpeningTime(8*3600);
		actParams.setClosingTime(16*3600);
	//	actParams.setLatestStartTime(9*3600);
	//	actParams.setEarliestEndTime(12*3600);
		utilParams.put(type, actParams);
		
		type = "education_secondary";
		actParams = new JohActUtilityParametersExtended("education_secondary", uMin_education, uMax_education, alpha_education, beta_education, gamma_education, beta_age_education);
		actParams.setOpeningTime(8*3600);
		actParams.setClosingTime(16*3600);
	//	actParams.setLatestStartTime(9*3600);
	//	actParams.setEarliestEndTime(12*3600);
		utilParams.put(type, actParams);
		
		type = "shop";
		actParams = new JohActUtilityParametersExtended("shop", uMin_shopping, uMax_shopping, alpha_shopping, beta_shopping, gamma_shopping, beta_age_shopping);
		actParams.setOpeningTime(10*3600);
		actParams.setClosingTime(18*3600);
		utilParams.put(type, actParams);
		
		type = "work_sector2";
		actParams = new JohActUtilityParametersExtended("work_sector2", uMin_work, uMax_work, alpha_work, beta_work, gamma_work, beta_age_work);
		actParams.setOpeningTime(8*3600);
		actParams.setClosingTime(18*3600);
	//	actParams.setLatestStartTime(10*3600);
	//	actParams.setEarliestEndTime(15*3600);
		utilParams.put(type, actParams);
		
		type = "work_sector3";
		actParams = new JohActUtilityParametersExtended("work_sector3", uMin_work, uMax_work, alpha_work, beta_work, gamma_work, beta_age_work);
		actParams.setOpeningTime(8*3600);
		actParams.setClosingTime(18*3600);
	//	actParams.setLatestStartTime(10*3600);
	//	actParams.setEarliestEndTime(15*3600);
		utilParams.put(type, actParams);
		
		type = "tta";
		actParams = new JohActUtilityParametersExtended("tta", uMin_work, uMax_home, alpha_home, beta_home, gamma_home, beta_age_home);
		actParams.setOpeningTime(3*3600);
		actParams.setClosingTime(24*3600);
		utilParams.put(type, actParams);
		
		type = "w";
		actParams = new JohActUtilityParametersExtended("w", uMin_work, uMax_work, alpha_work, beta_work, gamma_work, beta_age_work);
		actParams.setOpeningTime(8*3600);
		actParams.setClosingTime(18*3600);
	//	actParams.setLatestStartTime(10*3600);
	//	actParams.setEarliestEndTime(15*3600);
		utilParams.put(type, actParams);
		
		type = "h";
		actParams = new JohActUtilityParametersExtended("h", uMin_home, uMax_home, alpha_home, beta_home, gamma_home, beta_age_home);
		utilParams.put(type, actParams);
		
		type = "s";
		actParams = new JohActUtilityParametersExtended("s", uMin_shopping, uMax_shopping, alpha_shopping, beta_shopping, gamma_shopping, beta_age_shopping);
		actParams.setOpeningTime(10*3600);
		actParams.setClosingTime(18*3600);
		utilParams.put(type, actParams);

		type = "l";
		actParams = new JohActUtilityParametersExtended("l", uMin_leisure, uMax_leisure, alpha_leisure, beta_leisure, gamma_leisure, beta_age_leisure);
		actParams.setOpeningTime(18*3600);
		actParams.setClosingTime(21*3600);			
		utilParams.put(type, actParams);
		
		type = "e";
		actParams = new JohActUtilityParametersExtended("e", uMin_education, uMax_education, alpha_education, beta_education, gamma_education, beta_age_education);
		actParams.setOpeningTime(8*3600);
		actParams.setClosingTime(16*3600);
	//	actParams.setLatestStartTime(9*3600);
	//	actParams.setEarliestEndTime(12*3600);
		utilParams.put(type, actParams);

		
	}

	private void handleAct(final double time) {
		ActivityImpl act = (ActivityImpl)this.plan.getPlanElements().get(this.index);

		if (this.index == 0) {
			this.firstActTime = time;
		} /*else*/ if (this.index == this.lastActIndex) {
			String lastActType = act.getType();
			if (lastActType.equals(((ActivityImpl) this.plan.getPlanElements().get(0)).getType())) {
				// the first Act and the last Act have the same type
				double sc = calcActScore(this.lastTime, this.firstActTime + 24*3600, act); // SCENARIO_DURATION
				this.score += sc;				
			} else {
				if (scoreActs) {
					log.warn("The first and the last activity do not have the same type. The correctness of the scoring function can thus not be guaranteed.");
					// score first activity
					ActivityImpl firstAct = (ActivityImpl)this.plan.getPlanElements().get(0);
					this.score += calcActScore(0.0, this.firstActTime, firstAct);
					// score last activity
					this.score += calcActScore(this.lastTime, 24*3600, act); // SCENARIO_DURATION
					
				}
			}
		} else if (this.index != 0){
			double sc = calcActScore(this.lastTime, time, act);
			this.score += sc;
		}
		this.index++;
	}

	private void handleLeg(final double time) {
		LegImpl leg = (LegImpl)this.plan.getPlanElements().get(this.index);
		double lg = calcLegScore(this.lastTime, time, leg);
		this.score += lg;
		this.index++;
	}
	
	public void writeParametersToLog(){
		log.info("");
		log.info("Scoring function parameter settings:");
		log.info("");
		log.info("Activities:");
		log.info("uMin_home = "+uMin_home);
		log.info("uMax_home = "+uMax_home);
		log.info("alpha_home = "+alpha_home);
		log.info("beta_home = "+beta_home);
		log.info("gamma_home = "+gamma_home);
		log.info("beta_age_home = "+beta_age_home);
		log.info("uMin_innerHome = "+uMin_innerHome);
		log.info("uMax_innerHome = "+uMax_innerHome);
		log.info("alpha_innerHome = "+alpha_innerHome);
		log.info("beta_innerHome = "+beta_innerHome);
		log.info("gamma_innerHome = "+gamma_innerHome);
		log.info("beta_age_innerHome = "+beta_age_innerHome);
		log.info("uMin_work = "+uMin_work);
		log.info("uMax_work = "+uMax_work);
		log.info("alpha_work = "+alpha_work);
		log.info("beta_work = "+beta_work);
		log.info("gamma_work = "+gamma_work);
		log.info("beta_age_work = "+beta_age_work);
		JohActUtilityParametersExtended params = utilParams.get("work_sector2");
		log.info("opening_work = "+Time.writeTime(params.getOpeningTime())+" to "+Time.writeTime(params.getClosingTime()));
		log.info("uMin_education = "+uMin_education);
		log.info("uMax_education = "+uMax_education);
		log.info("alpha_education = "+alpha_education);
		log.info("beta_education = "+beta_education);
		log.info("gamma_education = "+gamma_education);
		log.info("beta_age_education = "+beta_age_education);
		params = utilParams.get("education_higher");
		log.info("opening_education = "+Time.writeTime(params.getOpeningTime())+" to "+Time.writeTime(params.getClosingTime()));
		log.info("uMin_leisure = "+uMin_leisure);
		log.info("uMax_leisure = "+uMax_leisure);
		log.info("alpha_leisure = "+alpha_leisure);
		log.info("beta_leisure = "+beta_leisure);
		log.info("gamma_leisure = "+gamma_leisure);
		log.info("beta_age_leisure = "+beta_age_leisure);
		params = utilParams.get("leisure");
		log.info("opening_leisure = "+Time.writeTime(params.getOpeningTime())+" to "+Time.writeTime(params.getClosingTime()));
		log.info("uMin_shopping = "+uMin_shopping);
		log.info("uMax_shopping = "+uMax_shopping);
		log.info("alpha_shopping = "+alpha_shopping);
		log.info("beta_shopping = "+beta_shopping);
		log.info("gamma_shopping = "+gamma_shopping);
		log.info("beta_age_shopping = "+beta_age_shopping);
		params = utilParams.get("shop");
		log.info("opening_shopping = "+Time.writeTime(params.getOpeningTime())+" to "+Time.writeTime(params.getClosingTime()));
		log.info("");
		log.info("Legs:");
		log.info("beta_time_car = "+beta_time_car);
		log.info("beta_cost_car = "+beta_cost_car); 
		log.info("lambda_cost_income_car = "+lambda_cost_income_car); 
		log.info("beta_time_pt = "+beta_time_pt);
		log.info("beta_cost_pt = "+beta_cost_pt);
		log.info("lambda_cost_income_pt = "+lambda_cost_income_pt); 
		log.info("constantPt = "+constantPt); 
		log.info("beta_time_bike = "+beta_time_bike);
		log.info("constantBike = "+constantBike); 
		log.info("beta_time_walk = "+beta_time_walk);
		log.info("constantWalk = "+constantWalk); 
		log.info("");
		log.info("Others:");
		log.info("beta_female_act = "+beta_female_act);
		log.info("beta_female_travel = "+beta_female_travel);
		log.info("beta_licenseCar = "+licenseCar);
		log.info("repeat = "+repeat);
	}

}
