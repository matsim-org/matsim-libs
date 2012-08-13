/* *********************************************************************** *
 * project: org.matsim.*
 * MATSimFactoryRegistry
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


/**
 * @author dgrether
 *
 */
public interface DgMATSimBuilderRegistry {

	public void setScoringBuilder(DgScoringBuilder scoringBuilder);

	public DgScoringBuilder getScoringBuilder();

	public void setReplanningBuilder(DgReplanningBuilder replanningBuilder);

	public DgReplanningBuilder getReplanningBuilder();

	public void setMobsimBuilder(DgMobsimBuilder mobsimBuilder);

	public  DgMobsimBuilder getMobsimBuilder();
	
	/**
	 * Same as for scenario, we need a way to plug in more than the above
	 */
	public void addCustomModelBuilder(Object o );
	public boolean removeCustomModelBuilder(Object o);
	public <T> T getCustomModelBuilder(Class<? extends T> klass);
	
	

}
