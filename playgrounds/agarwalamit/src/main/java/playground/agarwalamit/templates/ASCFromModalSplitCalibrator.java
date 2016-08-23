/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.templates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;

import com.google.inject.Inject;

import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.utils.MapUtils;

/**
 * This is just a template which needs to be updated before use. For e.g. run it
 * 
 * @author amit
 */

public class ASCFromModalSplitCalibrator implements StartupListener, IterationStartsListener, IterationEndsListener {

	private static final Logger LOG = Logger.getLogger(ASCFromModalSplitCalibrator.class);
	
	private final SortedMap<String, Double> initialMode2share = new TreeMap<>(); // if empty, take from it.0
	private SortedMap<String, Double> previousASC ; // all zeros
	
	private final List<String> availableModes = new ArrayList<>(); //AA_TODO : if multiple mode choice modules are used, this will only work with default. 
	
	@Inject ModalShareEventHandler modalShareEventHandler;
	@Inject Scenario scenario;

	private int updateASCAfterIts = 10;
	private int innovationStop ;

	public ASCFromModalSplitCalibrator(final SortedMap<String, Double> mode2share, final int updateASCIterations){
		this.initialMode2share.clear(); 
		this.initialMode2share.putAll( mode2share );
		this.updateASCAfterIts = updateASCIterations;
		this.availableModes.clear();
		this.availableModes.addAll(this.initialMode2share.keySet());
		initializeInitialASC();
	}

	public ASCFromModalSplitCalibrator(final int updateASCIterations){
		this(new TreeMap<>(),updateASCIterations);
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		Config config =event.getServices().getScenario().getConfig();
		this.innovationStop = (int) ( ( config.controler().getLastIteration() - config.controler().getFirstIteration() ) 
				* config.strategy().getFractionOfIterationsToDisableInnovation() );
		
		if(this.availableModes.isEmpty()){ // dont override the modes from input share.
			this.availableModes.addAll( Arrays.asList(config.changeMode().getModes()) );	
		}
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		int itr = event.getIteration();
		
		if(itr==0){
			if(this.initialMode2share.isEmpty()){
				SortedMap<String, Integer> mode2legs = modalShareEventHandler.getMode2numberOflegs();
				updateModes(mode2legs);// this is required if, at least one sub-population exists without mode choice and different modes
				this.initialMode2share.clear();
				this.initialMode2share.putAll(MapUtils.getIntPercentShare(mode2legs));
				
				initializeInitialASC();
				
				event.getServices().getEvents().removeHandler(modalShareEventHandler);
			}
		} else {
			if( itr%updateASCAfterIts==0 && itr <= innovationStop ) { //AA_TODO: not sure if update it during innovation stop 
				
				SortedMap<String, Integer> mode2legs = modalShareEventHandler.getMode2numberOflegs();
				updateModes(mode2legs); // this is required if, at least one sub-population exists without mode choice and different modes
				
				SortedMap<String, Double> mode2shre = MapUtils.getIntPercentShare(mode2legs);

				// update ascs
				updateASC(mode2shre);
				
				// update in scenario
				for (String mode : this.previousASC.keySet()) {
					event.getServices().getScenario().getConfig().planCalcScore().getOrCreateModeParams(mode).setConstant(this.previousASC.get(mode));
				}
				event.getServices().getEvents().removeHandler(modalShareEventHandler);
			}
		}
	}

	// asc update is required only for modes which are available for mode choice
	private void updateModes( SortedMap<String,Integer> mode2legs){
		Iterator<Entry<String,Integer>> it = mode2legs.entrySet().iterator();
		while(it.hasNext()){
			Entry<String,Integer> e = it.next();
			if (! this.availableModes.contains(e.getKey()) ) it.remove(); 
		}
	}
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		int itr = event.getIteration();
		if(itr==0){
			if(this.initialMode2share.isEmpty()) {// store initial modal split it not provided
				event.getServices().getEvents().addHandler(modalShareEventHandler);	
			}
		} else {
			if(itr%updateASCAfterIts==0 && itr <= innovationStop ) {
				event.getServices().getEvents().addHandler(modalShareEventHandler);
			}
		}
	}
	
	private void initializeInitialASC (){
		this.previousASC = new TreeMap<>();
		for (String mode : this.initialMode2share.keySet()){
			this.previousASC.put(mode, 0.0);
		}
	}
	
	private void updateASC( final SortedMap<String,Double> modeShareNow ){
		SortedMap<String, Double> ascs = new TreeMap<>();
		double lowestASC = Double.POSITIVE_INFINITY;
		
		for(Entry<String, Double> e: this.previousASC.entrySet()) {
			String mode = e.getKey();
			double asc = e.getValue() - Math.log(   modeShareNow.get(mode)  /  this.initialMode2share.get(mode) );
			ascs.put(mode, asc);
			lowestASC = Math.min(lowestASC, asc);
		}

		// number of non-zero ascs can not be greater than number of modes-1
		for(String mode :ascs.keySet()){
			double asc = ascs.get(mode) - lowestASC;
			LOG.warn("The previous ASC for "+ mode +" was "+ this.previousASC.get(mode) +". It is changed to "+ asc);
			this.previousASC.put(mode, asc);
		}
	}
}