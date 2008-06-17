package org.matsim.config.groups;

import org.apache.log4j.Logger;
import org.matsim.config.Module;
import org.matsim.gbl.Gbl;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.ScoringFunctionFactory;

import playground.meisterk.portland.scoring.PortlandScoringFunctionFactory;

public class PlanomatConfigGroup extends Module {

	public static final String GROUP_NAME = "planomat";
	
	public static final String SCORING_FUNCTION = "scoringFunction";
	public static final String LEG_TRAVEL_TIME_ESTIMATOR = "legTravelTimeEstimator";
//	public static final String LINK_TRAVEL_TIME_ESTIMATOR = "linkTravelTimeEstimator";
	public static final String JGAP_MAX_GENERATIONS = "jgapMaxGenerations";
	public static final String OPTIMIZATION_TOOLBOX = "optimizationToolbox";
	public static final String INDIFFERENCE = "indifference";
	public static final String POPSIZE = "populationSize";
	public static final String BE_VERBOSE = "beVerbose";

	public static final String CHARYPAR_NAGEL_SCORING_FUNCTION = "CharyparNagel";
	public static final String PORTLAND_SCORING_FUNCTION = "Portland";
	public static final String OPTIMIZATION_TOOLBOX_JGAP = "jgap";
	
	private ScoringFunctionFactory scoringFunctionFactory = null;
	private String legTravelTimeEstimatorName = null;
//	private String linkTravelTimeEstimatorName = null;
	private int jgapMaxGenerations = -1;
	private String optimizationToolbox = null;
	private double indifference = -1.0;
	private int popSize = -1;
	private boolean beVerbose = false;

	private final static Logger log = Logger.getLogger(PlanomatConfigGroup.class);

	public PlanomatConfigGroup() {
		super(PlanomatConfigGroup.GROUP_NAME);
	}

	@Override
	public void addParam(String param_name, String value) {
		if (SCORING_FUNCTION.equals(param_name)) {

			if (CHARYPAR_NAGEL_SCORING_FUNCTION.equals(value)) {
				this.scoringFunctionFactory = new CharyparNagelScoringFunctionFactory();
			} else if (PORTLAND_SCORING_FUNCTION.equals(value)) {
				this.scoringFunctionFactory = new PortlandScoringFunctionFactory();
			} else {
				Gbl.errorMsg("Unknown scoring function identifier. Aborting...");
			}
			
		} else if (LEG_TRAVEL_TIME_ESTIMATOR.equals(param_name)) {
			this.setLegTravelTimeEstimatorName(value);
//		} else if (LINK_TRAVEL_TIME_ESTIMATOR.equals(param_name)) {
//			this.setLinkTravelTimeEstimatorName(value);
		} else if (JGAP_MAX_GENERATIONS.equals(param_name)) {
			this.setJgapMaxGenerations(Integer.parseInt(value));
		} else if (OPTIMIZATION_TOOLBOX.equals(param_name)) {
			if (OPTIMIZATION_TOOLBOX_JGAP.equals(value)) {
				this.setOptimizationToolbox(value);
			} else {
				log.error("Unknown optimization toolbox identifier. Aborting...");
			}
		} else if (INDIFFERENCE.equals(param_name)) {
			this.setIndifference(Double.parseDouble(value));
		} else if (POPSIZE.equals(param_name)) {
			this.setPopSize(Integer.parseInt(value));
		} else if (BE_VERBOSE.equals(param_name)) {
			this.setBeVerbose(Boolean.parseBoolean(value));
		}
	}
	
	// getters/setters

	public String getLegTravelTimeEstimatorName() {
		return legTravelTimeEstimatorName;
	}

	public void setLegTravelTimeEstimatorName(String legTravelTimeEstimatorName) {
		this.legTravelTimeEstimatorName = legTravelTimeEstimatorName;
	}

//	public String getLinkTravelTimeEstimatorName() {
//		return linkTravelTimeEstimatorName;
//	}
//
//	public void setLinkTravelTimeEstimatorName(String linkTravelTimeEstimatorName) {
//		this.linkTravelTimeEstimatorName = linkTravelTimeEstimatorName;
//	}

	public int getJgapMaxGenerations() {
		return jgapMaxGenerations;
	}

	public void setJgapMaxGenerations(int jgapMaxGenerations) {
		this.jgapMaxGenerations = jgapMaxGenerations;
	}

	public String getOptimizationToolbox() {
		return optimizationToolbox;
	}

	public void setOptimizationToolbox(String optimizationToolbox) {
		this.optimizationToolbox = optimizationToolbox;
	}

	public double getIndifference() {
		return indifference;
	}

	public void setIndifference(double indifference) {
		this.indifference = indifference;
	}

	public int getPopSize() {
		return popSize;
	}

	public void setPopSize(int popSize) {
		this.popSize = popSize;
	}

	public boolean isBeVerbose() {
		return beVerbose;
	}

	public void setBeVerbose(boolean beVerbose) {
		this.beVerbose = beVerbose;
	}

	public ScoringFunctionFactory getScoringFunctionFactory() {
		return scoringFunctionFactory;
	}

	public void setScoringFunctionFactory(
			ScoringFunctionFactory scoringFunctionFactory) {
		this.scoringFunctionFactory = scoringFunctionFactory;
	}
	
}
