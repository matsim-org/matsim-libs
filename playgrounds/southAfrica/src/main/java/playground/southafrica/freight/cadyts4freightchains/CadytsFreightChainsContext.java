/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsPlanStrategy.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.freight.cadyts4freightchains;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.CadytsBuilder;
import org.matsim.contrib.cadyts.general.CadytsContextI;
import org.matsim.contrib.cadyts.general.CadytsCostOffsetsXMLFileIO;
import org.matsim.contrib.cadyts.general.LookUpItemFromId;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.demand.Plan;

class Item implements Identifiable<Item>, Comparable<Item> {
	private final Id<Item> id;
	Item( Id<Item> id ) {
		this.id = id ;
	}
	@Override
	public Id<Item> getId() {
		return this.id ;
	}
	@Override
	public int compareTo(Item o) {
		return id.toString().compareTo( o.getId().toString() ) ;
	}
	@Override
	public String toString() {
		return this.id.toString() ;
	}
}
/**
 */
class CadytsFreightChainsContext implements CadytsContextI<Item>, BeforeMobsimListener, IterationEndsListener {

	private final static Logger log = Logger.getLogger(CadytsFreightChainsContext.class);

	private final static String LINKOFFSET_FILENAME = "linkCostOffsets.xml";
	private static final String FLOWANALYSIS_FILENAME = "flowAnalysis.txt";

	/** 
	 * this is the method which is able to tell cadyts in which way a particular plan contributes to measurements.  Essentially, it
	 * follows the plan and registers every time the plan touches a measurement.  Here, there is only one measurement,
	 * so this is boring (and overkill).
	 * 
	 * JWJ, 28/01/2016: Errors occurred, so I just fixed it by applying the quick-fix recommendations.
	 */
	private final PlansTranslator<Item> plansTranslator = new PlansTranslator<Item>() {
		@Override
		public Plan<Item> getCadytsPlan(org.matsim.api.core.v01.population.Plan arg0) {
			// TODO Auto-generated method stub
			return null;
		}
	} ;

	private final AnalyticalCalibrator<Item> calibrator;

	private final SimResultsImpl<Item> simResults ;

	private final Map<Id<Item>,Item> itemContainer = new HashMap<>() ;

	private final LookUpItemFromId<Item> lookUp = new LookUpItemFromId<Item>() {
		@Override
		public Item getItem(Id<Item> id) {
			return itemContainer.get(id) ;
		}
	};

	CadytsFreightChainsContext( Config config, List<Integer> nChainsOfLength ) {
		// define and register the observations:
		Counts counts = new Counts() ;
		for ( int ii=0 ; ii<nChainsOfLength.size() ; ii++ ) {
			Id<Link> id = Id.create(ii, Link.class) ;
			Count count = counts.createAndAddCount( id, "chain_of_length_" + Integer.toString(ii) ) ;
			int time = 1 ; // dummy
			long cnt = nChainsOfLength.get(ii) ;
			count.createVolume(time, cnt ) ;

			// also produce the "items" since we don't have any objects in the simulation to which we can attach the count values:
			Id<Item> itemId = Id.create(id, Item.class);
			itemContainer.put( itemId, new Item( itemId ) ) ;
		}

		// build the calibrator. This is a static method, and in consequence has no side effects
		this.calibrator = CadytsBuilder.buildCalibratorAndAddMeasurements(config, counts , lookUp , Item.class);
		
		// prepare the sim results container:
		this.simResults = new SimResultsImpl<Item>( itemContainer.values() ) ;
	}

	private Item getCorrectItemFromPlan(org.matsim.api.core.v01.population.Plan plan) {
		int numberOfActs = PopulationUtils.getActivities(plan, null ).size();
		final Item item = itemContainer.get( Id.create(numberOfActs, Link.class) );
		if ( item==null ) {
			log.error("don't have a prepared item for numberOfActs=" + numberOfActs );
			throw new RuntimeException("error") ;
		}
		return item;
	}
	@Override
	public void notifyBeforeMobsim(final BeforeMobsimEvent event ) {
		this.simResults.reset() ;
	}
	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		String analysisFilepath = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), FLOWANALYSIS_FILENAME);
		this.calibrator.setFlowAnalysisFile(analysisFilepath);

		// since we have not constructed the output _during_ the mobsim, we need to do it now:
		for ( Person person : event.getServices().getScenario().getPopulation().getPersons().values() ) {
			Item item = getCorrectItemFromPlan( person.getSelectedPlan() ) ;
			this.simResults.incCnt(item);
		}
		log.warn( "simResults for cadyts:\n" + this.simResults.toString() ) ;
		this.calibrator.afterNetworkLoading(this.simResults);
		
		// write some output
		String filename = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), LINKOFFSET_FILENAME);
		try {
			new CadytsCostOffsetsXMLFileIO<Item>( this.lookUp, Item.class ).write(filename, this.calibrator.getLinkCostOffsets());
		} catch (IOException e) {
			log.error("Could not write link cost offsets.  Continuing anyway ...", e);
		}
	}

	@Override
	public AnalyticalCalibrator<Item> getCalibrator() {
		return this.calibrator ;
	}

	@Override
	public PlansTranslator<Item> getPlansTranslator() {
		return this.plansTranslator ;
	}

	LookUpItemFromId<Item> getLookUp() {
		return lookUp;
	}

}