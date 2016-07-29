/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.illustrative.daganzo.run;

import java.util.Calendar;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import scenarios.illustrative.analysis.TtAbstractAnalysisTool;
import scenarios.illustrative.analysis.TtAnalyzedResultsWriter;
import scenarios.illustrative.analysis.TtListenerToBindAndWriteAnalysis;
import scenarios.illustrative.daganzo.analysis.TtAnalyzeDgDaganzo;

/**
 * Run a simulation of the daganzo scenario of DG
 * 
 * @author tthunig
 *
 */
public class RunDgDaganzoSimulation {

	private static final String BASE_DIR = "../../matsim/examples/daganzo/";
	
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(BASE_DIR + "config.xml");
//		config.controler().setOutputDirectory("../../../runs-svn/daganzo/DGScenario/" + createDateString() + "_network21Length100_longRouteSelected/");
		config.controler().setOutputDirectory("../../../runs-svn/daganzo/DGScenario/" + createDateString() + "_network22_shortRouteSelected/");
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists ) ;
		
//		config.plans().setInputFile(BASE_DIR + "plans_long_route_selected.xml.gz");
		config.plans().setInputFile(BASE_DIR + "plans_short_route_selected.xml.gz");
		
//		config.network().setInputFile(BASE_DIR + "network21.xml");
		config.network().setInputFile(BASE_DIR + "network22.xml");
		
		config.controler().setWriteEventsInterval(config.controler().getLastIteration());
		config.vspExperimental().setWritingOutputEvents(true);
		
		// remove unmaterialized module
		config.removeModule("otfvis");
		
		config.controler().setLastIteration(100);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
//		scenario.getNetwork().getLinks().get(Id.createLinkId(4)).setLength(100);
		
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
//				this.bind(TtAnalyzeDgDaganzo.class).asEagerSingleton();
//				this.addEventHandlerBinding().to(TtAnalyzeDgDaganzo.class);
				this.bind(TtAbstractAnalysisTool.class).to(TtAnalyzeDgDaganzo.class).asEagerSingleton();
				this.addEventHandlerBinding().to(TtAbstractAnalysisTool.class);
				this.bind(TtAnalyzedResultsWriter.class);
				this.addControlerListenerBinding().to(TtListenerToBindAndWriteAnalysis.class);
			}
		});
		
		controler.run();
	}

	/**
	 * @return the current date in format "yyyy-mm-dd"
	 */
	private static String createDateString() {
		Calendar cal = Calendar.getInstance();
		// this class counts months from 0, but days from 1
		int month = cal.get(Calendar.MONTH) + 1;
		String monthStr = month + "";
		if (month < 10)
			monthStr = "0" + month;
		String date = cal.get(Calendar.YEAR) + "-" + monthStr + "-" + cal.get(Calendar.DAY_OF_MONTH);
		
		return date;
	}

}
