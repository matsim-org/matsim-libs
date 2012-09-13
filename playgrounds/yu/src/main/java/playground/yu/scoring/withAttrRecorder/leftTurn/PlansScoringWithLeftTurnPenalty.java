///* *********************************************************************** *
// * project: org.matsim.*
// * DummyPlansScoring4PC.java
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
//package playground.yu.scoring.withAttrRecorder.leftTurn;
//
//import org.apache.log4j.Logger;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.controler.events.IterationStartsEvent;
//import org.matsim.core.controler.events.ScoringEvent;
//import org.matsim.core.controler.events.StartupEvent;
//import org.matsim.core.controler.listener.IterationStartsListener;
//import org.matsim.core.controler.listener.ScoringListener;
//import org.matsim.core.controler.listener.StartupListener;
//import org.matsim.core.events.handler.EventHandler;
//
//import playground.yu.scoring.Events2ScoreI;
//import playground.yu.scoring.PlansScoringI;
//
///**
// * a changed copy of {@code PlansScoring} for the parameter calibration,
// * especially in order to put new parameters to CharyparNagelScoringConfigGroup
// * 
// * @author yu
// * 
// */
//public class PlansScoringWithLeftTurnPenalty implements PlansScoringI,
//		StartupListener, ScoringListener, IterationStartsListener {
//	private final static Logger log = Logger
//			.getLogger(PlansScoringWithLeftTurnPenalty.class);
//	protected Events2ScoreI planScorer;
//
//	@Override
//	public Events2ScoreI getPlanScorer() {
//		return planScorer;
//	}
//
//	@Override
//	public void notifyIterationStarts(final IterationStartsEvent event) {
//		planScorer.reset(event.getIteration());
//	}
//
//	@Override
//	public void notifyScoring(final ScoringEvent event) {
//		planScorer.finish();
//	}
//
//	@Override
//	public void notifyStartup(final StartupEvent event) {
//		Controler ctl = event.getControler();
//
//		planScorer = new Events2ScoreWithLeftTurnPenalty(ctl.getConfig(),
//				ctl.getScoringFunctionFactory(), ctl.getScenario());
//
//		log.debug("PlansScoringWithLeftTurnPenalty loaded ScoringFunctionFactory");
//
//		ctl.getEvents().addHandler((EventHandler) planScorer);
//	}
//
//}
