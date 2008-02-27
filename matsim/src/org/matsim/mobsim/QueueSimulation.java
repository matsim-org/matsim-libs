/* *********************************************************************** *
 * project: org.matsim.*
 * QueueSimulation.java
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

package org.matsim.mobsim;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentStuck;
import org.matsim.events.Events;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.netvis.VisConfig;
import org.matsim.utils.vis.netvis.streaming.SimStateWriterI;
import org.matsim.utils.vis.snapshots.writers.KmlSnapshotWriter;
import org.matsim.utils.vis.snapshots.writers.PlansFileSnapshotWriter;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;
import org.matsim.utils.vis.snapshots.writers.SnapshotWriterI;
import org.matsim.utils.vis.snapshots.writers.TransimsSnapshotWriter;

class PersonAlgo_CheckSelected extends PersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// run Method, creates a new Vehicle for every person
	//////////////////////////////////////////////////////////////////////

	/* (non-Javadoc)
	 * @see org.matsim.plans.algorithms.PersonAlgorithm#run(org.matsim.plans.Person)
	 */
	@Override
	public void run(final Person person) {
		// Choose Plan to follow
		// this actually ensures that there is one plan selected
		Plan selected = person.getSelectedPlan();
		if (selected == null) {
			if (person.getPlans().size() != 0) {
				person.setSelectedPlan(person.getPlans().get(0));
			}
		}
	}
}

//////////////////////////////////////////////////////////////////////
// A Queue Simulator class
//////////////////////////////////////////////////////////////////////
public class QueueSimulation extends Simulation {

	private int snapshotPeriod = Integer.MAX_VALUE;

	protected static final int INFO_PERIOD = 3600;

	private final Config config;
	protected final Plans plans;
	protected final QueueNetworkLayer network;

	private PersonAlgo_CreateVehicle veh_algo = new PersonAlgo_CreateVehicle();
	protected EventWriterTXT myeventwriter = null;

	protected static Events events = null; // TODO [MR] instead of making this static and Links/Nodes using QueueSimulation.getEvents(), Gbl should hold a global events-object
	protected  SimStateWriterI netStateWriter = null;

	private final List<SnapshotWriterI> snapshotWriters = new ArrayList<SnapshotWriterI>();

	/**
	 * teleportationList includes all vehicle that have transportation modes unknown to
	 * the QueueSimulation (i.e. != "car") or have two activities on the same link
 	 */
	private static PriorityQueue<Vehicle> teleportationList = new PriorityQueue<Vehicle>(30, new VehicleDepartureTimeComparator());

	final private static Logger log = Logger.getLogger(QueueSimulation.class);

	public QueueSimulation(final QueueNetworkLayer net, final Plans plans, final Events events) {
		super();
		setEvents(events);
		this.plans = plans;
		this.network = net;
		this.config = Gbl.getConfig();
	}

	// creating vehicles with PersonAlgo_CreateVehicles
	protected final void createAgents() {

		if (this.plans == null) {
			throw new RuntimeException("No valid Population found (plans == null)");
		}
		// add veh algo and run it through all plans
		if (this.veh_algo != null) {
			//Make sure SOME plan has been selected, select plan number 0
			this.plans.addAlgorithm(new PersonAlgo_CheckSelected());
			this.plans.addAlgorithm(this.veh_algo);
			this.plans.runAlgorithms();
			this.plans.clearAlgorithms();
		}

	}

	protected void prepareNetwork() {
		this.network.moveWaitFirst(this.config.simulation().moveWaitFirst());
		this.network.beforeSim();
	}

	public void openNetStateWriter(final String snapshotFilename, final String networkFilename, final int snapshotPeriod) {
		/* TODO [MR] I don't really like it that we change the configuration on the fly here.
		 * In my eyes, the configuration should usually be a read-only object in general, but
		 * that's hard to be implemented...
		 */
		this.config.network().setInputFile(networkFilename);
		this.config.simulation().setSnapshotFormat("netvis");
		this.config.simulation().setSnapshotPeriod(snapshotPeriod);
		this.config.simulation().setSnapshotFile(snapshotFilename);
	}

