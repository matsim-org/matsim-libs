/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesLoadCalculator.java.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.anhorni.scenarios.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.io.IOUtils;


public class CityShoppingCalculator implements ShutdownListener {
	
	private int numberOfCityShoppingLocations = -1;
	private double expenditurePerLocation[];
	private int shopCityLinkIds[];

	public void notifyShutdown(ShutdownEvent event) {
		this.evaluate(event);
		this.printStatistics(event);
	}
	
	public CityShoppingCalculator(int numberOfCityShoppingLocations, int shopCityLinkIds []) {
		this.numberOfCityShoppingLocations = numberOfCityShoppingLocations;
		this.shopCityLinkIds = shopCityLinkIds;
		this.expenditurePerLocation = new double[numberOfCityShoppingLocations];
	}
	
	private void evaluate(ShutdownEvent event) {
		for (Person p : event.getControler().getPopulation().getPersons().values()) {			
			int shopLocIndex = -1;								
			final List<? extends PlanElement> actslegs = p.getSelectedPlan().getPlanElements();
			for (int j = 0; j < actslegs.size(); j=j+2) {
				ActivityImpl act = (ActivityImpl)actslegs.get(j);
				if (act.getType().equals("sc")) {
					shopLocIndex = ArrayUtils.indexOf(this.shopCityLinkIds, Integer.parseInt(act.getLinkId().toString()));
					double expenditure = Double.parseDouble(((PersonImpl)(p)).getDesires().getDesc());
					this.expenditurePerLocation[shopLocIndex] += expenditure;
				}	
			}
		}
	}

	private void printStatistics(ShutdownEvent event) {		
		String runId = event.getControler().getConfig().findParam("controler", "runId");
				
		try {
			
			String outputPath = "src/main/java/playground/anhorni/scenarios/3towns/output/";
			if (runId.contains("random")) {
				outputPath = "src/main/java/playground/anhorni/scenarios/3towns/output/random/";
				new File(outputPath).mkdir();
			}

			final BufferedWriter out =
				IOUtils.getBufferedWriter(outputPath + runId + "_cityShopping.txt");
			for (int i = 0; i < this.numberOfCityShoppingLocations; i++) {
				out.append("loc_" + i + "\t");
			}
			out.newLine();				
			for (int i = 0; i < this.numberOfCityShoppingLocations; i++) {
				out.write(String.valueOf(expenditurePerLocation[i]) + "\t");	
			}
			out.flush();
			out.close();
		} catch (final IOException e) {
			Gbl.errorMsg(e);
		}
	}
}
