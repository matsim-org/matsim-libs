/* *********************************************************************** *
 * project: org.matsim.*
 * ExeViaListener.java
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

package playground.david.otfivs.executables;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.QueueNetwork;
import org.matsim.ptproject.qsim.QueueSimulation;
import org.matsim.vis.otfvis.OTFClientFile;
import org.matsim.vis.otfvis.executables.OTFVisController;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.server.OnTheFlyServer;


public class ExeViaListener {
	public static class SimCanceledException extends RuntimeException{
		
	}

	public static class ResetableMobsimControler extends Controler{
		protected boolean useResetable = true;
		
		public static class MyEventsManagerImpl extends EventsManagerImpl {
			public List<Event> events = new ArrayList<Event>();

			@Override
			public void processEvent(Event event) {
				events.add(event);
			}
			
		}

		@Override
		protected void loadCoreListeners() {
			this.addCoreControlerListener(new CoreControlerListener());

			// the default handling of plans
			//DSTODO does not compile ->ask for protected in Controler
			//this.plansScoring = new PlansScoring();
			//this.addCoreControlerListener(this.plansScoring);

			this.addCoreControlerListener(new PlansReplanning());
			
			setWriteEventsInterval(0);
		}


		protected void onRunMobsim() {
			super.runMobSim();
		}
		
		@Override
		protected void runMobSim() {
			// just behave like an regular controler
			if(!useResetable){
				onRunMobsim();
				return;
			}
			
			boolean simIsFinished = false;
			EventsManagerImpl events = this.events;
			this.events = new MyEventsManagerImpl();

			Gbl.startMeasurement();
			
			while(!simIsFinished) {
				try {
					
					onRunMobsim();
					simIsFinished = true;
				} catch (SimCanceledException e) {
					System.out.println("Sim canceled... Restarting!");
				}
			}
			System.out.println("Sim run");
			Gbl.printElapsedTime();
			Gbl.startMeasurement();
			System.out.println("Event handling");
			List<Event> myEvents = ((MyEventsManagerImpl)this.events).events;
			for(Event event : myEvents) events.processEvent(event);
			this.events = events;
			Gbl.printElapsedTime();
		}
	
		public ResetableMobsimControler(Config config) {
			super(config);
		}

		public ResetableMobsimControler(String configFileName) {
			super(configFileName);
		}
		
	
		}
	
public static class OTFControlerListener implements 
StartupListener,
BeforeMobsimListener, 
AfterMobsimListener, 
SimulationInitializedListener<QueueSimulation>,
SimulationAfterSimStepListener<QueueSimulation> {

	private QueueNetwork queueNetwork;
	protected OnTheFlyServer otfserver;
	private Population population;
	private EventsManager events;

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		Controler cont = event.getControler();
		otfserver.setControllerStatus(OTFVisController.RUNNING + cont.getIteration());
	}

	public void notifyAfterMobsim(AfterMobsimEvent event) {
		Controler cont = event.getControler();
		otfserver.setControllerStatus(OTFVisController.REPLANNING + cont.getIteration()+1);
	}

	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent<QueueSimulation> e) {
		int status = otfserver.updateStatus(e.getSimulationTime());
		if(otfserver.getRequestStatus() == OTFVisController.CANCEL) {
//			try {
//				myOTFServer.requestControllerStatus(0);
//				myOTFServer.play();
//				myOTFServer.reset();
//			} catch (RemoteException e1) {
				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			} // reset status!
			otfserver.reset();
			throw new SimCanceledException();
		}
	}

	public void notifySimulationInitialized(SimulationInitializedEvent<QueueSimulation> e) {
		QueueSimulation q = e.getQueueSimulation();
		otfserver.events = QueueSimulation.getEvents();
		otfserver.replaceQueueNetwork(q.getQueueNetwork());
	}

	protected void createServer(final UUID idOne, StartupEvent event) {
		this.otfserver = OnTheFlyServer.createInstance("OTFServer_" + idOne.toString(), this.queueNetwork, this.population, this.events, false);
		otfserver.setControllerStatus(OTFVisController.STARTUP);
		try {
			otfserver.pause();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}
	public void notifyStartup(StartupEvent event) {
		UUID idOne = UUID.randomUUID();
		Controler cont = event.getControler();
		this.population = cont.getPopulation();
		this.events = cont.getEvents();
		this.queueNetwork = new QueueNetwork(cont.getNetwork());
		
		createServer(idOne, event);
		// DSTODO take care of Event somewhat later
		// FOR TESTING ONLY!
		startupClient("rmi:127.0.0.1:4019:OTFServer_" + idOne.toString());
	}

	protected void startupClient(String url) {
		OTFClientFile client = new OTFClientFile(url);
//		client.setFilename( Gbl.getConfig().network().getInputFile());
		client.start();

	}
	
}

public static class OTFPopShowListener extends OTFControlerListener implements 
StartupListener,
BeforeMobsimListener, 
AfterMobsimListener, 
SimulationInitializedListener<QueueSimulation>,
SimulationAfterSimStepListener<QueueSimulation> {
	@Override
	protected void startupClient(String url)  {
		OTFVisConfig visconf = new OTFVisConfig();
		if (Gbl.getConfig() == null) Gbl.createConfig(null);
		Gbl.getConfig().addModule(OTFVisConfig.GROUP_NAME, visconf);
// commented as not really needed dg dec 09
//		OTFClientFile client = new OTFVisDualView(url, null, false);
//		client.setFilename( Gbl.getConfig().network().getInputFile());
//		client.start();
//		while(client.getQueryControl() == null) try {Thread.sleep(500);}catch(Exception e){};
//		if(
//				(client.getQueryControl() != null) && 
//				(controlListener instanceof OTFPopShowListener)
//		) {
//			client.getQueryControl().addQueryEntry("Show plan", "Shows the agent's plan on the left side", playground.david.otfvis.prefuse.QueryAgentPlanSyncView.class);
//		}
	}
	
}
	private static OTFControlerListener controlListener;
	// Deletes all files and subdirectories under dir.
    // Returns true if all deletions were successful.
    // If a deletion fails, the method stops attempting to delete and returns false.
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
    
        // The directory is now empty so delete it
        return dir.delete();
    }

    public static void deleteTmp() {
    	deleteDir(new File("tmp_delete_this"));
    }
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String config;
		if (args.length == 1) {
			config = args[0];
		} else {
			args = new String[]{ "../../tmp/studies/ivtch/Diss/config1p.xml"};
		}
		
		Config cfg = Gbl.createConfig(args);
		deleteTmp();
		Gbl.getConfig().controler().setOutputDirectory("tmp_delete_this");

		Controler controler = new ResetableMobsimControler(cfg);
//		controlListener = new OTFControlerListener();
		controlListener = new OTFPopShowListener();
		controler.addControlerListener(controlListener);
		controler.getQueueSimulationListener().add(controlListener);
		controler.run();
	}

}