	private void createSnapshotwriter() {
		// Initialize Snapshot file
		this.snapshotPeriod = (int) this.config.simulation().getSnapshotPeriod();

		// A snapshot period of 0 or less indicates that there should be NO snapshot written
		if (this.snapshotPeriod > 0 ) {
			String snapshotFormat =  this.config.simulation().getSnapshotFormat();

			if (snapshotFormat.contains("plansfile")) {
				String snapshotFilePrefix = Controler.getIterationPath() + "/positionInfoPlansFile";
				String snapshotFileSuffix = "xml";
				this.snapshotWriters.add(new PlansFileSnapshotWriter(snapshotFilePrefix,snapshotFileSuffix));
			}
			if (snapshotFormat.contains("transims")) {
				String snapshotFile = Controler.getIterationFilename("T.veh");
				this.snapshotWriters.add(new TransimsSnapshotWriter(snapshotFile));
			}
			if (snapshotFormat.contains("googleearth")) {
				String snapshotFile = Controler.getIterationFilename("googleearth.kmz");
				String coordSystem = this.config.global().getCoordinateSystem();
				this.snapshotWriters.add(new KmlSnapshotWriter(snapshotFile,
						TransformationFactory.getCoordinateTransformation(coordSystem, TransformationFactory.WGS84)));
			}
			if (snapshotFormat.contains("netvis")) {
				String snapshotFile;

				if (Controler.getIteration() == -1 ) snapshotFile = this.config.simulation().getSnapshotFile();
				else snapshotFile = Controler.getIterationPath() + "/Snapshot";

				File networkFile = new File(this.config.network().getInputFile());
				VisConfig myvisconf = VisConfig.newDefaultConfig();
				String[] params = {VisConfig.LOGO, VisConfig.DELAY, VisConfig.LINK_WIDTH_FACTOR, VisConfig.SHOW_NODE_LABELS, VisConfig.SHOW_LINK_LABELS};
				for (String param : params) {
					String value = this.config.findParam("vis", param);
					if (value != null) {
						myvisconf.set(param, value);
					}
				}
				// OR do it like this: buffers = Integer.parseInt(Config.getSingleton().getParam("temporal", "buffersize"));
				// Automatic reasoning about buffersize, so that the file will be about 5MB big...
				int buffers = this.network.getLinks().size();
				String buffString = this.config.findParam("vis", "buffersize");
				if (buffString == null) {
					buffers = Math.max(5, Math.min(500000/buffers, 100));
				} else buffers = Integer.parseInt(buffString);

				this.netStateWriter = new QueueNetStateWriter(this.network, networkFile.getAbsolutePath(), myvisconf, snapshotFile, this.snapshotPeriod, buffers);
				this.netStateWriter.open();
			}
		} else this.snapshotPeriod = Integer.MAX_VALUE; // make sure snapshot is never called
	}

	//////////////////////////////////////////////////////////////////////
	// Prepare Simulation, get all settings from config.xml
	//////////////////////////////////////////////////////////////////////
	@Override
	protected void  prepareSim() {
		if (events == null) {
			throw new RuntimeException("No valid Events Object (events == null)");
		}

		prepareNetwork();

		double startTime = this.config.simulation().getStartTime();
		this.stopTime = this.config.simulation().getEndTime();

		if (startTime == Time.UNDEFINED_TIME) startTime = 0.0;
		if (this.stopTime == Time.UNDEFINED_TIME || this.stopTime == 0) this.stopTime = Double.MAX_VALUE;

		SimulationTimer.setSimStartTime(24*3600);
		SimulationTimer.setTime(startTime);

		createAgents();

		// set sim start time to config-value ONLY if this is LATER than the first plans starttime
		SimulationTimer.setSimStartTime(Math.max(startTime,SimulationTimer.getSimStartTime()));
		SimulationTimer.setTime(SimulationTimer.getSimStartTime());

		createSnapshotwriter();
	}

