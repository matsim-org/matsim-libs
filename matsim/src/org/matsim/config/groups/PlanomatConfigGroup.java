package org.matsim.config.groups;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.config.Module;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.ScoringFunctionFactory;

public class PlanomatConfigGroup extends Module {

	public static final String GROUP_NAME = "planomat";

	public static final String OPTIMIZATION_TOOLBOX = "optimizationToolbox";
	public static final String OPTIMIZATION_TOOLBOX_JGAP = "jgap";
	public static final String DEFAULT_OPTIMIZATION_TOOLBOX = PlanomatConfigGroup.OPTIMIZATION_TOOLBOX_JGAP;
	private String optimizationToolbox;

	public static final String SCORING_FUNCTION = "scoringFunction";
	public static final String CHARYPAR_NAGEL_SCORING_FUNCTION = "CharyparNagel";
	public static final String CHARYPAR_NAGEL_OPEN_TIMES_SCORING_FUNCTION = "CharyparNagelOpenTimes";
	public static final ScoringFunctionFactory DEFAULT_SCORING_FUNCTION = new CharyparNagelScoringFunctionFactory();
	private ScoringFunctionFactory scoringFunctionFactory;

	public static final String POPSIZE = "populationSize";
	public static final int DEFAULT_POPSIZE = 10; 
	private int popSize;

	public static final String JGAP_MAX_GENERATIONS = "jgapMaxGenerations";
	public static final int DEFAULT_JGAP_MAX_GENERATIONS = 100;
	private int jgapMaxGenerations;

	public static final String POSSIBLE_MODES = "possibleModes";
	public static final String POSSIBLE_MODES_CAR = BasicLeg.CARMODE;
	public static final String POSSIBLE_MODES_CAR_PT = BasicLeg.CARMODE + " " + BasicLeg.PTMODE;
	public static final String DEFAULT_POSSIBLE_MODES = PlanomatConfigGroup.POSSIBLE_MODES_CAR;
	private ArrayList<String> possibleModes;

	public static final String LINK_TRAVEL_TIME_ESTIMATOR = "linkTravelTimeEstimator";
	public static final String TRAVEL_TIME_CALCULATOR = "org.matsim.trafficmonitoring.TravelTimeCalculator";
	public static final String LINEAR_INTERPOLATING = "org.matsim.planomat.costestimators.LinearInterpolatingTTCalculator";
	public static final String DEFAULT_LINK_TRAVEL_TIME_ESTIMATOR = TRAVEL_TIME_CALCULATOR;
	private String linkTravelTimeEstimatorName;

	public static final String LEG_TRAVEL_TIME_ESTIMATOR = "legTravelTimeEstimator";
	public static final String CETIN_COMPATIBLE = "org.matsim.planomat.costestimators.CetinCompatibleLegTravelTimeEstimator";
	public static final String CHARYPAR_ET_AL_COMPATIBLE = "org.matsim.planomat.costestimators.CharyparEtAlCompatibleLegTravelTimeEstimator";
	public static final String DEFAULT_LEG_TRAVEL_TIME_ESTIMATOR = CETIN_COMPATIBLE;
	private String legTravelTimeEstimatorName;
	
	public static final String INDIFFERENCE = "indifference";
	public static final String BE_VERBOSE = "beVerbose";

	/**
	 * TODO [meisterk, Sep 2, 2008] keep the use of this parameter as a controler for efficiency of the algorithm
	 * e.g. when changing to integer coded times, use it to control size of time slices
	 */
	//private double indifference = -1.0;

	private final static Logger log = Logger.getLogger(PlanomatConfigGroup.class);

	public PlanomatConfigGroup() {
		super(PlanomatConfigGroup.GROUP_NAME);

		// set defaults
		this.optimizationToolbox = PlanomatConfigGroup.DEFAULT_OPTIMIZATION_TOOLBOX;
		this.scoringFunctionFactory = PlanomatConfigGroup.DEFAULT_SCORING_FUNCTION;
		this.popSize = PlanomatConfigGroup.DEFAULT_POPSIZE;
		this.jgapMaxGenerations = PlanomatConfigGroup.DEFAULT_JGAP_MAX_GENERATIONS;
		this.possibleModes = new ArrayList<String>();
		for (String possibleMode : PlanomatConfigGroup.DEFAULT_POSSIBLE_MODES.split(" ")) {
			this.possibleModes.add(possibleMode);
		}
		this.linkTravelTimeEstimatorName = PlanomatConfigGroup.DEFAULT_LINK_TRAVEL_TIME_ESTIMATOR;
		this.legTravelTimeEstimatorName = PlanomatConfigGroup.DEFAULT_LEG_TRAVEL_TIME_ESTIMATOR;
		
	}

