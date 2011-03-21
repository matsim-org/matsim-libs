/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.benchmark;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.misc.ArgumentParser;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.mrieser.core.mobsim.usecases.OptimizedCarSimFactory;

/**
 * New version of the MATSim Benchmark. Given a network, it automatically generates
 * agents for it and runs 10 iterations on it.
 *
 * @author mrieser
 */
public class BenchmarkV2 {

	private final Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	public void convertNetwork() {
		OsmNetworkReader osmReader = new OsmNetworkReader(this.scenario.getNetwork(), new WGS84toCH1903LV03());
		osmReader.setKeepPaths(false);
		osmReader.parse("/Volumes/Data/projects/benchmarkV2/zurich.osm");
		new NetworkCleaner().run(this.scenario.getNetwork());
		for (Link link : this.scenario.getNetwork().getLinks().values()) {
			if (link.getFreespeed() == 0.0) {
				link.setFreespeed(5.0); // fix data problems
			}
		}
		new NetworkWriter(this.scenario.getNetwork()).write("network.xml.gz");
	}

	public void loadNetwork() {
		new MatsimNetworkReader(this.scenario).readFile("network.xml.gz");
	}

	public void createPopulation(final int count, final String outputDir) {
		Random r = new Random(4711);
		Link[] links = this.scenario.getNetwork().getLinks().values().toArray(new Link[this.scenario.getNetwork().getLinks().size()]);
		PopulationFactory pf = this.scenario.getPopulation().getFactory();
		for (int i = 0; i < count; i++) {
			Person p = pf.createPerson(new IdImpl(i));
			Link homeRegionLink = links[r.nextInt(links.length)];
			Coord homeCoord = new CoordImpl(
					(homeRegionLink.getToNode().getCoord().getX() + homeRegionLink.getFromNode().getCoord().getX()) / 2 + r.nextInt(1000) - 500,
					(homeRegionLink.getToNode().getCoord().getY() + homeRegionLink.getFromNode().getCoord().getY()) / 2 + r.nextInt(1000) - 500);

			double rangeMedian = 0;
			boolean medianAccepted = false;
			double twoSigmaSquare = 2 * 0.4 * 0.4;
			double sqrt = Math.sqrt(2 * Math.PI * 0.4 * 0.4);
			while (!medianAccepted) {
				rangeMedian = r.nextDouble() * 30.0; // in km
				double probability = 8.0 / (rangeMedian * sqrt) * Math.exp(-Math.pow(Math.log(rangeMedian) - 1.1, 2.0) / twoSigmaSquare); // Lognormal with sigma=0.4 and mu=1.1, and multiplied with 8
				if (probability <= r.nextDouble()) {
					medianAccepted = true;
					rangeMedian *= 1000; // convert into meters
				}
			}

			double lowerRange = rangeMedian - 500;
			double upperRange = rangeMedian + 500;
			boolean linkIsInRange = false;
			int counter = 0;
			Link workRegionLink = null;
			Coord workCoord = null;
			while (!linkIsInRange && counter < 100000) {
				workRegionLink = links[r.nextInt(links.length)];
				workCoord = new CoordImpl(
						(workRegionLink.getToNode().getCoord().getX() + workRegionLink.getFromNode().getCoord().getX()) / 2 + r.nextInt(1000) - 500,
						(workRegionLink.getToNode().getCoord().getY() + workRegionLink.getFromNode().getCoord().getY()) / 2 + r.nextInt(1000) - 500);
				double dist = CoordUtils.calcDistance(homeCoord, workCoord);
				if (dist >= lowerRange && dist <= upperRange) {
					linkIsInRange = true;
				}
			}

			double startTime = 8.0*3600 + r.nextGaussian()*3600;
			startTime = Math.max(startTime, 5*3600);
			startTime = Math.min(startTime, 14*3600);
			double workDuration = 8.5*3600 + r.nextGaussian()*2*3600;
			workDuration = Math.max(workDuration, 15*60);
			workDuration = Math.min(workDuration, 12*3600);

			Activity h1 = pf.createActivityFromCoord("home", homeCoord);
			h1.setEndTime(startTime);
			Leg l1 = pf.createLeg("car");
			Activity w = pf.createActivityFromCoord("work", workCoord);
			w.setEndTime(startTime + workDuration);
			Leg l2 = pf.createLeg("car");
			Activity h2 = pf.createActivityFromCoord("home", homeCoord);

			Plan plan = pf.createPlan();
			plan.addActivity(h1);
			plan.addLeg(l1);
			plan.addActivity(w);
			plan.addLeg(l2);
			plan.addActivity(h2);

			p.addPlan(plan);
			this.scenario.getPopulation().addPerson(p);
		}
		new PopulationWriter(this.scenario.getPopulation(), this.scenario.getNetwork()).write(outputDir + "/population.xml.gz");
	}