	//////////////////////////////////////////////////////////////////////
	// close any files, etc.
	//////////////////////////////////////////////////////////////////////
	@Override
	protected void cleanupSim() {

		this.network.afterSim();
		double now = SimulationTimer.getTime();
		for (Vehicle veh : teleportationList) {
			new EventAgentStuck(now, veh.getDriverID(), veh.getCurrentLegNumber(),
					veh.getCurrentLink().getId().toString(), veh.getDriver(),
					veh.getCurrentLeg(), veh.getCurrentLink());
		}
		teleportationList.clear();

		if (this.myeventwriter != null) this.myeventwriter.reset(0);
		for (SnapshotWriterI writer : this.snapshotWriters) {
			writer.finish();
		}

		if (this.netStateWriter != null) {
			try {
				this.netStateWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.netStateWriter = null;
		}

	}

	/**
	 * Do one step of the simulation run.
	 *
	 * @return true if the simulation needs to continue
	 */
	@Override
	public boolean doSimStep(final double time) {
		this.moveVehiclesWithUnknownLegMode(time);
		this.network.simStep(time);

		if (time % INFO_PERIOD == 0) {
			Date endtime = new Date();
			long diffreal = (endtime.getTime() - this.starttime.getTime())/1000;
			double diffsim  = time - SimulationTimer.getSimStartTime();
			int nofActiveLinks = this.network.getSimulatedLinks().size();
			log.info("SIMULATION AT " + Time.writeTime(time) + ": #Veh=" + getLiving() + " lost=" + getLost() + " #links=" + nofActiveLinks
					+ " simT=" + diffsim + "s realT=" + (diffreal) + "s; (s/r): " + (diffsim/(diffreal + Double.MIN_VALUE)));
			Gbl.printMemoryUsage();
		}

		return isLiving() && (this.stopTime >= time);
	}

	@Override
	public void afterSimStep(final double time) {
		if (time % this.snapshotPeriod == 0) {
			doSnapshot(time);
		}
	}


	private void doSnapshot(final double time) {
		if (!this.snapshotWriters.isEmpty()) {
			Collection<PositionInfo> positions = this.network.getVehiclePositions();
			for (SnapshotWriterI writer : this.snapshotWriters) {
				writer.beginSnapshot(time);
				for (PositionInfo position : positions) {
					writer.addAgent(position);
				}
				writer.endSnapshot();
			}
		}

		if (this.netStateWriter != null) {
			try {
				this.netStateWriter.dump((int)time);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static final Events getEvents() {
		return events;
	}

	private static final void setEvents(final Events events) {
		QueueSimulation.events = events;
	}

	public static final void handleUnknownLegMode(final Vehicle veh) {
		veh.setDepartureTime_s(SimulationTimer.getTime() + veh.getCurrentLeg().getTravTime());
		veh.setCurrentLink(veh.getDestinationLink());
		teleportationList.add(veh);
	}

	private final void moveVehiclesWithUnknownLegMode(final double now) {
	  	while (teleportationList.peek() != null ) {
	  		Vehicle veh = teleportationList.peek();
	  		if (veh.getDepartureTime_s() <= now) {
	  			teleportationList.poll();

				getEvents().processEvent(new EventAgentArrival(now, veh.getDriverID(), veh.getCurrentLegNumber(),
						veh.getCurrentLink().getId().toString(), veh.getDriver(), veh.getCurrentLeg(), veh.getCurrentLink()));
	  			veh.reachActivity(now);

	  		} else break;
  		}
	}

	public boolean addSnapshotWriter(final SnapshotWriterI writer) {
		return this.snapshotWriters.add(writer);
	}

	public boolean removeSnapshotWriter(final SnapshotWriterI writer) {
		return this.snapshotWriters.remove(writer);
	}

	public void setVehicleCreateAlgo(final PersonAlgo_CreateVehicle algo) {
		this.veh_algo = algo;
	}
}
