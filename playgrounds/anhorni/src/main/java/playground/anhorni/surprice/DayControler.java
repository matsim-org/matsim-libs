/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.surprice;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.anhorni.surprice.scoring.LaggedScoringFunctionFactory;

public class DayControler extends Controler {
	
	private AgentMemories memories = new AgentMemories();
	private String day;	
	ObjectAttributes incomes;
	
	public DayControler(final Config config, AgentMemories memories, String day, ObjectAttributes incomes) {
		super(config);	
		super.setOverwriteFiles(true);
		this.memories = memories;	
		this.day = day;
		this.incomes = incomes;
	} 
		
	protected void setUp() {
	    super.setUp();	
	    	    
	  	LaggedScoringFunctionFactory scoringFunctionFactory = new LaggedScoringFunctionFactory(
	  			this, this.config.planCalcScore(), this.network, this.memories, this.day, this.incomes);	  		
	  	this.setScoringFunctionFactory(scoringFunctionFactory);
	  	
	  	//this.addControlerListener(new ScoringFunctionResetter()); TODO: check if really not necessary anymore!
	  	this.addControlerListener(new Memorizer(this.memories, this.day));
	}
}
