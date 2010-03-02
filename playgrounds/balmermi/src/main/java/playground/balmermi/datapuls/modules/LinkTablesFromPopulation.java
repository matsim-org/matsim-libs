/* *********************************************************************** *
 * project: org.matsim.*
 * LinkTablesFromPopulation.java
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

package playground.balmermi.datapuls.modules;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

/**
 * @author mrieser
 */
public final class LinkTablesFromPopulation {

	private final int timeBinSize;
	private final String outputDirectory;
	private final Network network;
	private final LeastCostPathCalculator router;
	private final Counter counter = new Counter("person #");
	
	private final HashMap<TransportMode, HashMap<Integer, BufferedWriter>> writers = new HashMap<TransportMode, HashMap<Integer, BufferedWriter>>();
	private final EnumSet<TransportMode> analyzedModes = EnumSet.of(TransportMode.walk, TransportMode.bike);
	private final HashMap<TransportMode, Double> speeds = new HashMap<TransportMode, Double>();
	
	public LinkTablesFromPopulation(final int timeBinSize,
			final String outputDirectory, final Network network, 
			final LeastCostPathCalculator router, final PlansCalcRouteConfigGroup config) {
		this.timeBinSize = timeBinSize;
		this.outputDirectory = outputDirectory;
		this.network = network;
		this.router = router;
		for (TransportMode mode : analyzedModes) {
			this.writers.put(mode, new HashMap<Integer, BufferedWriter>());
		}
		this.speeds.put(TransportMode.walk, config.getWalkSpeed());
		this.speeds.put(TransportMode.bike, config.getBikeSpeed());
	}
	
	public void run(final Population population) throws IOException {
		for (Person person : population.getPersons().values()) {
			counter.incCounter();
			run(person);
		}
		counter.printCounter();
		closeAllFiles();
	}

	private void run(Person person) throws IOException {
		Plan plan = person.getSelectedPlan();
		Id personId = person.getId();
		Activity prevActivity = null;
		Activity thisActivity = null;
		Leg prevLeg = null;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				prevLeg = (Leg) pe;
			}
			if (pe instanceof Activity) {
				prevActivity = thisActivity;
				thisActivity = (Activity) pe;
				
				if (prevActivity != null) {
					if (this.analyzedModes.contains(prevLeg.getMode())) {
						TransportMode mode = prevLeg.getMode();
						double time = prevActivity.getEndTime();
						Id fromFacilityId = prevActivity.getFacilityId();
						Id toFacilityId = thisActivity.getFacilityId();
						Node fromNode = network.getLinks().get(prevActivity.getLinkId()).getToNode();
						Node toNode = network.getLinks().get(thisActivity.getLinkId()).getFromNode();
						String fromActType = prevActivity.getType();
						String toActType = thisActivity.getType();
						double speed = this.speeds.get(mode).doubleValue();
						
						Path path = this.router.calcLeastCostPath(fromNode, toNode, time);
						for (Link link : path.links) {
							writeLine(mode, time, link.getId(), personId, fromActType, fromFacilityId, toActType, toFacilityId);
							time += link.getLength() / speed;
						}
					}
				}
			}
		}
	}
	
	private void closeAllFiles() {
		for (HashMap<Integer, BufferedWriter> writersByMode : this.writers.values()) {
			for (BufferedWriter writer : writersByMode.values()) {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private BufferedWriter getWriter(final TransportMode mode, final double time) {
		int timeBin = ((int) time) / timeBinSize;
		BufferedWriter writer = this.writers.get(mode).get(Integer.valueOf(timeBin));
		if (writer == null) {
			String filename = "/linkAnalysis_"+mode.toString()+"_"+(timeBin*this.timeBinSize)+"-"+((timeBin+1)*this.timeBinSize)+".txt.gz";
			try {
				writer = IOUtils.getBufferedWriter(this.outputDirectory + filename);
				writer.write("lid\tpid\tfromActType\tfromActFid\ttoActType\ttoActFid\n");
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			this.writers.get(mode).put(Integer.valueOf(timeBin), writer);
		}
		return writer;
	}
	
	private void writeLine(final TransportMode mode, final double time, 
			final Id linkId, final Id personId,
			final String fromActType, final Id fromFacilityId, 
			final String toActType, final Id toFacilityId) throws IOException {
		BufferedWriter writer = getWriter(mode, time);
		writer.write(linkId.toString());
		writer.write("\t");
		writer.write(personId.toString());
		writer.write("\t");
		writer.write(fromActType);
		writer.write("\t");
		writer.write(fromFacilityId.toString());
		writer.write("\t");
		writer.write(toActType);
		writer.write("\t");
		writer.write(toFacilityId.toString());
		writer.write("\n");
	}
}
