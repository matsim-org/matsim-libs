/* *********************************************************************** *
 * project: org.matsim.*
 * DistributedQueueSimulation.java
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.matsim.events.BasicEvent;
import org.matsim.events.Events;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.mobsim.Vehicle;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plans;
import org.matsim.utils.misc.Time;

public class DistributedQueueSimulation extends QueueSimulation implements DistributedSimulationRemoteI {

	private static final String SIMULATION = "simulation";
	private static final String SHELLTYPE = "shellType";
	private static final String JAVACLASSPATH = "classPath";
	private static final String JVMOPTIONS = "JVMOptions";
	private static final String CLIENTLIST = "clientList";
	private static final String LOCALCONFIG = "localConfig";
	private static final String LOCALCONFIGDTD = "localConfigDTD";

	private final Object simStepStart = new Object();
	private final Object simStepDone = new Object();
	private int count = 0, accu = 0;
	private static Map<Integer, Remote> partSims = new TreeMap<Integer, Remote>();
	private static Map<Integer, String> partSimsIP = new TreeMap<Integer, String>();
	private static ArrayList<Integer> partitionTable = null;
	private static ArrayList<Process> processes = new ArrayList<Process>();

	public DistributedQueueSimulation(QueueNetworkLayer network, Plans plans, Events events) {
		super(network, plans, events);
	}

	public static void sendVehicle(int partId, String driverID, List actLegs) {
		PartialSimulationRemoteI proc = (PartialSimulationRemoteI)partSims.get(partId);
		try {
			proc.createVehicle(driverID, actLegs);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	// when all n PartialSims have called this the amin DistSim is released from below method
	public void signalSimStepDone(int partID, int partLiving) throws RemoteException {

//		System.out.println("SimStep" + " done from #" + partID + " " + accu + " of " + count);
		synchronized(this.simStepStart) {
			this.accu++;
			incLiving(partLiving);
			if (this.accu >= this.count)
			{
				// signal, that all partsims have called back, for not running into wait in case
				// all parsim were faster than MainSim... should only happen while debugging!
	//			System.out.println("SimStep completed" + " done from #" + partID + " " + accu + " of " + count);
					synchronized(this.simStepDone) {
					this.simStepDone.notifyAll();
					}

			}
	//					System.out.println("SimStep done from ALL notify Main");
			try {
				this.simStepStart.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// main DistSim waits here until all PartialSims signaled SimstepDone
	public int waitSimStepDone() {
		int result = 0;
//		System.out.println("ENTERING SimStepDone from Main");
		synchronized(this.simStepDone) {

			while (this.accu < this.count) { // accu == count all partsims have already returned
				  try {
					  this.simStepDone.wait();
				  } catch ( InterruptedException e ) {
					  System.out.println("*LEAVING SimStepDone with Exception");
				  }
			  }
			  this.accu = 0;
			  result = getLiving();
			  setLiving(0);
			  this.count = partSims.size();
//			  System.out.println("*LEAVING SimStepDone from Main"+count);
		}
		return result;
	}

	// Static member of DistributedQueueSimulation
	private static Registry reg = null;
	private static boolean haveCreatedRegistry = false;
	public static void  registerWithRMI(String name, Remote test) {
		try {
			if(reg == null) {
				reg = LocateRegistry.getRegistry( Registry.REGISTRY_PORT );
				try {
					@SuppressWarnings("unused")
					String[] liste = Naming.list("localhost");
					liste.getClass();
				} catch (RemoteException e) {
					reg = LocateRegistry.createRegistry( Registry.REGISTRY_PORT );
					haveCreatedRegistry = true;
				}
			}

			UnicastRemoteObject.exportObject(test,0);
			Naming.rebind(name, test);
//			Gbl.debugMsg(0,test.getClass(),"RMI connect to Link " + name + "]");
			System.out.println("*Register : " + name + test.toString());
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public static void unregisterWithRMI(String name, Remote test) {
		try {
			System.out.println("*UNRegister : " + name + test.toString());
			Naming.unbind(name);
			UnicastRemoteObject.unexportObject(test, true);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}

	}
	static Object getObjectOverMRI(String hostname, String name) {
		Object ret = null;
		try {
//			String[] liste = Naming.list("localhost");
//			int i = 0;
			ret  = Naming.lookup("rmi://" + hostname + "/" + name);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		return ret;
	}

	synchronized protected void signalNextSimStep() {
		synchronized(this.simStepStart) {
			this.simStepStart.notifyAll();
		}
	}

	protected boolean doSimStep(int time)
	{
		int living = 0;
		signalNextSimStep();
//		System.out.println("Simstep done by MainSim " +  time + " " + count);
		living = waitSimStepDone();

		if(time % INFO_PERIOD == 0)
		{
			Date endtime = new Date();
			long diffreal = (endtime.getTime() - this.starttime.getTime())/1000;
			double diffsim = time - SimulationTimer.getSimStartTime();
			System.out.println(Time.writeTime(time) + ": # act. Veh= " + living + " lost: " + getLost() + " + delta: simTime: " + diffsim + "s took realTime: " + (diffreal) + "s; ratio(s/r): " + (diffsim/(diffreal+0.0000001)));
		}

		return living != 0 ;
	}

	@Override
	protected void prepareSim()
	{
		if (events == null)  {
			events = new Events();
			this.myeventwriter = new EventWriterTXT("MatSimJEvents.txt");
			events.addHandler(this.myeventwriter);
		}
	}

	@Override
	protected void cleanupSim()
	{
		if (this.myeventwriter != null )this.myeventwriter.reset(0);
	}

	static DistributedQueueSimulation testRMI;
	class ProcHandler {
		String procname;
		Process proc;

	}

	/**
	 * in arg[0] we find which shell to use e.g. "rsh" or "ssh"
	 * in arg[1] we find the classpath to mobsim e.g. "e:/Development/workspace/matsimJ-HEAD/bin"
	 * in arg[2] etc the ip adresses of the client hosts
	 * @param args
	 * @throws UnknownHostException
	 * @throws RemoteException
	 */
	public static void main(String[] args) throws UnknownHostException, RemoteException {
		Gbl.createConfig(args);
		Gbl.createWorld();

	    testRMI = new DistributedQueueSimulation(null, null, null);
		registerWithRMI("Dsim", testRMI);

	     Runtime run = Runtime.getRuntime();
	     run.addShutdownHook( new Thread()
	     {
	    	 @Override
				public void run()
	    	 {
	    		 System.out.println( "Program cancelled by Ctrl-C, flushing files!" );
	    		 Date endtime = new Date();
	    		 System.out.println("simulation Time: " + ((endtime.getTime() - testRMI.starttime.getTime())/1000));
	    		 testRMI.cleanupSim();

	    		 for (Object s : DistributedQueueSimulation.partSims.values()) {
	    			 PartialSimulationRemoteI proc = (PartialSimulationRemoteI)s;
	    			 System.out.println("destroying process: " + proc);
	    			 try {
	    				 proc.exit();
	    			 } catch (RemoteException e) {
	    				 e.printStackTrace();
	    			 }
	    		 }
	    		 for (Process proc : processes) {
	    			 System.out.println("destroying process: " + proc);
	    			 proc.destroy();
	    		 }
	    	 }
	     } );

//		QueueRemoteLink link1 = new QueueRemoteLink(null, "4", null, null, hostname, hostname, hostname, hostname, hostname, hostname);
//		link1.finishInit();

		InetAddress me = InetAddress.getLocalHost();

		String shellpath = Gbl.getConfig().getParam(SIMULATION, SHELLTYPE);
		String classpath = Gbl.getConfig().getParam(SIMULATION, JAVACLASSPATH);;
		String JavaVMOptions = Gbl.getConfig().getParam(SIMULATION, JVMOPTIONS);
		String clientIPs = 	Gbl.getConfig().getParam(SIMULATION, CLIENTLIST);
		String localConfig = Gbl.getConfig().getParam(SIMULATION, LOCALCONFIG);
		String localDTD = Gbl.getConfig().getParam(SIMULATION, LOCALCONFIGDTD);

		StringTokenizer tokenizer = new StringTokenizer( clientIPs );

		while ( tokenizer.hasMoreTokens() ) {
			try {
				String ipadress = tokenizer.nextToken();
				DistributedQueueSimulation.partSimsIP.put(testRMI.count, ipadress);
				String command =  shellpath + " " + ipadress + " java " + JavaVMOptions + " -classpath \"" + classpath + "\" org.matsim.demandmodeling.mobsim.distributed.PartialQueueSimulation ";
				command += testRMI.count++ + " " + me.getHostAddress()+ " Dsim " + localConfig + " " + localDTD;
				System.out.println(command);
				Process p = Runtime.getRuntime().exec(command);
				processes.add(p);
//			    BufferedReader in = new BufferedReader( new InputStreamReader(p.getInputStream()) );
//		   	    for ( String s; (s = in.readLine()) != null; )     System.out.println( s );
//		   	    in.close();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

//		// just FOR TESTING additionla partSim must be started by hand!
//		testRMI.partSimsIP.put(testRMI.count, "192.168.35.73");
//		testRMI.count++;

//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e1) {
//			e1.printStackTrace();
//		}
//		synchronized(testRMI) {
//			if ( testRMI.accu < testRMI.count)
//				try {
//					testRMI.simStepDone.wait(2000);
//				} catch (InterruptedException e1) {
//					e1.printStackTrace();
//				}
//			testRMI.count = testRMI.partSims.size();
//			testRMI.accu = 0;
//		}
//
		testRMI.waitSimStepDone();  // WAIT FOR Signal A: Ready and WAITING for sim to connect to others
		System.out.println("wait in main" + testRMI.accu +" done by MainSim");
		testRMI.signalNextSimStep(); // RELEASE Signal A
		testRMI.waitSimStepDone();  // WAIT FOR Signal B: Network read

		testRMI.prepareNetwork();

		testRMI.signalNextSimStep(); // RELEASE Signal B
		testRMI.waitSimStepDone();  // WAIT FOR Signal C: Registered RMI links
		System.out.println("wait in main" + testRMI.accu +" done by MainSim");

		testRMI.prepareSim(); // we need EVENTS HERE already
		PersonAlgo_CreateVehicleDistributed algo = new PersonAlgo_CreateVehicleDistributed();
		testRMI.setVehicleCreateAlgo(algo);
		testRMI.createAgents();

		testRMI.signalNextSimStep(); // RELEASE Signal C
		testRMI.waitSimStepDone();  // WAIT FOR Signal D: Ready to GO

//		RemoteLinkI link = (RemoteLinkI)getObjectOverMRI(test[0],"RLink3");
//		Vehicle v = new Vehicle();
//		v.setCurrentDepartureTime(45);
//		v.setSpeed(45.5);
//		link.add(v);
		testRMI.starttime = new Date();
		SimulationTimer.setTime(SimulationTimer.getSimStartTime());
		testRMI.run();
		Date endtime = new Date();
		System.out.println("simulation Time: " + ((endtime.getTime() - testRMI.starttime.getTime()) / 1000));
		System.out.println("simulation Time: "
				+ Time.writeTime(((int) ((endtime.getTime() - testRMI.starttime.getTime()) / 1000))));

		for (Object s : DistributedQueueSimulation.partSims.values()) {
			PartialSimulationRemoteI proc = (PartialSimulationRemoteI) s;
			System.out.println("destroying process: " + proc);
			try {
				proc.exit();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Simstep sending last nextsimstep done ");
		testRMI.signalNextSimStep();
		testRMI.waitSimStepDone(); // WAIT FOR Signal B: Ready and WAITING for sim
																// to start
		System.out.println("Simstep done by MainSim");

		testRMI.signalNextSimStep();
		unregisterWithRMI("Dsim", testRMI);
		// unregisterWithRMI("RLink4", link1);
		try {
			String[] liste = Naming.list("localhost");
			for( String li : liste)
			System.out.println("FORGOTTEN" + li);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		if (haveCreatedRegistry) UnicastRemoteObject.unexportObject(reg, true);
	}

	synchronized public void incActivePartSims(int partId) throws RemoteException {
		try {
			String rmilookup = "rmi://" + partSimsIP.get(partId) + ":/PSim"+ partId;
			Remote server = Naming.lookup(rmilookup);
			System.out.println("added "+rmilookup + "done by MainSim" +server + partSims.size());
			System.out.println("added "+partId + "done by MainSim" + partSims.size());
			partSims.put(partId, server); //TODO DS Something more useful as Object!
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
	}

	synchronized public void decActivePartSims(int partId) throws RemoteException {
		System.out.println("remove "+partId + "done by MainSim" + partSims.size());
		partSims.remove(partId);
	}

	public void add(Vehicle veh) throws RemoteException {
		// TODO Auto-generated method stub
	}

	public Map<Integer, String> getPartIDIP() throws RemoteException {
		return partSimsIP;
	}

	public void setLocalInitialTimeStep(int time) throws RemoteException {
		SimulationTimer.updateSimStartTime(time);
		System.out.println("SimStartTime set to min "+time + " secs");
	}

	public int getGlobalInitialTimeStep() throws RemoteException {
		return (int)SimulationTimer.getSimStartTime();
	}

	@Override
	protected void prepareNetwork() {
		// DS TODO somebody made network FINAL therefor i had to add a local network here
		// this breaks functionality ... repair if needed
		QueueNetworkLayer network = (QueueNetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());

		try {
			partitionTable = MetisExeWrapper.decomposeNetwork(network, partSimsIP.size(), "");
		} catch (IOException e) {
			e.printStackTrace();
		}
		network.beforeSim();
	}

	public ArrayList<Integer> getPartitionTable() throws RemoteException {
		return partitionTable;
	}

	synchronized public void add(BasicEvent event) throws RemoteException {
		// rebuild missing references to person/link/leg
		try {
			event.rebuild(this.plans, this.network);
		} catch (NullPointerException e )
		{
		}
		// process event as usual
		events.processEvent(event);
	}

	private LinkedList<BasicEvent> pendingEvents = new LinkedList<BasicEvent>();
	private int lastTimes[] = new int[512];
	// Note: returns true if all lastTimes are equal, OR if only one partition is there
	private boolean checkForEqualLastTimes()
	{
		for(int i= 0; i < partSims.size()-1 ; i++) {
			if (this.lastTimes[i] != this.lastTimes[i+1])return false;
		}
		return true;
	}

	synchronized public void addEventList(LinkedList<BasicEvent> eventlist, int lastTime, int partID) throws RemoteException {
		BasicEvent event = null;

		// Sort all event into pendingEvents List. Update lastTime[partID]
		ListIterator<BasicEvent> newIt = eventlist.listIterator(eventlist.size());
		ListIterator<BasicEvent> oldIt = this.pendingEvents.listIterator(this.pendingEvents.size());

		while (newIt.hasPrevious()) {
			event = newIt.previous();
			while (oldIt.hasPrevious() && (oldIt.previous().time > event.time));
			oldIt.add (event);
		}
		this.lastTimes[partID] = lastTime;

		// every time, all lastTimes have catched up it is safe to dump the whole
		// eventslist
//		int size1 = pendingEvents.size();
		boolean hasEqualLastTimes = checkForEqualLastTimes();
		if (hasEqualLastTimes ){
			oldIt = this.pendingEvents.listIterator();
			while (oldIt.hasNext())
			{
				event = oldIt.next();
//				if (event.time <= minLastTime)
//				{
					oldIt.remove();
					// rebuild missing references to person/link/leg
					try {
						event.rebuild(this.plans, this.network);
					} catch (NullPointerException e )
					{
					}
					// process event as usual
					events.processEvent(event);
//				} ;//else break; // we do not need to look any further
			}
		}
		//System.out.println("PendingeEvents size before after diff " + size1 + ", " + pendingEvents.size() + ", " + (pendingEvents.size() - size1));
	}
}