	public void runScenario(final int nOfTheadsReplanning, final int nOfThreadsSim, final int nOfThreadsEvents,
			final double flowFactor, final String outputDir, final String mobsim) {
		Config c = this.scenario.getConfig();
		c.controler().setLastIteration(10);
		c.controler().setEventsFileFormats(EnumSet.of(EventsFileFormat.xml));
		c.controler().setWriteEventsInterval(5);
		c.controler().setRoutingAlgorithmType(RoutingAlgorithmType.AStarLandmarks);

		if ("qsim".equals(mobsim)) {
			c.addQSimConfigGroup(new QSimConfigGroup());
	//		c.getQSimConfigGroup().setSnapshotStyle("queue");
			c.getQSimConfigGroup().setEndTime(30.0*3600);
			c.getQSimConfigGroup().setFlowCapFactor(flowFactor);
			if (nOfThreadsSim > 0) {
				c.getQSimConfigGroup().setNumberOfThreads(nOfThreadsSim);
			}
		}

		c.setParam("strategy", "Module_1", "ChangeExpBeta");
		c.setParam("strategy", "ModuleProbability_1", "0.4");
		c.setParam("strategy", "Module_2", "ReRoute");
		c.setParam("strategy", "ModuleProbability_2", "0.3");
		c.setParam("strategy", "Module_3", "TimeAllocationMutator");
		c.setParam("strategy", "ModuleProbability_3", "0.3");

		ActivityParams actParams = new ActivityParams("home");
		actParams.setTypicalDuration(8.0 * 3600);
		c.planCalcScore().addActivityParams(actParams);

		actParams = new ActivityParams("work");
		actParams.setTypicalDuration(8.0 * 3600);
		c.planCalcScore().addActivityParams(actParams);

		c.controler().setOutputDirectory(outputDir + "/sim/");

		c.global().setNumberOfThreads(nOfTheadsReplanning);

		if (nOfThreadsEvents > 0) {
			Module m = new Module("parallelEventHandling");
			m.addParam("numberOfThreads", Integer.toString(nOfThreadsEvents));
			c.addModule("parallelEventHandling", m);
		}

		Controler ctrl = new Controler((ScenarioImpl) this.scenario);
		if ("newsim".equals(mobsim)) {
			System.out.println("using newsim mobsim-factory");
			OptimizedCarSimFactory factory = new OptimizedCarSimFactory(nOfThreadsSim);
			factory.setMobsimStopTime(30.0 * 3600);
			factory.setPopulationWeight(1.0 / flowFactor);
			ctrl.setMobsimFactory(factory);
		}
		ctrl.setCreateGraphs(false);
		ctrl.run();
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
//		args=new String[]{"-r2", "-s 2", "-e=0", "-a10000", "-f1.0", "-m newsim"};

		double flowFactor = 1.0;
		int nOfThreadsReplanning = 4;
		int nOfThreadsSim = 3;
		int nOfThreadsEvents = 1;
		int nOfAgents = 10000;
		String mobsim = "qsim";

		String outputDir = "output_";
		Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    outputDir += sdf.format(cal.getTime());

		Iterator<String> iter = new ArgumentParser(args, false).iterator();
		while (iter.hasNext()) {
			String arg = iter.next();
			if (arg.startsWith("-h")) {
				System.out.println("MATSim Benchmark v2");
				System.out.println("Arguments: ");
				System.out.println("  -r n    Number of threads for replanning [default: " + nOfThreadsReplanning + "]");
				System.out.println("  -s n    Number of threads for simulation [default: " + nOfThreadsSim + "]");
				System.out.println("  -e n    Number of threads for events handling [default: " + nOfThreadsEvents + "]");
				System.out.println("  -a n    Number of agents to generate [default: " + nOfAgents + "]");
				System.out.println("  -f n    Flow capacity factor to use [default: " + flowFactor + "]");
				System.out.println("  -o dir  output-directory to use [default: output_DATE-TIME]");
				System.out.println("  -m qsim|newsim   simulation to use [default: " + mobsim + "]");
				return;
			} else if (arg.startsWith("-r")) {
				if (arg.length() == 2) {
					nOfThreadsReplanning = Integer.parseInt(iter.next().trim());
				} else {
					nOfThreadsReplanning = Integer.parseInt(arg.substring(2).trim());
				}
			} else if (arg.startsWith("-s")) {
				if (arg.length() == 2) {
					nOfThreadsSim = Integer.parseInt(iter.next().trim());
				} else {
					nOfThreadsSim = Integer.parseInt(arg.substring(2).trim());
				}
			} else if (arg.startsWith("-e")) {
				if (arg.length() == 2) {
					nOfThreadsEvents = Integer.parseInt(iter.next().trim());
				} else {
					nOfThreadsEvents = Integer.parseInt(arg.substring(2).trim());
				}
			} else if (arg.startsWith("-a")) {
				if (arg.length() == 2) {
					nOfAgents = Integer.parseInt(iter.next().trim());
				} else {
					nOfAgents = Integer.parseInt(arg.substring(2).trim());
				}
			} else if (arg.startsWith("-f")) {
				if (arg.length() == 2) {
					flowFactor = Float.parseFloat(iter.next().trim());
				} else {
					flowFactor = Float.parseFloat(arg.substring(2).trim());
				}
			} else if (arg.startsWith("-o")) {
				if (arg.length() == 2) {
					outputDir = iter.next().trim();
				} else {
					outputDir = arg.substring(2).trim();
				}
			} else if (arg.startsWith("-m")) {
				if (arg.length() == 2) {
					mobsim = iter.next().trim();
				} else {
					mobsim = arg.substring(2).trim();
				}
			} else {
				System.out.println("Argument " + arg + " not recognized. Ignoring it.");
			}
		}

		System.out.println("MATSim-Benchmark v2");
		System.out.println("# agents: " + nOfAgents);
		System.out.println("# threads replanning: " + nOfThreadsReplanning);
		System.out.println("# threads simulation: " + nOfThreadsSim);
		System.out.println("# threads events:     " + nOfThreadsEvents);
		System.out.println("flow capacity factor: " + flowFactor);
		System.out.println("used mobility sim:    " + mobsim);
		System.out.println("output directory:     " + outputDir);

		File outputDirF = new File(outputDir);
		if (!outputDirF.exists()) {
			outputDirF.mkdir();
		}

		BufferedWriter infoWriter = IOUtils.getBufferedWriter(outputDir + "/info.txt");
		infoWriter.write("MATSim-Benchmark v2\n");
		infoWriter.write("# agents: " + nOfAgents + "\n");
		infoWriter.write("# threads replanning: " + nOfThreadsReplanning + "\n");
		infoWriter.write("# threads simulation: " + nOfThreadsSim + "\n");
		infoWriter.write("# threads events:     " + nOfThreadsEvents + "\n");
		infoWriter.write("flow capacity factor: " + flowFactor + "\n");
		infoWriter.write("used mobility sim:    " + mobsim + "\n");
		infoWriter.write("output directory:     " + outputDir + "\n");
		infoWriter.close();

		BenchmarkV2 app = new BenchmarkV2();
//		app.convertNetwork();
		app.loadNetwork();
		app.createPopulation(nOfAgents, outputDir);
		app.runScenario(nOfThreadsReplanning, nOfThreadsSim, nOfThreadsEvents, flowFactor, outputDir, mobsim);
	}
}
