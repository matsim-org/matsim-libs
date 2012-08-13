/* *********************************************************************** *
 * project: org.matsim.*
 * DgDefaultMatsimBuilderRegistry
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
package playground.dgrether.designdrafts.controller;

import org.apache.log4j.Logger;


/**
 * @author dgrether
 *
 */
public class DgDefaultMatsimBuilderRegistry implements DgMATSimBuilderRegistry {

	private static final Logger log = Logger.getLogger(DgDefaultMatsimBuilderRegistry.class);
	
	private DgMobsimBuilder mobsimBuilder;
	private DgReplanningBuilder replanningBuilder;
	private DgScoringBuilder scoringBuilder;
	
	@Override
	public DgMobsimBuilder getMobsimBuilder() {
		return mobsimBuilder;
	}
	
	@Override
	public void setMobsimBuilder(DgMobsimBuilder mobsimBuilder) {
		this.mobsimBuilder = mobsimBuilder;
	}
	
	@Override
	public DgReplanningBuilder getReplanningBuilder() {
		return replanningBuilder;
	}
	
	@Override
	public void setReplanningBuilder(DgReplanningBuilder replanningBuilder) {
		this.replanningBuilder = replanningBuilder;
	}
	
	@Override
	public DgScoringBuilder getScoringBuilder() {
		return scoringBuilder;
	}
	
	@Override
	public void setScoringBuilder(DgScoringBuilder scoringBuilder) {
		//this warning could be replaced by an exception that is thrown if not both are replaced
		log.warn("Changing the scoring implies in nearly all usecases that you have to change the replanning as well!");
		this.scoringBuilder = scoringBuilder;
	}

	@Override
	public void addCustomModelBuilder(Object o) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean removeCustomModelBuilder(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T> T getCustomModelBuilder(Class<? extends T> klass) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
