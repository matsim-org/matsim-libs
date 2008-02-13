/* *********************************************************************** *
 * project: org.matsim.*
 * TransimsPostProcessor.java
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

package org.matsim.utils.vis.snapshots.postprocessor;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.utils.vis.snapshots.postprocessor.processors.DestinationDependentColorizer;
import org.matsim.utils.vis.snapshots.postprocessor.processors.PostProcessorI;
import org.matsim.utils.vis.snapshots.postprocessor.processors.TimeDependentColorizer;
import org.matsim.world.World;



public class TransimsSnapshotFilePostProcessor {

	private static final Logger log = Logger.getLogger(TransimsSnapshotFilePostProcessor.class);

	private TransimsSnapshotFileReader reader;

	private TransimsSnapshotFileWriter writer;

	private Plans plans;

	private List<PostProcessorI> processors;

	public TransimsSnapshotFilePostProcessor(Plans plans, final String tVehFile){
		this.plans = plans;
		this.reader = new TransimsSnapshotFileReader(tVehFile);

		String outfile = "./output/colorizedT.veh.txt"; 
		this.writer = new TransimsSnapshotFileWriter(outfile);

		this.processors = new ArrayList<PostProcessorI>();
		addProcessor(new TimeDependentColorizer(this.plans));
		addProcessor(new DestinationDependentColorizer(this.plans));

	}

	public void run(){

		this.writer.writeLine(this.reader.readLine()); // first line should be the header
		String [] line = this.reader.readLine();
		while (line != null){

			for (PostProcessorI processor : this.processors){
				line = processor.processEvent(line);
			}

			this.writer.writeLine(line);
			line = this.reader.readLine();
		}

		this.writer.finish();

	}


	public void addProcessor(PostProcessorI processor){
		this.processors.add(processor);
	}


	public static void main(String [] args){

		String tVehFile;

		if (args.length != 2) {
			throw new RuntimeException("wrong number of arguments! Pleas run TransimsSnaphotFilePostProcessor config.xml T.veh.gz");
		} else {
			Gbl.createConfig(new String[]{args[0], "config_v1.dtd"});
			tVehFile = args[1];
		}

		World world = Gbl.createWorld();

		log.info("loading network from " + Gbl.getConfig().network().getInputFile());
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		world.setNetworkLayer(network);
		world.complete();
		log.info("done.");

		log.info("loading population from " + Gbl.getConfig().plans().getInputFile());
		Plans population = new Plans();
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		log.info("done.");

		TransimsSnapshotFilePostProcessor tpp = new TransimsSnapshotFilePostProcessor(population,tVehFile);
		tpp.run();
	} 
}
