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

	private static final long serialVersionUID = 1L;

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
	static final BasicLeg.Mode[] DEFAULT_POSSIBLE_MODES = new BasicLeg.Mode[]{};
	private BasicLeg.Mode[] possibleModes;

	public static final String LEVEL_OF_TIME_RESOLUTION = "levelOfTimeResolution";
	static final int DEFAULT_LEVEL_OF_TIME_RESOLUTION = 7;
	private int levelOfTimeResolution;

	public static final String LEG_TRAVEL_TIME_ESTIMATOR_NAME = "legTravelTimeEstimator";
	public static final String CETIN_COMPATIBLE = "org.matsim.planomat.costestimators.CetinCompatibleLegTravelTimeEstimator";
	public static final String CHARYPAR_ET_AL_COMPATIBLE = "org.matsim.planomat.costestimators.CharyparEtAlCompatibleLegTravelTimeEstimator";
	public static final String MY_RECENT_EVENTS = "org.matsim.planomat.costestimators.MyRecentEventsBasedEstimator";
	public static final String DEFAULT_LEG_TRAVEL_TIME_ESTIMATOR_NAME = CETIN_COMPATIBLE;
	private String legTravelTimeEstimatorName;

	public static final String DO_LOGGING = "doLogging";
	public static final boolean DEFAULT_DO_LOGGING = false;
	private boolean doLogging;

	private final static Logger logger = Logger.getLogger(PlanomatConfigGroup.class);

	public PlanomatConfigGroup() {
		super(PlanomatConfigGroup.GROUP_NAME);

		// set defaults
		this.optimizationToolbox = PlanomatConfigGroup.DEFAULT_OPTIMIZATION_TOOLBOX;
		this.popSize = PlanomatConfigGroup.DEFAULT_POPSIZE;
		this.jgapMaxGenerations = PlanomatConfigGroup.DEFAULT_JGAP_MAX_GENERATIONS;
		this.possibleModes = PlanomatConfigGroup.DEFAULT_POSSIBLE_MODES;
		this.legTravelTimeEstimatorName = PlanomatConfigGroup.DEFAULT_LEG_TRAVEL_TIME_ESTIMATOR_NAME;
		this.levelOfTimeResolution = PlanomatConfigGroup.DEFAULT_LEVEL_OF_TIME_RESOLUTION;
		this.doLogging = PlanomatConfigGroup.DEFAULT_DO_LOGGING;

	}

	@Override
	public void addParam(final String param_name, final String value) {

		if (PlanomatConfigGroup.POSSIBLE_MODES.equals(param_name)) {
			String[] possibleModesStringArray = value.split(",");
			this.possibleModes = new BasicLeg.Mode[possibleModesStringArray.length];
			for (int ii=0; ii < possibleModesStringArray.length; ii++) {
				this.possibleModes[ii] = BasicLeg.Mode.valueOf(possibleModesStringArray[ii]);
			}
		} else if (PlanomatConfigGroup.LEG_TRAVEL_TIME_ESTIMATOR_NAME.equals(param_name)) {
			this.legTravelTimeEstimatorName = value;
		} else if (PlanomatConfigGroup.JGAP_MAX_GENERATIONS.equals(param_name)) {
			this.setJgapMaxGenerations(Integer.parseInt(value));
		} else if (PlanomatConfigGroup.LEVEL_OF_TIME_RESOLUTION.equals(param_name)) {
			this.setLevelOfTimeResolution(Integer.parseInt(value));
		} else if (PlanomatConfigGroup.DO_LOGGING.equals(param_name)) {
			this.setDoLogging(Boolean.parseBoolean(value));
		} else {
			logger.warn("Unknown parameter name in module " + PlanomatConfigGroup.GROUP_NAME + ": \"" + param_name + "\". It is ignored.");
		}

		// TODO add remaining config parameters to test

//		} else if (LINK_TRAVEL_TIME_ESTIMATOR.equals(param_name)) {
//		this.setLinkTravelTimeEstimatorName(value);
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
		return possibleModes.clone();
	}

	public void setPossibleModes(BasicLeg.Mode[] possibleModes) {
		this.possibleModes = possibleModes.clone();
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

	public int getLevelOfTimeResolution() {
		return levelOfTimeResolution;
	}

	public void setLevelOfTimeResolution(int levelOfTimeResolution) {
		this.levelOfTimeResolution = levelOfTimeResolution;
	}

	public boolean isDoLogging() {
		return doLogging;
	}

	public void setDoLogging(boolean doLogging) {
		this.doLogging = doLogging;
	}

}
