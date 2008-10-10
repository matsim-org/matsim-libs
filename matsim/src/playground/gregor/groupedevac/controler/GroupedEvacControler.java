/* *********************************************************************** *
 * project: org.matsim.*
 * GroupedEvacControler.java
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

package playground.gregor.groupedevac.controler;

import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import org.matsim.controler.corelisteners.PlansDumping;
import org.matsim.controler.corelisteners.PlansReplanning;
import org.matsim.controler.corelisteners.PlansScoring;
import org.matsim.evacuation.EvacuationAreaFileReader;
import org.matsim.evacuation.EvacuationAreaLink;
import org.matsim.network.NetworkWriter;
import org.xml.sax.SAXException;

public class GroupedEvacControler extends Controler {

	private final static Logger log = Logger.getLogger(GroupedEvacControler.class);
	
	private final HashMap<Id, EvacuationAreaLink> evacuationAreaLinks = new HashMap<Id, EvacuationAreaLink>();
	
	public GroupedEvacControler(final String[] args) {
		super(args);
	}

	@Override
	protected void setup() {
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
		new GroupedEvacuationPlansGeneratorAndNetworkTrimmer().generatePlans(this.population, this.network, this.evacuationAreaLinks);
		log.info("done");

		log.info("writing network xml file... ");
		new NetworkWriter(this.network, getOutputFilename("evacuation_net.xml")).write();
		log.info("done");

		// then do the regular setup with the modified data

		super.setup();
	}

	@Override
	protected void loadCoreListeners() {
		/* The order how the listeners are added is very important!
		 * As dependencies between different listeners exist or listeners
		 * may read and write to common variables, the order is important.
		 * Example: The RoadPricing-Listener modifies the scoringFunctionFactory,
		 * which in turn is used by the PlansScoring-Listener.
		 * Note that the execution order is contrary to the order the listeners are added to the list.
		 */

		this.addCoreControlerListener(new CoreControlerListener());

		
		
		this.addCoreControlerListener(new EvacDestinationAssigner(this.travelCostCalculator,this.travelTimeCalculator,this.network));
		
		
		// the default handling of plans
//		this.addCoreControlerListener(new AggregatedPlansScoring());
//		this.addCoreControlerListener(new SelectedPlansScoring());
		this.addCoreControlerListener(new PlansScoring());

		this.addCoreControlerListener(new PlansReplanning());
		this.addCoreControlerListener(new PlansDumping());
	}

	public static void main(final String[] args) {
		final Controler controler = new GroupedEvacControler(args);
		controler.run();
		System.exit(0);
	}
	
	
}
