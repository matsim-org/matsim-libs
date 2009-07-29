/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.marcel.pt.application;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.api.experimental.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.transitSchedule.TransitScheduleWriterV1;
import org.matsim.transitSchedule.api.TransitScheduleReader;
import org.xml.sax.SAXException;

import playground.marcel.pt.config.TransitConfigGroup;
import playground.marcel.pt.routerintegration.PlansCalcTransitRoute;
import playground.mohit.converter.Visum2TransitSchedule;
import playground.mohit.converter.VisumNetwork;
import playground.mohit.converter.VisumNetworkReader;

public class Application1 {

	private static final Logger log = Logger.getLogger(Application1.class);

	private final static String DILUTED_PT_1PCT_PLANS_FILENAME = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/plans.census2000ivtch1pct.dilZh30km.pt.xml.gz";
	private final static String DILUTED_PT_ROUTED_1PCT_PLANS_FILENAME = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/plans.census2000ivtch1pct.dilZh30km.pt-routed.xml.gz";

	private final ScenarioImpl scenario;
	private final Config config;

	public Application1() {
		this.scenario = new ScenarioImpl();
		this.config = this.scenario.getConfig();
	}

	protected void prepareConfig() {
		this.config.scenario().setUseTransit(true);
	}

	protected void convertSchedule() {
		final VisumNetwork vNetwork = new VisumNetwork();
		try {
			log.info("reading visum network.");
			new VisumNetworkReader(vNetwork).read("/Volumes/Data/VSP/coding/eclipse35/thesis-data/networks/yalcin/ptzh_orig.net");
			log.info("converting visum data to TransitSchedule.");
			new Visum2TransitSchedule(vNetwork, this.scenario.getTransitSchedule()).convert();
			log.info("writing TransitSchedule to file.");
			new TransitScheduleWriterV1(this.scenario.getTransitSchedule()).write("../thesis-data/application/zuerichSchedule.xml");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void readSchedule() {
		log.info("reading TransitSchedule from file.");
		try {
			new TransitScheduleReader(this.scenario).readFile("../thesis-data/application/zuerichSchedule.xml");
//			new TransitScheduleReader(this.scenario).readFile("../shared-svn/studies/schweiz-ivtch/pt-experimental/TransitSim/transitSchedule.xml");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}


	protected void routePopulation() {
		PopulationImpl pop = this.scenario.getPopulation();
		try {
			new MatsimPopulationReader(this.scenario).parse(DILUTED_PT_1PCT_PLANS_FILENAME);
			pop.printPlansCount();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		FreespeedTravelTimeCost timeCostCalculator = new FreespeedTravelTimeCost(this.scenario.getConfig().charyparNagelScoring());
		TransitConfigGroup transitConfig = new TransitConfigGroup();
		PlansCalcTransitRoute router = new PlansCalcTransitRoute(this.scenario.getConfig().plansCalcRoute(),
				this.scenario.getNetwork(), timeCostCalculator, timeCostCalculator, dijkstraFactory,
				this.scenario.getTransitSchedule(), transitConfig);
		log.info("start pt-router");
		router.run(pop);
		log.info("write routed plans out.");
		new PopulationWriter(pop).write(DILUTED_PT_ROUTED_1PCT_PLANS_FILENAME);
	}

	public static void main(final String[] args) {
		Application1 app = new Application1();
		app.prepareConfig();
//		app.convertSchedule();
		app.readSchedule(); // either convert, or read, but not both!
		app.routePopulation();

		log.info("done.");
	}

}
