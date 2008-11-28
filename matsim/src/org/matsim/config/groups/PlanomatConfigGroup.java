package org.matsim.config.groups;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.config.Module;
import org.matsim.network.NetworkLayer;
import org.matsim.planomat.costestimators.CetinCompatibleLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.CharyparEtAlCompatibleLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

public class PlanomatConfigGroup extends Module {

	public static final String GROUP_NAME = "planomat";

	public static final String OPTIMIZATION_TOOLBOX = "optimizationToolbox";
	public static final String OPTIMIZATION_TOOLBOX_JGAP = "jgap";
	public static final String DEFAULT_OPTIMIZATION_TOOLBOX = PlanomatConfigGroup.OPTIMIZATION_TOOLBOX_JGAP;
	private String optimizationToolbox;

	public static final String POPSIZE = "populationSize";
	public static final int DEFAULT_POPSIZE = 10; 
	private int popSize;

	public static final String JGAP_MAX_GENERATIONS = "jgapMaxGenerations";
	public static final int DEFAULT_JGAP_MAX_GENERATIONS = 100;
	private int jgapMaxGenerations;

	public static final String POSSIBLE_MODES = "possibleModes";
	public static final BasicLeg.Mode[] POSSIBLE_MODES_CAR = new BasicLeg.Mode[]{BasicLeg.Mode.car};
	public static final BasicLeg.Mode[] POSSIBLE_MODES_CAR_PT = new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.pt};
	public static final BasicLeg.Mode[] DEFAULT_POSSIBLE_MODES = PlanomatConfigGroup.POSSIBLE_MODES_CAR;
	private BasicLeg.Mode[] possibleModes;

	public static final String LEG_TRAVEL_TIME_ESTIMATOR_NAME = "legTravelTimeEstimator";
	public static final String CETIN_COMPATIBLE = "org.matsim.planomat.costestimators.CetinCompatibleLegTravelTimeEstimator";
	public static final String CHARYPAR_ET_AL_COMPATIBLE = "org.matsim.planomat.costestimators.CharyparEtAlCompatibleLegTravelTimeEstimator";
	public static final String MY_RECENT_EVENTS = "org.matsim.planomat.costestimators.MyRecentEventsBasedEstimator";
	public static final String DEFAULT_LEG_TRAVEL_TIME_ESTIMATOR_NAME = CETIN_COMPATIBLE;
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
		this.popSize = PlanomatConfigGroup.DEFAULT_POPSIZE;
		this.jgapMaxGenerations = PlanomatConfigGroup.DEFAULT_JGAP_MAX_GENERATIONS;
		this.possibleModes = PlanomatConfigGroup.DEFAULT_POSSIBLE_MODES;
		this.legTravelTimeEstimatorName = PlanomatConfigGroup.DEFAULT_LEG_TRAVEL_TIME_ESTIMATOR_NAME;

	}

	@Override
	public void addParam(final String param_name, final String value) {

		if (PlanomatConfigGroup.POSSIBLE_MODES.equals(param_name)) {
			String[] possibleModesStringArray = value.split(" ");
			this.possibleModes = new BasicLeg.Mode[possibleModesStringArray.length];
			for (int ii=0; ii < possibleModesStringArray.length; ii++) {
				this.possibleModes[ii] = BasicLeg.Mode.valueOf(possibleModesStringArray[ii]);
			}
		} else if (PlanomatConfigGroup.LEG_TRAVEL_TIME_ESTIMATOR_NAME.equals(param_name)) {
			this.legTravelTimeEstimatorName = value;
		}

		// TODO add remaining config parameters to test

//		} else if (LEG_TRAVEL_TIME_ESTIMATOR.equals(param_name)) {
//		this.setLegTravelTimeEstimatorName(value);
////		} else if (LINK_TRAVEL_TIME_ESTIMATOR.equals(param_name)) {
////		this.setLinkTravelTimeEstimatorName(value);
//		} else if (JGAP_MAX_GENERATIONS.equals(param_name)) {
//		this.setJgapMaxGenerations(Integer.parseInt(value));
//		} else if (OPTIMIZATION_TOOLBOX.equals(param_name)) {
//		if (OPTIMIZATION_TOOLBOX_JGAP.equals(value)) {
//		this.setOptimizationToolbox(value);
//		} else {
//		log.error("Unknown optimization toolbox identifier. Aborting...");
//		}
//		} else if (POPSIZE.equals(param_name)) {
//		int popSize = 0;
//		try {
//		popSize = Integer.parseInt(value);
//		} catch (NumberFormatException e) {
//		// TODO Auto-generated catch block
//		e.printStackTrace();
//		}
//		if (popSize <= 0) {
//		Gbl.errorMsg("JGAP Population size must be a non-null positive integer.");
//		}
//		this.setPopSize(popSize);
//		} 
	}

	// getters/setters

	public LegTravelTimeEstimator getLegTravelTimeEstimator(TravelTime travelTime, TravelCost travelCost, DepartureDelayAverageCalculator tDepDelayCalc, NetworkLayer network) {

		LegTravelTimeEstimator legTravelTimeEstimator = null;

		if (this.legTravelTimeEstimatorName.equalsIgnoreCase(PlanomatConfigGroup.CETIN_COMPATIBLE)) {
			legTravelTimeEstimator = new CetinCompatibleLegTravelTimeEstimator(travelTime, travelCost, tDepDelayCalc, network);
		} else if (this.legTravelTimeEstimatorName.equalsIgnoreCase(PlanomatConfigGroup.CHARYPAR_ET_AL_COMPATIBLE)) {
			legTravelTimeEstimator = new CharyparEtAlCompatibleLegTravelTimeEstimator(travelTime, travelCost, tDepDelayCalc, network);
		} else {
			throw new RuntimeException("legTravelTimeEstimator value: \"" + this.legTravelTimeEstimatorName + "\" is not allowed.");
		}

		return legTravelTimeEstimator;
	}

	public int getJgapMaxGenerations() {
		return this.jgapMaxGenerations;
	}

	public String getOptimizationToolbox() {
		return this.optimizationToolbox;
	}

	public int getPopSize() {
		return this.popSize;
	}

	public BasicLeg.Mode[] getPossibleModes() {
		return possibleModes;
	}

	public void setPossibleModes(BasicLeg.Mode[] possibleModes) {
		this.possibleModes = possibleModes;
	}

	public void setPopSize(int popSize) {
		this.popSize = popSize;
	}

	public String getLegTravelTimeEstimatorName() {
		return legTravelTimeEstimatorName;
	}

	public void setJgapMaxGenerations(int jgapMaxGenerations) {
		this.jgapMaxGenerations = jgapMaxGenerations;
	}

}
