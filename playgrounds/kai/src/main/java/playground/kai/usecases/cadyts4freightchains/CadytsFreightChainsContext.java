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

package playground.kai.usecases.cadyts4freightchains;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.CadytsBuilder;
import org.matsim.contrib.cadyts.general.CadytsContextI;
import org.matsim.contrib.cadyts.general.CadytsCostOffsetsXMLFileIO;
import org.matsim.contrib.cadyts.general.LookUp;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.utils.misc.PopulationUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.demand.Plan;
import cadyts.demand.PlanBuilder;
import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.supply.SimResults;

class Item implements Identifiable {
	private final Id id;
	private double cnt = 0. ;
	Item( Id id ) {
		this.id = id ;
	}
	@Override
	public Id getId() {
		return this.id ;
	}
	double getCnt() {
		return this.cnt ;
	}
	void incCnt() {
		cnt++ ;
	}
}
/**
 * {@link PlanStrategy Plan Strategy} used for replanning in MATSim which uses Cadyts to
 * select plans that better match to given occupancy counts.
 */
class CadytsFreightChainsContext implements CadytsContextI<Item>, IterationEndsListener {

	private final static Logger log = Logger.getLogger(CadytsFreightChainsContext.class);

	private final static String LINKOFFSET_FILENAME = "linkCostOffsets.xml";
	private static final String FLOWANALYSIS_FILENAME = "flowAnalysis.txt";

	private final PlansTranslator<Item> plansTranslator ;

	private final AnalyticalCalibrator<Item> calibrator;
	private final SimResults<Item> simResults;

	private final Map<Id,Item> itemContainer = new HashMap<Id,Item>() ;

	private final LookUp<Item> lookUp;

	CadytsFreightChainsContext( Config config ) {
		// define and register the observations:
		Counts counts = new Counts() ;
		for ( int ii=0 ; ii< 20 ; ii++ ) {
			Id id = new IdImpl(ii) ;

			Count count = counts.createAndAddCount( id, "chain_of_length_" + Integer.toString(ii) ) ;
			count.createVolume(0, ii ) ; // fill with real values!

			// also produce the "items" since we don't have any objects in the simulation to which we can attach the count values:
			itemContainer.put( id, new Item( id ) ) ;
		}

		// need to be able to find the items based on the ids (possibly, LookUp should be replaced by a regular map)
		this.lookUp = new LookUp<Item>() {
			@Override
			public Item lookUp(Id id) {
				return itemContainer.get(id) ;
			}
		};

		// build the calibrator. This is a static method, and in consequence has no side effects
		this.calibrator = CadytsBuilder.buildCalibrator(config, counts , lookUp );

		// prepare the container into which to put results:
		this.simResults = new SimResults<Item>(){
			private static final long serialVersionUID = 1L;
			@Override
			public double getSimValue(Item item, int time1, int time2, TYPE arg3) {
				switch( arg3 ) {
				case COUNT_VEH:
					return item.getCnt() ;
					// (we can either put results into the simResults, or attach them to item.  here we are doing the latter)
				default:
					throw new RuntimeException("not implemented") ;
				}
			}
		} ;
		
		// this is the method which is able to tell cadyts in which way a particular plan contributes to measurements.  Essentially, it
		// follows the plan and registers every time the plan touches a measurement.  Here, there is only one measurement,
		// so this is boring (and overkill).
		this.plansTranslator = new PlansTranslator<Item>() {
			@Override
			public Plan<Item> getPlanSteps(org.matsim.api.core.v01.population.Plan plan) {
				Item item = getCorrectItemFromPlan(plan);
				
				PlanBuilder<Item> planBuilder = new PlanBuilder<Item>() ;
				int time = 0 ; // there is no time here but we need to set something 
				planBuilder.addEntry( item, time );
				// (the meaning of this is that this plan contributes one counts unit to this item)
				
				return planBuilder.getResult();
			}

		} ;

	}

	private Item getCorrectItemFromPlan(org.matsim.api.core.v01.population.Plan plan) {
		int numberOfActs = PopulationUtils.getActivities(plan, null ).size();
		return itemContainer.get( new IdImpl(numberOfActs) );
	}
	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		// since we have not constructed the output _during_ the mobsim, we need to do it now:
		for ( Person person : event.getControler().getScenario().getPopulation().getPersons().values() ) {
			Item item = getCorrectItemFromPlan( person.getSelectedPlan() ) ;
			item.incCnt() ;
		}
		// (the sim results are now in the items, which means that SimResults will return them)
		// yyyy this is overall not the right way; simResults should store this.

		String analysisFilepath = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), FLOWANALYSIS_FILENAME);
		this.calibrator.setFlowAnalysisFile(analysisFilepath);

		this.calibrator.afterNetworkLoading(this.simResults);
		
		// write some output
		String filename = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), LINKOFFSET_FILENAME);
		try {
			new CadytsCostOffsetsXMLFileIO<Item>( this.lookUp ).write(filename, this.calibrator.getLinkCostOffsets());
		} catch (IOException e) {
			log.error("Could not write link cost offsets!", e);
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

	LookUp<Item> getLookUp() {
		return lookUp;
	}

}