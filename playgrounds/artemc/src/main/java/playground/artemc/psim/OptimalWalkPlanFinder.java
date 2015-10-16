package playground.artemc.psim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Created by artemc on 6/5/15.
 */
public class OptimalWalkPlanFinder {

	Config config;

	private static final Logger log = Logger.getLogger(OptimalWalkPlanFinder.class);

	public OptimalWalkPlanFinder(Config config) {
		this.config = config;
	}

	public static void main(String[] args){
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		OptimalWalkPlanFinder optimalWalkPlanFinder = new OptimalWalkPlanFinder(scenario.getConfig());
		Plan plan = scenario.getPopulation().getFactory().createPlan();
		optimalWalkPlanFinder.findOptimalWalkPlan(plan);

	}

	public Plan findOptimalWalkPlan(Plan plan) {

		Double beelineFactor = config.plansCalcRoute().getBeelineDistanceFactors().get("walk");
		Double speed =  config.plansCalcRoute().getTeleportedModeSpeeds().get("walk");

		Coord home = ((ActivityImpl) plan.getPlanElements().get(0)).getCoord();
		Coord work = ((ActivityImpl) plan.getPlanElements().get(2)).getCoord();

		Double distance = Math.sqrt((home.getX() - work.getX()) * (home.getX() - work.getX()) + (home.getY() - work.getY()) * (home.getY() - work.getY()));
		Double travelTime = distance * beelineFactor / speed;

		Double totalActivityTime = 24*3600 - 2 * travelTime;

		Double homeTyp = config.planCalcScore().getActivityParams("home").getTypicalDuration();
		Double workTyp = config.planCalcScore().getActivityParams("work").getTypicalDuration();

		Double betaAct = config.planCalcScore().getPerforming_utils_hr();
		Double betaLate = config.planCalcScore().getLateArrival_utils_hr();

		Double workDuration = workTyp;

		workDuration = (totalActivityTime / (homeTyp+workTyp)) * workTyp;

		//Double s = (totalActivityTime * betaLate - betaAct * workTyp) / ( 2 * betaLate);
		//workDuration = Math.sqrt(-(totalActivityTime * betaAct * workTyp) / betaLate  + s*s) - s;

		Double workOpeningTime = config.planCalcScore().getActivityParams("work").getOpeningTime();
		Double workClosingTime = config.planCalcScore().getActivityParams("work").getClosingTime();
		Double workEarliestEndTime = config.planCalcScore().getActivityParams("work").getEarliestEndTime();
		Double workLatestStartTime = config.planCalcScore().getActivityParams("work").getLatestStartTime();

		Double workStartTime = 0.0;

		if(workOpeningTime.isInfinite() || workClosingTime.isInfinite())
			log.error("No work opening times are defined!");

		if(workDuration>=(workEarliestEndTime-workOpeningTime)){
			workStartTime = workOpeningTime;
			if(workDuration>=(workClosingTime - workOpeningTime)){
				workDuration=workClosingTime - workOpeningTime;
			}
		}

		if(workDuration < (workEarliestEndTime-workOpeningTime)){
			workStartTime=workEarliestEndTime-workDuration;
			if(workDuration < (workEarliestEndTime-workLatestStartTime)){
				workStartTime = workLatestStartTime;
				workDuration = (workEarliestEndTime-workLatestStartTime);
			}
		}

		((ActivityImpl) plan.getPlanElements().get(0)).setEndTime(workStartTime - travelTime);

		((LegImpl) plan.getPlanElements().get(1)).setDepartureTime(((ActivityImpl) plan.getPlanElements().get(0)).getEndTime());

		((ActivityImpl) plan.getPlanElements().get(2)).setStartTime(workStartTime);
		((ActivityImpl) plan.getPlanElements().get(2)).setEndTime(workStartTime + workDuration);

		((LegImpl) plan.getPlanElements().get(3)).setDepartureTime(((ActivityImpl) plan.getPlanElements().get(2)).getEndTime());

		((ActivityImpl) plan.getPlanElements().get(4)).setStartTime(((LegImpl) plan.getPlanElements().get(3)).getDepartureTime() + travelTime);

		return plan;
	}

	public double getWalkTravelTime(Plan plan){
		Double beelineFactor = config.plansCalcRoute().getBeelineDistanceFactors().get("walk");
		Double speed =  config.plansCalcRoute().getTeleportedModeSpeeds().get("walk");

		Coord home = ((ActivityImpl) plan.getPlanElements().get(0)).getCoord();
		Coord work = ((ActivityImpl) plan.getPlanElements().get(2)).getCoord();

		Double distance = Math.sqrt((home.getX() - work.getX()) * (home.getX() - work.getX()) + (home.getY() - work.getY()) * (home.getY() - work.getY()));
		Double travelTime = distance * beelineFactor / speed;

		return travelTime;
	}
}
