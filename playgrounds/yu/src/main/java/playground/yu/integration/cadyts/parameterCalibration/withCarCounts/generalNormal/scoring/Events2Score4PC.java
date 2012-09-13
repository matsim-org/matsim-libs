///* *********************************************************************** *
// * project: org.matsim.*
// * MnlChoice.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
///**
// *
// */
//package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.generalNormal.scoring;
//
//import java.util.List;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.population.Leg;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.api.core.v01.population.Plan;
//import org.matsim.api.core.v01.population.PlanElement;
//import org.matsim.api.core.v01.population.Route;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.groups.ControlerConfigGroup;
//import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
//import org.matsim.core.controler.OutputDirectoryHierarchy;
//import org.matsim.core.population.routes.NetworkRoute;
//import org.matsim.core.scoring.ScoringFunctionFactory;
//import org.matsim.core.utils.collections.Tuple;
//
//import playground.yu.integration.cadyts.CalibrationConfig;
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.BseStrategyManager;
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.CadytsChoice;
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.MultinomialLogitChoice;
//import playground.yu.scoring.withAttrRecorder.Events2Score4AttrRecorder;
//import playground.yu.utils.io.SimpleWriter;
//import utilities.math.BasicStatistics;
//import utilities.math.MultinomialLogit;
//import utilities.math.Vector;
//
///**
// * @author yu
// * 
// */
//public class Events2Score4PC extends Events2Score4AttrRecorder implements
//		MultinomialLogitChoice, CadytsChoice {
//	// public final static List<String> attrNameList = new ArrayList<String>();
//	// public final static List<Double> paramScaleFactorList = new
//	// ArrayList<Double>();
//
//	// private static final String PARAM_SCALE_FACTOR_INDEX =
//	// "paramScaleFactor_";
//
//	// protected final Config config;
//	// protected Population pop = null;
//	// protected ScoringFunctionFactory sfFactory = null;
//	// protected PlanCalcScoreConfigGroup scoring;
//	// // protected boolean setPersonScore = true;
//	// protected int maxPlansPerAgent;
//	// protected final TreeMap<Id, Tuple<Plan, ScoringFunction>> agentScorers =
//	// new TreeMap<Id, Tuple<Plan, ScoringFunction>>();
//	// protected final TreeMap<Id, Integer> agentPlanElementIndex = new
//	// TreeMap<Id, Integer>();
//
//	private final static Logger log = Logger.getLogger(Events2Score4PC.class);
//
//	private MultinomialLogit mnl;
//	// private boolean outputCalcDetail = false;
//	private SimpleWriter writer = null;
//	boolean setUCinMNL = false, addUCtoScore = true;
//
//	public Events2Score4PC(Config config, ScoringFunctionFactory sfFactory,
//			Scenario scenario) {
//		super(config, sfFactory, scenario);
//
//		mnl = createMultinomialLogit(config);
//
//		String setUCinMNLStr = config.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME, "setUCinMNL");
//		if (setUCinMNLStr != null) {
//			setUCinMNL = Boolean.parseBoolean(setUCinMNLStr);
//			System.out.println("BSE:\tsetUCinMNL\t=\t" + setUCinMNL);
//		} else {
//			System.out.println("BSE:\tsetUCinMNL\t= default value\t"
//					+ setUCinMNL);
//		}
//
//		String addUCtoScoreStr = config.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME, "addUCtoScore");
//		if (addUCtoScoreStr != null) {
//			addUCtoScore = Boolean.parseBoolean(addUCtoScoreStr);
//			System.out.println("BSE:\taddUCtoScore\t=\t" + addUCtoScore);
//		} else {
//			System.out.println("BSE:\taddUCtoScore\t= default value\t"
//					+ addUCtoScore);
//		}
//	}
//
//	private Vector buildAttrVector(Plan plan) {
//		Vector attrVector = new Vector(attrNameList.size());
//		for (String attrName : attrNameList) {
//			int attrNameIndex = attrNameList.indexOf(attrName);
//			Object o = plan.getCustomAttributes().get(attrName);
//
//			attrVector.set(attrNameIndex,
//					o == null ? 0d : Double.parseDouble(o.toString()));
//		}
//		return attrVector;
//	}
//
//	/**
//	 * should be called after that all setPersonScore(Person) have been called
//	 * in every iteration.
//	 */
//	public void closeWriter() {
//		if (writer != null) {
//			writer.close();
//		}
//	}
//
//	protected MultinomialLogit createMultinomialLogit(Config config) {
//		int choiceSetSize = config.strategy().getMaxAgentPlanMemorySize(), // =4
//		attributeCount = Integer.parseInt(config.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME, "attributeCount"));
//
//		double traveling = scoring.getTraveling_utils_hr();
//		double betaStuck = Math.min(
//				Math.min(scoring.getLateArrival_utils_hr(),
//						scoring.getEarlyDeparture_utils_hr()),
//				Math.min(traveling, scoring.getWaiting_utils_hr()));
//
//		// initialize MultinomialLogit
//		MultinomialLogit mnl = new MultinomialLogit(choiceSetSize,// =4
//				attributeCount);// =5 [travCar,travPt,travWalk,Perf,Stuck]
//
//		mnl.setUtilityScale(scoring.getBrainExpBeta());
//
//		for (int i = 0; i < choiceSetSize; i++) {
//			mnl.setASC(i, 0);
//		}
//		// travelTime
//		int attrNameIndex = attrNameList.indexOf("traveling");
//		mnl.setCoefficient(attrNameIndex, traveling);
//
//		attrNameIndex = attrNameList.indexOf("travelingPt");
//		mnl.setCoefficient(attrNameIndex, scoring.getTravelingPt_utils_hr());
//
//		attrNameIndex = attrNameList.indexOf("travelingWalk");
//		mnl.setCoefficient(attrNameIndex, scoring.getTravelingWalk_utils_hr());
//
//		//
//		attrNameIndex = attrNameList.indexOf("performing");
//		mnl.setCoefficient(attrNameIndex, scoring.getPerforming_utils_hr());
//		//
//		attrNameIndex = attrNameList.indexOf("stuck");
//		mnl.setCoefficient(attrNameIndex, betaStuck);
//
//		// distances
//		attrNameIndex = attrNameList.indexOf("monetaryDistanceCostRateCar");
//		mnl.setCoefficient(
//				attrNameIndex,
//				scoring.getMonetaryDistanceCostRateCar()
//						* scoring.getMarginalUtilityOfMoney());
//
//		attrNameIndex = attrNameList.indexOf("monetaryDistanceCostRatePt");
//		mnl.setCoefficient(
//				attrNameIndex,
//				scoring.getMonetaryDistanceCostRatePt()
//						* scoring.getMarginalUtilityOfMoney());
//
//		attrNameIndex = attrNameList.indexOf("marginalUtlOfDistanceWalk");
//		mnl.setCoefficient(attrNameIndex,
//				scoring.getMarginalUtlOfDistanceWalk());
//
//		// constants
//		attrNameIndex = attrNameList.indexOf("constantCar");
//		mnl.setCoefficient(attrNameIndex, scoring.getConstantCar());
//
//		attrNameIndex = attrNameList.indexOf("constantPt");
//		mnl.setCoefficient(attrNameIndex, scoring.getConstantPt());
//
//		attrNameIndex = attrNameList.indexOf("constantWalk");
//		mnl.setCoefficient(attrNameIndex, scoring.getConstantWalk());
//
//		return mnl;
//	}
//
//	public void createWriter() {
//
//		ControlerConfigGroup ctlCfg = config.controler();
//		if (iteration <= ctlCfg.getLastIteration()
//				&& iteration > ctlCfg.getLastIteration() - 100) {
//			// outputCalcDetail = true;
//			OutputDirectoryHierarchy ctlIO = new OutputDirectoryHierarchy(ctlCfg.getOutputDirectory(), false);
//			writer = new SimpleWriter(ctlIO.getIterationFilename(iteration,
//					"scoreCalcDetails.log.gz"));
//
//			StringBuilder head = new StringBuilder("AgentID");
//			for (String attrName : attrNameList) {
//				head.append("\t");
//				head.append(attrName);
//			}
//			head.append("\tselected");
//			writer.writeln(head);
//
//		}
//	}
//
//	@Override
//	public MultinomialLogit getMultinomialLogit() {
//		return mnl;
//	}
//
//	@Override
//	public PlanCalcScoreConfigGroup getScoring() {
//		return scoring;
//	}
//
//	@Override
//	public void reset(List<Tuple<Id, Plan>> toRemoves) {
//		// just dummy
//	}
//
//	private void setAttr2MNL(int choiceIdx, String attrName, Plan plan) {
//		int attrNameIndex = attrNameList.indexOf(attrName);
//		Object o = plan.getCustomAttributes().get(attrName);
//		mnl.setAttribute(choiceIdx, attrNameIndex,
//				o != null ? ((Number) o).doubleValue() : 0d);
//	}
//
//	public void setMultinomialLogit(MultinomialLogit mnl) {
//		this.mnl = mnl;
//	}
//
//	/**
//	 * set Attr. plans of a person. This method should be called after
//	 * removedPlans, i.e. there should be only choiceSetSize plans in the memory
//	 * of an agent.
//	 * 
//	 * @param person
//	 */
//	@Override
//	public void setPersonAttrs(Person person, BasicStatistics[] statistics) {
//		Id personId = person.getId();
//
//		List<? extends Plan> plans = person.getPlans();
//
//		if (plans.size() <= maxPlansPerAgent)/* with mnl */{
//			for (Plan plan : plans) {
//				int choiceIdx = plans.indexOf(plan);
//
//				// choice set index & size check
//				if (choiceIdx < 0 || choiceIdx >= mnl.getChoiceSetSize()) {
//					log.warn("IndexOutofBound, choiceIdx<0 or >=choiceSetSize!\nperson "
//							+ personId + " the " + choiceIdx + ". Plan");
//					throw new RuntimeException();
//				}
//
//				// System.out.println(">>>>>plan customAttr.\t"
//				// + plan.getCustomAttributes().toString());
//				// set attributes to MultinomialLogit
//				for (String attrName : attrNameList) {
//					setAttr2MNL(choiceIdx, attrName, plan);
//				}
//				// System.out.println(">>>>>MNL Attr.\t"
//				// + mnl.getAttributesView().toString());
//				// ##########################################################
//				/*
//				 * ASC (utilityCorrection, ASC for "stay home" Plan in the
//				 * future...), just for experiments, in general ASC should NOT
//				 * be set in MNL, because of the newest @
//				 * ChoiceParameterCalibrator4
//				 */
//				// #############################################
//
//				Object uc = plan.getCustomAttributes().get(
//						BseStrategyManager.UTILITY_CORRECTION);
//
//				// add UC as ASC into MNL
//				if (setUCinMNL) {
//					mnl.setASC(choiceIdx, uc != null ? (Double) uc : 0d);
//				}
//			}
//		}
//	}
//
//	/**
//	 * set Attr. and Utility (not the score in MATSim) of plans of a person.
//	 * This method should be called after removedPlans, i.e. there should be
//	 * only choiceSetSize plans in the memory of an agent.
//	 * 
//	 * @param person
//	 * @param monetaryDistanceCostRateCarStats
//	 */
//	@Override
//	public void setPersonScore(Person person) {
//		Vector coeff = mnl.getCoeff();
//		for (Plan plan : person.getPlans()) {
//
//			// calculate utility of the plan
//			Vector attrVector = buildAttrVector(plan);
//
//			Object uc = plan.getCustomAttributes().get(
//					BseStrategyManager.UTILITY_CORRECTION);
//			double utilCorrection = addUCtoScore ? uc != null ? (Double) uc
//					: 0d : 0d;
//
//			double util = coeff/*
//								 * s. the attributes order in
//								 * Events2Score4PC2.attrNameList
//								 */
//			.innerProd(attrVector) + utilCorrection
//			/* utilityCorrection is also an important ASC */;
//			plan.setScore(util);
//			if (writer != null) {
//				writer.writeln("/////CALC-DETAILS of PERSON\t" + person.getId()
//						+ "\t////////////////\n/////coeff\tattr");
//				for (int i = 0; i < coeff.size(); i++) {
//					writer.writeln("/////\t" + coeff.get(i) + "\t"
//							+ attrVector.get(i) + "\t/////");
//				}
//				writer.writeln("/////\tUtiliy Correction\t=\t" + utilCorrection
//						+ "\t/////\n/////\tscore before replanning\t=\t" + util
//						+ "\t/////");
//				for (PlanElement pe : plan.getPlanElements()) {
//					if (pe instanceof Leg) {
//						Route route = ((Leg) pe).getRoute();
//						if (route instanceof NetworkRoute) {
//							writer.write("/////\tRoute :\t"
//									+ ((NetworkRoute) route).getLinkIds()
//									+ "\t");
//							if (plan.isSelected()) {
//								writer.write("selected");
//							}
//							writer.writeln("/////\n");
//						}
//						break;
//					}
//				}
//				writer.writeln("///////////////////////////////////////");
//				writer.flush();
//			}
//		}
//	}
//
//	public void setSinglePlanAttrs(Plan plan, MultinomialLogit mnl) {
//		Id personId = plan.getPerson().getId();
//
//		int choiceIdx = 0; // plans.indexOf(plan)
//
//		// if (plans.size() <= maxPlansPerAgent)/* with mnl */{
//		// choice set index & size check
//		if (choiceIdx < 0 || choiceIdx >= mnl.getChoiceSetSize()) {
//			log.warn("IndexOutofBound, choiceIdx<0 or >=choiceSetSize!\nperson "
//					+ personId + " the " + choiceIdx + ". Plan");
//			throw new RuntimeException();
//		}
//
//		// set attributes to MultinomialLogit
//		// set attributes to MultinomialLogit
//		for (String attrName : attrNameList) {
//			setAttr2MNL(choiceIdx, attrName, plan);
//		}
//
//		// ##########################################################
//		/*
//		 * ASC (utilityCorrection, ASC for "stay home" Plan in the future...)
//		 */
//		// #############################################
//
//		// add UC as ASC into MNL
//
//		if (setUCinMNL) {
//			Object uc = plan.getCustomAttributes().get(
//					BseStrategyManager.UTILITY_CORRECTION);
//			mnl.setASC(choiceIdx, uc != null ? (Double) uc : 0d);
//		}
//
//	}
//}