	@Override
	public void addParam(final String param_name, final String value) {
		
		if (PlanomatConfigGroup.POSSIBLE_MODES.equals(param_name)) {
			if (
					PlanomatConfigGroup.POSSIBLE_MODES_CAR.equals(value) ||
					PlanomatConfigGroup.POSSIBLE_MODES_CAR_PT.equals(value)) {
				this.possibleModes = new ArrayList<String>();
				for (String possibleMode : value.split(" ")) {
					this.possibleModes.add(possibleMode);
				}
			}
		}
		
		// TODO add remaining config parameters to test
		
//		if (SCORING_FUNCTION.equals(param_name)) {
//
//			if (CHARYPAR_NAGEL_SCORING_FUNCTION.equals(value)) {
//				this.scoringFunctionFactory = new CharyparNagelScoringFunctionFactory();
//			} else if (CHARYPAR_NAGEL_OPEN_TIMES_SCORING_FUNCTION.equals(value)) {
//				this.scoringFunctionFactory = new CharyparNagelOpenTimesScoringFunctionFactory();
//			} else {
//				Gbl.errorMsg("Unknown scoring function identifier. Aborting...");
//			}
//
//		} else if (LEG_TRAVEL_TIME_ESTIMATOR.equals(param_name)) {
//			this.setLegTravelTimeEstimatorName(value);
////			} else if (LINK_TRAVEL_TIME_ESTIMATOR.equals(param_name)) {
////			this.setLinkTravelTimeEstimatorName(value);
//		} else if (JGAP_MAX_GENERATIONS.equals(param_name)) {
//			this.setJgapMaxGenerations(Integer.parseInt(value));
//		} else if (OPTIMIZATION_TOOLBOX.equals(param_name)) {
//			if (OPTIMIZATION_TOOLBOX_JGAP.equals(value)) {
//				this.setOptimizationToolbox(value);
//			} else {
//				log.error("Unknown optimization toolbox identifier. Aborting...");
//			}
//		} else if (POPSIZE.equals(param_name)) {
//			int popSize = 0;
//			try {
//				popSize = Integer.parseInt(value);
//			} catch (NumberFormatException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			if (popSize <= 0) {
//				Gbl.errorMsg("JGAP Population size must be a non-null positive integer.");
//			}
//			this.setPopSize(popSize);
//		} 
	}

	// getters/setters

	public String getLegTravelTimeEstimatorName() {
		return this.legTravelTimeEstimatorName;
	}

	public String getLinkTravelTimeEstimatorName() {
		return linkTravelTimeEstimatorName;
	}

	public int getJgapMaxGenerations() {
		return this.jgapMaxGenerations;
	}

	public void setJgapMaxGenerations(final int jgapMaxGenerations) {
		this.jgapMaxGenerations = jgapMaxGenerations;
	}

	public String getOptimizationToolbox() {
		return this.optimizationToolbox;
	}

	public void setOptimizationToolbox(final String optimizationToolbox) {
		this.optimizationToolbox = optimizationToolbox;
	}

	public int getPopSize() {
		return this.popSize;
	}

	public void setPopSize(final int popSize) {
		this.popSize = popSize;
	}

	public ScoringFunctionFactory getScoringFunctionFactory() {
		return this.scoringFunctionFactory;
	}

	public void setScoringFunctionFactory(
			final ScoringFunctionFactory scoringFunctionFactory) {
		this.scoringFunctionFactory = scoringFunctionFactory;
	}

	public ArrayList<String> getPossibleModes() {
		return possibleModes;
	}

	public void setPossibleModes(ArrayList<String> possibleModes) {
		this.possibleModes = possibleModes;
	}

	public void setLegTravelTimeEstimatorName(String legTravelTimeEstimatorName) {
		this.legTravelTimeEstimatorName = legTravelTimeEstimatorName;
	}

}
