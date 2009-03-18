/* *********************************************************************** *
 * project: org.matsim.*
 * RiskCostControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.risk;

import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.evacuation.EvacuationAreaFileReader;
import org.matsim.evacuation.EvacuationAreaLink;
import org.matsim.evacuation.EvacuationPlansGeneratorAndNetworkTrimmer;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.NetworkWriter;
import org.matsim.trafficmonitoring.TravelTimeCalculatorBuilder;
import org.xml.sax.SAXException;

public class RiskCostController extends Controler {

	
	private final static Logger log = Logger.getLogger(RiskCostController.class);
	
	private final HashMap<Id, EvacuationAreaLink> evacuationAreaLinks = new HashMap<Id, EvacuationAreaLink>();

	
	public RiskCostController(final String[] args) {
		super(args);
	}

	
	@Override
	protected void setup() {
		
//		TravelTimeAggregatorFactory factory = new TravelTimeAggregatorFactory();
//		factory.setTravelTimeDataPrototype(TravelTimeDataHashMap.class);
//		factory.setTravelTimeAggregatorPrototype(PessimisticTravelTimeAggregator.class);
//		double endTime = this.config.simulation().getEndTime() > 0 ? this.config.simulation().getEndTime() : 30*3600;
		RiskCostCalculator rc = new RiskCostCalculator(this.network, false);
		this.events.addHandler(rc);
		double endTime = this.config.simulation().getEndTime() > 0 ? this.config.simulation().getEndTime() : 30*3600;
		if (this.travelTimeCalculator == null) {
			this.travelTimeCalculator = new TravelTimeCalculatorBuilder(this.config.controler()).createTravelTimeCalculator(this.network, (int)endTime);
		}
		
//		TravelTimeAndSocialCostCalculator t = new TravelTimeAndSocialCostCalculator(this.network,this.config.controler().getTraveltimeBinSize(),(int)endTime,factory);
//		this.events.removeHandler(this.travelTimeCalculator);
//		this.travelTimeCalculator = t;
//		this.events.addHandler(sc);
		this.travelCostCalculator = new RiskAverseTravelCostCalculator(this.travelTimeCalculator,rc);
		
		
		// first modify network and plans

		try {
			String evacuationAreaLinksFile = this.config.evacuation().getEvacuationAreaFile();
			new EvacuationAreaFileReader(this.evacuationAreaLinks).readFile(evacuationAreaLinksFile);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("generating initial evacuation plans... ");
		EvacuationPlansGeneratorAndNetworkTrimmer e = new EvacuationPlansGeneratorAndNetworkTrimmer();
		e.setTravelCostCalculator(this.getTravelCostCalculator());
		e.generatePlans(this.population, this.network, this.evacuationAreaLinks);
		log.info("done");

		log.info("writing network xml file... ");
		new NetworkWriter(this.network, getOutputFilename("evacuation_net.xml")).write();
		log.info("done");


		super.setup();
		
		

		this.strategyManager = loadStrategyManager();
//		this.addControlerListener(sc);
	}
	
	
	public static void main(final String [] args) {
		final Controler controler = new RiskCostController(args);
		controler.run();
		System.exit(0);
	}
	
}
