///* *********************************************************************** *
// * project: org.matsim.*
// * ScorAttrWriteTrigger.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
//package playground.yu.scoring.withAttrRecorder;
//
//import org.matsim.core.controler.Controler;
//import org.matsim.core.controler.events.IterationEndsEvent;
//import org.matsim.core.controler.listener.IterationEndsListener;
//
//import playground.yu.integration.cadyts.CalibrationConfig;
//
///**
// * decides when to write scoring function attributes into a ASCII file
// * 
// * @author yu
// * 
// */
//public class ScorAttrWriteTrigger implements IterationEndsListener {
//
//	@Override
//	public void notifyIterationEnds(IterationEndsEvent event) {
//		int iteration = event.getIteration();
//		Controler controler = event.getControler();
//		String writeScorAttrIntervalStr = controler.getConfig().findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME,
//				"writeScorAttrInterval");
//		if (writeScorAttrIntervalStr != null) {
//			int writeScorAttrInterval = Integer
//					.parseInt(writeScorAttrIntervalStr);
//			if (writeScorAttrInterval > 0
//					&& iteration % writeScorAttrInterval == 0) {
//				((Events2Score4AttrRecorder) ((ControlerWithAttrRecorderI) controler)
//						.getPlansScoring4AttrRecorder().getPlanScorer())
//						.writeScoreAttrs(controler
//								.getControlerIO()
//								.getIterationFilename(iteration, "scorAttr.log"));
//			}
//		}
//	}
//
//}
