/* *********************************************************************** *
 * project: org.matsim.*
 * PartialQueueSimulation.java
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

package playground.david.mobsim.distributed;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.config.Config;
import org.matsim.events.BasicEvent;
import org.matsim.events.Events;
import org.matsim.events.handler.BasicEventHandlerI;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueNode;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.mobsim.Vehicle;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Plans;


public class PartialQueueSimulation extends QueueSimulation implements
		PartialSimulationRemoteI {

	class PartialEventsHandler implements BasicEventHandlerI {

		private final LinkedList<BasicEvent> eventList = new LinkedList<BasicEvent>();
		//private EventWriterTXT txtwriter = new EventWriterTXT("PartSimEvents"+ partID);
		public void catchEvent (final int time, final BasicEvent event)
		{
		}

		public void handleEvent(final BasicEvent event) {
			// if size not reached just stuff it at the end of out list
			if (event != null) this.eventList.add(event);
			//txtwriter.handleEvent(event);
		}

		public void reset(final int iteration) {
			try {
				PartialQueueSimulation.this.host.addEventList(this.eventList, Integer.MAX_VALUE, PartialQueueSimulation.this.partID);
			} catch (RemoteException e) {
				e.printStackTrace();
				java.io.OptionalDataException exi = (java.io.OptionalDataException)e.getCause();
				System.out.println("OptionalDataEx says: length = " + exi.length + "and eof=" + exi.eof);
				//txtwriter.closefile();
				System.exit(1);
			}
			this.eventList.clear();
		}
		public void purgeEvents(final int now)
		{
			try {
				PartialQueueSimulation.this.host.addEventList(this.eventList, now, PartialQueueSimulation.this.partID);
			} catch (Exception e) {
				e.printStackTrace();
				java.io.OptionalDataException exi = (java.io.OptionalDataException)(e.getCause().getCause());
				System.out.println("OptionalDataEx says: length = " + exi.length + "and eof=" + exi.eof);
			}
			this.eventList.clear();
		}
	}

	DistributedSimulationRemoteI host;
	private String simname;
	private int partID;
	private Map<Integer, String> partSimsIP;
	private boolean exitNow = false;
	// Is main server calling metis, or should PartailServer do it on its own?
	private final boolean doLocalDecomp = false;
	private PartialEventsHandler eventhandler = null;


	public PartialQueueSimulation(final QueueNetworkLayer network, final Plans plans, final Events events) {
		super(network, plans, events);
	}

	public void initPartition(final int ID, final String hostname) throws RemoteException {
		try {
			this.partID = ID;
			this.host = (DistributedSimulationRemoteI)Naming.lookup("rmi://" + hostname + "/" + this.simname);
			// Initialize Vehicle Events
			events = new Events();
			this.eventhandler = new PartialEventsHandler();
			events.addHandler(this.eventhandler);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
	}

	int step = 0;
	protected boolean doSimStep(final int time)
	{
		// do Sim stuff!
		super.doSimStep(time);
		//System.out.println("Living veh:" + getLiving());
		try {
			this.host.signalSimStepDone(this.partID, getLiving());
			if (time % 1800 == 0 ) this.eventhandler.purgeEvents(time);
		} catch (RemoteException e) {
			e.printStackTrace();
			super.cleanupSim();
			System.out.println("Simulant #" + this.partID + " stopped by Connection RESET!");
			System.exit(1);
		}
		return this.exitNow == false;
	}

	@Override
	protected void prepareSim()
	{
		// Read the plan files
//		readPlans();
		// Parallel:: shrink the network
		// Initialize Snapshot file
//		String snapshotFile = Controler.getIterationFilename("T.veh");
//		TransimsSnapshotWriter.init(snapshotFile, true);
		// TODO [DS] define start time param and simulation module in dtd
//		String startTime = Config.getSingleton().getParam("simulation","startTime");
		SimulationTimer.updateSimStartTime(23*3600);
		SimulationTimer.setTime(SimulationTimer.getSimStartTime());
	}

	@Override
	protected void cleanupSim()
	{
		super.cleanupSim();
		prepareLinks(false);
	}

	public static void redirect(final File directory) {
		  try {
		    File tempFile = File.createTempFile(
		          "stderr", "", directory);
		    System.setErr(
		        new PrintStream(new FileOutputStream(tempFile)));
		    tempFile = File.createTempFile("stdout",
		                 "", directory);
		    System.setOut(
		        new PrintStream(new FileOutputStream(tempFile)));
		      }
		    catch (Throwable t) {
		      System.err.println(
		        "Error overriding standard output to file.");
		      t.printStackTrace(System.err);
		      }
		  }

	// TODO [DS] I don't think there should be a main method in this class.
	// Tests should be done as external test-cases / marcel
	public static void main(final String[] args) {
		redirect (new File("/home/dstrippgen/tmp"));
		System.out.println("Simulant #" + args[0] + " started");
		Gbl.startMeasurement();
		String[] args2 = {args[3], args[4]};
		Config config = Gbl.createConfig(args2);
		Gbl.createWorld();

		try {
			QueueNetworkLayer network = (QueueNetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);

			PartialQueueSimulation pq = new PartialQueueSimulation(network, null, null);
			DistributedQueueSimulation.registerWithRMI("PSim"+ args[0], pq);
			pq.simname =  args[2];
			pq.initPartition(Integer.parseInt(args[0]), args[1]);
			pq.host.incActivePartSims(pq.partID);
			pq.host.signalSimStepDone(pq.partID,1); // SEND Signal A: Ready and WAITING to connect ot others
			pq.partSimsIP = pq.host.getPartIDIP();

			new MatsimNetworkReader(pq.network).readFile(config.network().getInputFile());
			pq.host.signalSimStepDone(pq.partID,1); // SEND Signal B: Network read
			pq.prepareNetwork();
			pq.host.signalSimStepDone(pq.partID,1); // SEND Signal C: RemoteLinks registered
			pq.connectNetwork();
			if (pq.doLocalDecomp) {
				PersonAlgo_CreateVehiclePartial algo = new PersonAlgo_CreateVehiclePartial();
				algo.myID = pq.partID;
				pq.setVehicleCreateAlgo(algo);
				pq.createAgents();
			}
			pq.compactNetwork();
			// host set my initial timestep
			pq.host.setLocalInitialTimeStep((int)SimulationTimer.getSimStartTime());
			pq.host.signalSimStepDone(pq.partID,1); // SEND Signal D: Connected and ready to GO
			// host get global initial timestep
			SimulationTimer.updateSimStartTime(pq.host.getGlobalInitialTimeStep());
			SimulationTimer.setTime(SimulationTimer.getSimStartTime());
			System.out.println("starttime on client" + SimulationTimer.getSimStartTime());
			pq.starttime = new Date();


			pq.run();

			events.resetHandlers(0);  // send last events, flushing eventsbuffer
			pq.host.decActivePartSims(pq.partID);
			DistributedQueueSimulation.unregisterWithRMI("PSim"+ args[0],pq);
			pq.host.signalSimStepDone(pq.partID,1); // SEND Signal C: FINAL end and out
			pq.cleanupSim();
		} catch (RemoteException e) {
			e.printStackTrace();
			//pq.cleanupSim();
			System.exit(1);
		}
	}

	private void connectNetwork() {
		for (QueueLink link : this.network.getLinks().values()) {
			int fromID = ((QueueNode) link.getFromNode()).getPartitionId();
			int toID = ((QueueNode) link.getToNode()).getPartitionId();

			if ((fromID != toID) && (fromID == this.partID)) {
				// this link goes out to another CPU
				((QueueRemoteLink) link).connectToRemoteLink(this.partSimsIP.get(toID));
			}
		}
	}

	private QueueRemoteLink replaceLink(final QueueLink linkold) {
		Id key = linkold.getId();
		QueueRemoteLink linknew = new QueueRemoteLink(linkold, this.network);
		Node from = linkold.getFromNode();
		if(from.getOutLinks().containsKey(key)){
			System.out.println("outl key exits "+key.toString());
			from.removeOutLink(linkold);
		}
		from.addOutLink(linknew);
		Node to = linkold.getToNode();
		if (to.getInLinks().containsKey(key)){
			System.out.println("inl key exits "+key.toString());
			to.removeInLink(linkold);
		}
		to.addInLink(linknew);

		return linknew;
	}

	private void prepareLinks(final boolean startup) {
		int count = 0;
		int linkcount = 0;
		int fromID = 0, toID = 0;
		// TreeMap remoteLinks = new TreeMap();
		for (QueueLink link : this.network.getLinks().values()) {
			linkcount++;
			fromID = ((QueueNode) link.getFromNode()).getPartitionId();
			toID = ((QueueNode) link.getToNode()).getPartitionId();

			if ((fromID != this.partID) && (toID != this.partID)) {
				// both nodes are not on my partition, remove link
			} else if (fromID != toID) {
				count++;
				if (toID == this.partID) {
					// that link needs to receive messages fomr other CPUs
					if (startup) {
						// exchange Normal QueueLink with remote
						QueueRemoteLink link2 = replaceLink(link);
						((Map<Id, QueueLink>) this.network.getLinks()).put(link2.getId(), link2);
						link2.initRemoteVisibility(); // open RMI interface to talk to other
																					// partition
					} else
						((QueueRemoteLink) link).exitRemoteVisibility();
				} else if ((fromID == this.partID) && startup) {
					// this link goes out to another CPU
					// exchange Normal QueueLink with remote
					QueueRemoteLink link2 = replaceLink(link);
					((Map<Id, QueueLink>) this.network.getLinks()).put(link2.getId(), link2);
					// gets connected in connectNetwork()
				}
			}
		}
	}

	private void compactLinks() {
		int fromID = 0, toID = 0;
		int size = this.network.getSimulatedLinks().size();
		Iterator<QueueLink> l_it = this.network.getSimulatedLinks().iterator();

		while (l_it.hasNext()) {
			QueueLink link = l_it.next();
			fromID = ((QueueNode) link.getFromNode()).getPartitionId();
			toID = ((QueueNode) link.getToNode()).getPartitionId();

			if ((fromID != this.partID) && (toID != this.partID)) {
				// both nodes are not on my partition, remove link
				l_it.remove();
			}
		}

		System.out.println("Link size reduced from " + size + " to " + this.network.getSimulatedLinks().size() + " that is "
				+ (size - this.network.getSimulatedLinks().size()) + " less");
	}

	private void compactNodes() {
		int size = this.network.getSimulatedNodes().size();
		Iterator<QueueNode> n_it = this.network.getSimulatedNodes().iterator();
		while (n_it.hasNext()) {
			QueueNode node = n_it.next();
			if (node.getPartitionId() != this.partID) {
				// both nodes are not on my partition, remove link
				n_it.remove();
			}
		}
		System.out.println("Node size reduced from " + size + " to " + this.network.getSimulatedNodes().size() + " that is "
				+ (size - this.network.getSimulatedNodes().size()) + " less");
	}

	private void compactNetwork() {
		compactLinks();
		compactNodes();
	}

	public boolean isActive() throws RemoteException {
		return isLiving();
	}

	@Override
	protected void prepareNetwork() {
		// call metis for decomposition
		try {
			if (this.doLocalDecomp) MetisExeWrapper.decomposeNetwork(this.network, this.partSimsIP.size(), Integer.toString(this.partID));
			else {
				ArrayList<Integer> table = this.host.getPartitionTable();
				int count = 0;
				for (QueueNode node : this.network.getNodes().values()) {
					node.setPartitionId(table.get(count++).intValue());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		// after decomposing, make RemoteLinks for all links on MY boundary
		//remove all links/nodes that are not on my partition
		// RMI release all INCOMING RemoteLinks
		// SYNCH with others, to make sure all remote links have been constructed
		// Outgoing RemoteLink could now be RMI connected with their local counterpart
		// Synch again

		prepareLinks(true);
		this.network.beforeSim();
	}

	public void exit() throws RemoteException {

		this.exitNow = true;
	}

	public void createVehicle(final String driverID, final List actLegs) throws RemoteException {
		Vehicle veh = new Vehicle();
		veh.setActLegs(actLegs);
		veh.setDriverID(driverID);
		veh.initVeh();
		//int test = veh.getCurrentLink().getID();
		//QueueSimulation.incLiving();
		//Route route = ((Leg)actLegs.get(1)).getRoute();
		//for (Node n : route.getRoute()) {
		//	System.out.print(n.getID() + ",");
		//}

		//System.out.println("at link " + actLegs.get(0).toString() + "veh createLiving veh on part"+this.partID + ":" + getLiving());
	}

}
