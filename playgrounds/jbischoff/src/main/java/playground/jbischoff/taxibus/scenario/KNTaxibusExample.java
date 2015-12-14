/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.jbischoff.taxibus.scenario;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator.ArbitraryEventScoring;
import org.matsim.core.scoring.ScoringFunctionAccumulator.LegScoring;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters.Mode;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.jbischoff.taxibus.run.configuration.ConfigBasedTaxibusLaunchUtils;
import playground.jbischoff.taxibus.run.configuration.TaxibusConfigGroup;
import playground.jbischoff.taxibus.scenario.analysis.quick.TTEventHandler;
import playground.jbischoff.taxibus.scenario.analysis.quick.TaxiBusTravelTimesAnalyzer;

/**
 * @author jbischoff
 *
 */
public class KNTaxibusExample {

	public static void main(String[] args) {

		Config config = ConfigUtils.loadConfig("../../../shared-svn/projects/vw_rufbus/scenario/input/example/configVWTB.xml", new TaxibusConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		new ConfigBasedTaxibusLaunchUtils(controler).initiateTaxibusses(true);

		TaxiBusTravelTimesAnalyzer a = new TaxiBusTravelTimesAnalyzer();
		TTEventHandler b = new TTEventHandler();
		controler.getEvents().addHandler(a);
		controler.getEvents().addHandler(b);
		controler.run();
		a.printOutput();
		b.printOutput();
	}

	
	
}
