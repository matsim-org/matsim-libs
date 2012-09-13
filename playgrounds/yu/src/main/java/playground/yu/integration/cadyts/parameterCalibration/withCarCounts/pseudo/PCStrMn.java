///* *********************************************************************** *
// * project: org.matsim.*
// * StrMninclLT.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2008 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.pseudo;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.TreeMap;
//
//import org.ejml.data.DenseMatrix64F;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.api.core.v01.population.Plan;
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
//import org.matsim.core.gbl.Gbl;
//import org.matsim.core.replanning.PlanStrategy;
//
//import playground.yu.integration.cadyts.CalibrationConfig;
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.BseStrategyManager;
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.generalNormal.scoring.Events2Score4PC;
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.MultinomialLogitChoice;
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.parametersCorrection.BseParamCalibrationStrategyManager;
//import playground.yu.scoring.withAttrRecorder.Events2Score4AttrRecorder;
//import utilities.math.BasicStatistics;
//import utilities.math.MultinomialLogit;
//import cadyts.calibrators.Calibrator;
//
//public class PCStrMn extends BseParamCalibrationStrategyManager implements
//		BseStrategyManager {
//	// private final static Logger log = Logger.getLogger(StrMninclLT.class);
//	// private double delta;
//	private final Config config;
//	// private Plan oldSelected = null;
//	// private final MultinomialLogit singleMnl = null;
//
//	private BasicStatistics betaTravelingStats = null,
//			constantLeftTurnStats = null;
//	private final ParameterEstimator pe;
//	private DenseMatrix64F attrM = null, utilCorrV = null;
//
//	private Map<String/* paramName */, List<Double>> attrs = null;
//	List<Double> utilCorrs = null;
//	private int correctedPlanNb = 0;
//
//	final double minStdDev, varianceScale;
//	final int parameterUpdateInterval;
//	private PCCtlListener cltListener = null;
//
//	public PCStrMn(Network net, int firstIteration, Config config) {
//		super(firstIteration);
//		this.net = net;
//		this.config = config;
//
//		final String minStdDevStr = config.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME, "minFlowStddevVehH");
//		minStdDev = minStdDevStr != null ? Double.parseDouble(minStdDevStr)
//				: Calibrator.DEFAULT_MIN_FLOW_STDDEV_VEH_H;
//
//		final String varianceScaleStr = config.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME, "varianceScale");
//		varianceScale = varianceScaleStr != null ? Double
//				.parseDouble(varianceScaleStr)
//				: Calibrator.DEFAULT_VARIANCE_SCALE;
//
//		final String parameterUpdateIntervalStr = config.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME,
//				"parameterUpdateInterval");
//		parameterUpdateInterval = parameterUpdateIntervalStr != null ? Integer
//				.parseInt(parameterUpdateIntervalStr) : 1;
//
//		pe = new ParameterEstimator();
//	}
//
//	// @Override
//	// protected void afterRemovePlanHook(Plan plan) {
//	// super.afterRemovePlanHook(plan);
//	// }
//
//	@Override
//	protected void afterRunHook(Population population) {
//		super.afterRunHook(population);
//		// output stats and variabilities
//		statistics = new double[] { betaTravelingStats.getAvg(),
//				betaTravelingStats.getVar(), constantLeftTurnStats.getAvg(),
//				constantLeftTurnStats.getVar() };
//		System.out.println("BSE_Statistics\tavg.\t" + statistics[0]/*
//																	 * betaTravelingPtAttr
//																	 * .
//																	 */
//				+ "\tvar.\t" + statistics[1]/* constantLeftTurn */
//				+ "\tavg.\t" + statistics[2] + "\tvar.\t" + statistics[3]);
//	}
//
//	// @Override
//	// protected void afterStrategyRunHook(Person person, PlanStrategy strategy)
//	// {
//	// super.afterStrategyRunHook(person, strategy);
//	// if (strategy != null) {
//	// // if (iter - firstIter > getMaxPlansPerAgent()) {//
//	// // //////////////////////////
//	// // // ENSURE THAT EVERY PLAN IN CHOICE SET HAS BEEN SIMULATED
//	// // // ATLEAST ONE TIME
//	// if (strategy.getNumberOfStrategyModules() > 0) {
//	// /*
//	// * New plan has been created by e.g. ReRoute,
//	// * TimeAllocationMutator etc. Only the old score of last
//	// * selected Plan will set to the new created Plan.
//	// */
//	// Plan selectedPlan = person.getSelectedPlan();
//	// selectedPlan.setScore(oldSelected.getScore());
//	// oldSelected = null;
//	//
//	// /*
//	// * Vector p = new Vector(1/* (single-)choiceSetSize /); p.set(0,
//	// * 1d/* 100% /);
//	// *
//	// * Matrix d = new Matrix(1/* n-choiceSetSize /, paramDimension
//	// * // m-size of parameters that has to be calibrated ); for (int
//	// * i = 0; i < paramDimension; i++) { d.setColumn(i, new
//	// * Vector(0d)); }
//	// */
//	// // ******************************************************
//	//
//	// calibrator.selectPlan(0, getSinglePlanChoiceSet(selectedPlan),
//	// singleMnl);
//	// singleMnl = null;
//	//
//	// // **********************************************************
//	// } else {// Change-/SelectExpBeta
//	// // ********************************************************
//	// /* UPDATE PARAMETERS (OBSERVE THE PLAN CHOOSING IN MATSIM) */
//	// /* int selectedIdx= */calibrator.selectPlan(person.getPlans()
//	// .indexOf(person.getSelectedPlan())/* selectedIdx */,
//	// getPlanChoiceSet((PersonImpl) person),
//	// ((MultinomialLogitChoice) chooser)
//	// .getMultinomialLogit()/* MNL */);
//	// // ***************************************************
//	// }
//	// // }
//	// } else {// strategy==null
//	// Gbl.errorMsg("No strategy found!");
//	// }
//	// }
//
//	private DenseMatrix64F calculateDeltaParameter(int planNb) {
//		attrM = new DenseMatrix64F(planNb, PCCtlListener.paramDim);
//		utilCorrV = new DenseMatrix64F(planNb, 1);
//		// converts from Map to Matrix or Vector
//		for (int i = 0; i < PCCtlListener.paramNames.length; i++) {
//			List<Double> attrList = attrs.get(PCCtlListener.paramNames[i]);
//			for (int j = 0; j < attrList.size(); j++) {
//				attrM.set(j, i,/* CAUTION, the order of j and i */
//						attrList.get(j));
//			}
//		}
//		// attrs.clear();
//		pe.setAttrM(attrM);
//
//		for (int i = 0; i < utilCorrs.size(); i++) {
//			utilCorrV.set(i, 0, utilCorrs.get(i));
//		}
//		pe.setUtilCorrV(utilCorrV);
//
//		return pe.getDeltaParameters();
//	}
//
//	@Override
//	protected void beforePopulationRunHook(Population population) {
//		// the most things before "removePlans"
//		super.beforePopulationRunHook(population);// iter++
//		// cadyts class - create new BasicStatistics Objects
//		betaTravelingStats = new BasicStatistics();
//		constantLeftTurnStats = new BasicStatistics();
//
//		if (iter % parameterUpdateInterval == 0) {
//			correctedPlanNb = 0;
//			// (re)initializes attrs and utilCorrs
//			attrs = new TreeMap<String, List<Double>>();
//			for (String paramName : PCCtlListener.paramNames) {
//				attrs.put(paramName, new ArrayList<Double>());
//			}
//			utilCorrs = new ArrayList<Double>();
//
//			for (Person person : population.getPersons().values()) {
//				// now there could be #maxPlansPerAgent+?# Plans in choice set
//				// *********************UTILITY CORRECTION********************
//				// ***before removePlans and plan choice, correct utility***
//				generateScoreCorrections(person);
//			}
//			if (correctedPlanNb > 0) {
//				// calculate least squares and get delta parameter from
//				// ParameterEstimator and set new parameters in MNL
//				DenseMatrix64F estimatedParams = calculateDeltaParameter(correctedPlanNb);
//
//				//
//				MultinomialLogit mnl = ((MultinomialLogitChoice) chooser)
//						.getMultinomialLogit();
//				for (int i = 0; i < PCCtlListener.paramNames.length; i++) {
//					double deltaParam = estimatedParams.get(i, 0/* CAUTION! */);
//					int paramIdx = Events2Score4AttrRecorder.attrNameList
//							.indexOf(PCCtlListener.paramNames[i]);
//					double oldParamVal = mnl.getParameter(paramIdx);
//					double newParamVal = oldParamVal + deltaParam;
//					mnl.setParameter(paramIdx, newParamVal);
//					if (i == 0) {
//						betaTravelingStats.add(newParamVal);
//					} else if (i == 1) {
//						constantLeftTurnStats.add(newParamVal);
//					}
//
//					// set new parameters in config
//					PlanCalcScoreConfigGroup scoringCfg = config
//							.planCalcScore();
//					if (scoringCfg.getParams().containsKey(
//							PCCtlListener.paramNames[i])) {
//						scoringCfg.addParam(PCCtlListener.paramNames[i],
//								Double.toString(newParamVal));
//					} else {
//						config.setParam(
//								CalibrationConfig.BSE_CONFIG_MODULE_NAME,
//								PCCtlListener.paramNames[i],
//								Double.toString(newParamVal));
//					}
//				}
//			}
//		}
//		for (Person person : population.getPersons().values()) {
//			/* ***********************************************************
//			 * scoringCfg has been done, but they should be newly defined
//			 * because of new calibrated parameters and -- WITHOUT
//			 * utilityCorrections--
//			 * *******************************************************
//			 */
//			chooser.setPersonScore(person);
//		}
//
//		((Events2Score4PC) chooser).closeWriter();
//	}
//
//	@Override
//	protected void beforeStrategyRunHook(Person person, PlanStrategy strategy) {
//		// choose reset because of removeWorstPlan
//		resetChooser();
//		// ******************************************************
//		super.beforeStrategyRunHook(person, strategy);
//
//		if (strategy != null) {
//			// ENSURE THAT EVERY PLAN IN CHOICE SET HAS BEEN SIMULATED ATLEAST
//			// ONE TIME
//			// oldSelected = person.getSelectedPlan();
//
//			if (strategy.getNumberOfStrategyModules() <= 0) {
//				// only with planSelector/-Changer, without Plan innovation,
//				// i.e. no new plan will be created
//				// **************WRITE ATTR.S INTO MNL******************
//				chooser.setPersonAttrs(person, new BasicStatistics[] {
//						betaTravelingStats, constantLeftTurnStats });
//				// now there are only #maxPlansPerAgent# Plans in choice set
//			}
//			// else {// with planInnovation
//			// singleMnl = new MultinomialLogitCreator().createSingle(config);
//			// ((Events2Score4PC) chooser).setSinglePlanAttrs(oldSelected,
//			// singleMnl);
//			// }
//		} else { // strategy==null
//			Gbl.errorMsg("No strategy found!");
//		}
//	}
//
//	/**
//	 * @param plan
//	 * @return whether the score is corrected
//	 */
//	private boolean generateScoreCorrection(Plan plan) {
//
//		double scoreCorrection = cltListener.getUtilityCorrection(plan)
//				/ config.planCalcScore().getBrainExpBeta();
//		// #######SAVE "utilityCorrection" 4 MNL.ASC#########
//		plan.getCustomAttributes().put(UTILITY_CORRECTION, scoreCorrection);
//
//		return scoreCorrection != 0d;
//	}
//
//	/**
//	 * generates score corrections, and also prepares the Attr-Matrix and
//	 * UC-Vector
//	 *
//	 * @param person
//	 *            *
//	 */
//	private void generateScoreCorrections(Person person) {
//		for (Plan plan : person.getPlans()) {
//			// ------------------------------
//			generateScoreCorrection(plan);
//			// if (generateScoreCorrection(plan)) {
//
//			Map<String, Object> customAttrs = plan.getCustomAttributes();
//			/* set attr and uc values */
//			Map<String, Double> tmpNameVals = new TreeMap<String, Double>();
//			for (String paramName : PCCtlListener.paramNames) {
//				Object ob = customAttrs.get(paramName);
//				tmpNameVals.put(paramName,
//						ob == null ? 0d : ((Number) ob).doubleValue());
//			}
//
//			// boolean allZero = true;
//			// for (Double value : tmpNameVals.values()) {
//			// if (value != 0d) {
//			// allZero = false;
//			// break;
//			// }
//			// }
//			// if (allZero) {
//			// continue;// debugged
//			// }
//
//			double uc = (Double) customAttrs.get(UTILITY_CORRECTION);
//			if (uc != 0d) {
//				correctedPlanNb++;
//				utilCorrs.add(uc);
//				for (Entry<String, Double> entry : tmpNameVals.entrySet()) {
//					attrs.get(entry.getKey()).add(entry.getValue());
//				}
//			}
//
//			// }
//		}
//	}
//
//	/**
//	 * judge if there is too low count during the plan
//	 *
//	 * @param pcCtlListener
//	 *
//	 * @param plan
//	 * @return
//	 */
//
//	// private boolean hasTooLowCount(Plan plan) {
//	// planConverter.convert((PlanImpl) plan);
//	// cadyts.demand.Plan<Link> planSteps = planConverter.getPlanSteps();
//	//
//	// for (Iterator<PlanStep<Link>> planStepIt = planSteps.iterator();
//	// planStepIt
//	// .hasNext();) {
//	// PlanStep<Link> planStep = planStepIt.next();
//	// Id linkId = planStep.getLink().getId();
//	// /*
//	// * if counts contains this linkId. Yes, look at the time, judge if
//	// * count * varianceScale < minStdDev^2: yes -> break, directly
//	// * return true;
//	// */
//	// Count count = counts.getCount(linkId);
//	// if (count != null) {
//	// if (varianceScale
//	// * count.getVolume(
//	// planStep.getEntryTime_s() / countTimeBin + 1)
//	// .getValue()/* volume (count-y) */< minStdDev
//	// * minStdDev) {
//	// return true;
//	// }
//	// }
//	//
//	// }
//	// return false;
//	// }
//
//	public void init(PCCtlListener pcCtlListener, MultinomialLogitChoice chooser) {
//		cltListener = pcCtlListener;
//		this.chooser = chooser;
//	}
//}
