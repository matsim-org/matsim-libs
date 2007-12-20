/* *********************************************************************** *
 * project: org.matsim.*
 * OTFNetEventFileHandler.java
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

package playground.david.vis;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.snapshots.PositionInfo;
import org.matsim.mobsim.snapshots.SnapshotWriterI;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.plans.Plan;
import org.matsim.run.Events2Snapshot;
import org.matsim.utils.vis.netvis.streaming.SimStateWriterI;
import org.matsim.world.World;

import playground.david.vis.data.OTFNetWriterFactory;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.interfaces.OTFAgentHandler;
import playground.david.vis.interfaces.OTFNetHandler;
import playground.david.vis.interfaces.OTFServerRemote;

public class OTFNetEventFileHandler implements Serializable, SimStateWriterI, OTFServerRemote, SnapshotWriterI{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	/*
	 * OTFEventVisNet has a changed writeAgent Method. We collect the agents
	 * while reading in the eventsfile and store them in a map, so we can dump them out at
	 * once. it is not necessary nor feasible in this case to collect them over iterating over the links
	 */
	public static class OTFEventVisNet extends OTFVisNet {

		//public Map<String, PositionInfo> positions = new HashMap<String, PositionInfo>();

		public OTFEventVisNet(QueueNetworkLayer source) {
			super(source);
		}

	    @Override
		public void writeAgents(DataOutputStream out) throws IOException {
			OTFAgentHandler<PositionInfo> agentHandler = handler.getAgentHandler();
			agentHandler.setOTFNet(this);

			if (positions.size() == 0) return;

			out.writeInt(positions.size());

			for (PositionInfo pos : positions) {
				agentHandler.writeAgent(pos, out);
	    }
	  }
	}

	private FileOutputStream outStream = null;
	private DataOutputStream outFile;
	private final String fileName;

	private OTFEventVisNet net = null;
	QueueNetworkLayer qnetwork;
	public ByteArrayOutputStream out = null;
	double nextTime = 0;
	double intervall_s = 1;
	Map<Double,Long> timeSteps = new HashMap<Double,Long>();

	public OTFNetEventFileHandler(double intervall_s, QueueNetworkLayer network, String fileName) {
		if (network != null) net = new OTFEventVisNet(network);
		out = new ByteArrayOutputStream(5000000);
		this.intervall_s = intervall_s;
		this.fileName = fileName;
		qnetwork = network;
	}

	public void run(String eventFileName) {
		open();
		// create SnapshotGenerator
		Gbl.startMeasurement();
		Events2Snapshot app = new Events2Snapshot();
		app.addExternalSnapshotWriter(this);
		app.run(new File(eventFileName), Gbl.getConfig(), qnetwork);
		try {
			close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void close() throws IOException {
		try {
			outFile.close();
			outStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean dump(int time_s) throws IOException {
		if (time_s >= nextTime) {
			// dump time
			System.out.print(" Time spend on event to snap ");
			Gbl.printElapsedTime();
			Gbl.startMeasurement();
			outFile.writeDouble(time_s);
			// get State
			//Gbl.startMeasurement();
			out.reset();
			net.writeMyself(null, new DataOutputStream(out));
			outFile.writeInt(out.size());
			out.writeTo(outFile);
			System.out.print(" Time spend on snap to vis ");
			Gbl.printElapsedTime();
			Gbl.startMeasurement();
			// dump State
			//Gbl.printElapsedTime();

			nextTime = time_s + intervall_s;
			return true;
		}
		return false;
	}

	public void open() {
		// open file
		try {
			outStream = new FileOutputStream(fileName);
//			outFile = new DataOutputStream(new BufferedOutputStream(outStream, 100000000));
			outFile = new DataOutputStream(outStream);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// dump some infos
		try {
			outFile.writeDouble(intervall_s);
			new ObjectOutputStream(outStream).writeObject(net);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// dump the network out
	}



	// IN
	private FileInputStream inStream = null;
	private DataInputStream inFile;

	public void readNet() {
		// open file
		try {
			inStream = new FileInputStream(fileName);
			inFile = new DataInputStream(inStream);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// dump some infos
		try {
			intervall_s = inFile.readDouble();
			net = (OTFEventVisNet) new ObjectInputStream(inStream).readObject();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// dump the network out
 catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void hasNextTimeStep() {

	}

	public void getNextTimeStep() {

	}

	public Plan getAgentPlan(String id) throws RemoteException {
		return null;
	}

	public int getLocalTime() throws RemoteException {
		return (int)nextTime;
	}

	public OTFVisNet getNet(OTFNetHandler handler) throws RemoteException {
		if (net == null) readNet();

		return net;
	}

	long filepos = 0;
	public byte[] getStateBuffer() throws RemoteException {
		int size =  0 ;
		byte [] result = null;

		try {
			nextTime = inFile.readDouble();
			size = inFile.readInt();

			result = new byte[size];
			int read = inFile.read(result);

			if (read != size) throw new IOException("READ SIZE did not fit! File corrupted!");
			timeSteps.put(nextTime, filepos);
			filepos += read;

		} catch (IOException e) {
			System.out.println(e.toString());
		}

		return result;
	}

	public void pause() throws RemoteException {
	}

	public void play() throws RemoteException {
	}

	public void setStatus(int status) throws RemoteException {
	}

	public void step() throws RemoteException {
	}


	double actualTime = 0;
	public void beginSnapshot(double time) {
		net.positions.clear();
		actualTime = time;
	}

	public void addAgent(PositionInfo position) {
		net.positions.add(position);
	}

	public void endSnapshot() {
		try {
			dump((int)actualTime);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void finish() {

	}
	public static void main(String[] args) {
		if ( args.length==0 )
			args = new String[] {"./test/dstrippgen/myconfig.xml"};

		Gbl.createConfig(args);
		Gbl.startMeasurement();
		World world = Gbl.createWorld();

		String netFileName = Gbl.getConfig().getParam("network","inputNetworkFile");
		QueueNetworkLayer net = new QueueNetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);
		world.setNetworkLayer(net);


		String eventFile = Gbl.getConfig().getParam("events","outputFile");
		eventFile = "output/current/ITERS/it.0/0.events.txt.gz";
		eventFile = "../../tmp/studies/berlin-wip/run125/200.events.txt.gz";
		OTFNetEventFileHandler test = new OTFNetEventFileHandler(10, net,"output/ds_fromEvent.vis" );
		test.run(eventFile);
	}

	public OTFServerQuad getQuad(OTFNetWriterFactory writers)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	public byte[] getQuadConstStateBuffer() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	public byte[] getQuadDynStateBuffer() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
}
