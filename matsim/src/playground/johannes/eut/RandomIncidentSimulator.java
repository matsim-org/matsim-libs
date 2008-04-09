/* *********************************************************************** *
 * project: org.matsim.*
 * RandomIncidentSimulator.java
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

/**
 *
 */
package playground.johannes.eut;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;

import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;

/**
 * @author illenberger
 *
 */
public class RandomIncidentSimulator implements IterationStartsListener, IterationEndsListener {

	private final double incidentProba;

	private double capReduction = 0.7;

	private int startIteration = 0;

//	private final QueueNetworkLayer network;

	private final List<QueueLink> changedCaps = new LinkedList<QueueLink>();

	private final List<QueueLink> links = new LinkedList<QueueLink>();

	private BufferedWriter writer;

	public RandomIncidentSimulator(QueueNetworkLayer network, double proba) {
//		this.network = network;
		this.incidentProba = proba;

//		links.add((QueueLink) network.getLink("800"));
//		links.add((QueueLink) network.getLink("1100"));
//		links.add((QueueLink) network.getLink("1400"));
//		links.add((QueueLink) network.getLink("5"));

		try {
			this.writer = new BufferedWriter(new FileWriter(Controler.getOutputFilename("incidents.txt")));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addLink(QueueLink link) {
		this.links.add(link);
	}

	public void setCapReduction(double factor) {
		this.capReduction = factor;
	}

	public double getCapReduction() {
		return this.capReduction;
	}

	public void setStartIteration(int iteration) {
		this.startIteration = iteration;
	}

	public int getStartIteration() {
		return this.startIteration;
	}

	public void notifyIterationStarts(IterationStartsEvent event) {
		/*
		 * Reduce capacity here...
		 */
		try {

			this.writer.write(String.valueOf(event.getIteration()));
			this.writer.write("\t");

			for (QueueLink link : this.links) {
				Gbl.random.nextDouble();
				if ((Gbl.random.nextDouble() < this.incidentProba)
						&& (event.getIteration() >= this.startIteration)) {

					link.changeSimulatedFlowCapacity(this.capReduction);
					this.changedCaps.add(link);

					this.writer.write("\t");
					this.writer.write(link.getLink().getId().toString());
				}

			}

			this.writer.newLine();
			this.writer.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		for (QueueLink link : this.links) {
			if (this.changedCaps.contains(link)) {
				link.changeSimulatedFlowCapacity(1.0 / this.capReduction);
				this.changedCaps.remove(link);
			}
		}

	}

}
