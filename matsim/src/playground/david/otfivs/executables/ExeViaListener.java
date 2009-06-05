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

import org.matsim.api.basic.v01.events.BasicEvent;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.corelisteners.RoadPricing;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.Events;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationAfterSimStepEvent;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationInitializedEvent;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationAfterSimStepListener;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationInitializedListener;
import org.matsim.vis.otfvis.executables.OTFVisController;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.gui.PreferencesDialog;
import org.matsim.vis.otfvis.opengl.OnTheFlyClientFileQuad;
import org.matsim.vis.otfvis.opengl.OnTheFlyClientQuad;
import org.matsim.vis.otfvis.opengl.gui.PreferencesDialog2;
import org.matsim.vis.otfvis.server.OnTheFlyServer;

import playground.david.prefuse.OTFVisDualView;

public class ExeViaListener {
	public static class SimCanceledException extends RuntimeException{
		
	};
	

	public static class ResetableMobsimControler extends Controler{
		protected boolean useResetable = true;
		
		public static class MyEvents extends Events{
			List<BasicEvent> events = new ArrayList<BasicEvent>();

			@Override
			public void processEvent(BasicEvent event) {
				events.add(event);
			}
			
		}

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
			Events events = this.events;
			this.events = new MyEvents();
			
			while(!simIsFinished) {
				try {
					
					onRunMobsim();
					simIsFinished = true;
				} catch (SimCanceledException e) {
					System.out.println("Sim canceled... Restarting!");
				}
			}

			List<BasicEvent> myEvents = ((MyEvents)this.events).events;
			for(BasicEvent event : myEvents) events.processEvent(event);
			this.events = events;
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
QueueSimulationInitializedListener,
QueueSimulationAfterSimStepListener {

	private QueueNetwork queueNetwork;
	protected OnTheFlyServer myOTFServer;
	private Population population;
	private Events events;

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		Controler cont = event.getControler();
		myOTFServer.setControllerStatus(OTFVisController.RUNNING + cont.getIteration());
	}

	public void notifyAfterMobsim(AfterMobsimEvent event) {
		Controler cont = event.getControler();
		myOTFServer.setControllerStatus(OTFVisController.REPLANNING + cont.getIteration()+1);
	}

	public void notifySimulationAfterSimStep(QueueSimulationAfterSimStepEvent e) {
		int status = myOTFServer.updateStatus(e.getSimulationTime());
		if(myOTFServer.getRequestStatus() == OTFVisController.CANCEL) {
			try {
				myOTFServer.requestControllerStatus(0);
				myOTFServer.reset();
			} catch (RemoteException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} // reset status!
			myOTFServer.reset();
			throw new SimCanceledException();
		}
	}

	public void notifySimulationInitialized(QueueSimulationInitializedEvent e) {
		QueueSimulation q = e.getQueueSimulation();
		myOTFServer.events = QueueSimulation.getEvents();
		myOTFServer.replaceQueueNetwork(q.getQueueNetwork());
	}

	protected void createServer(final UUID idOne, StartupEvent event) {
		this.myOTFServer = OnTheFlyServer.createInstance("OTFServer_" + idOne.toString(), this.queueNetwork, this.population, this.events, false);
		myOTFServer.setControllerStatus(OTFVisController.STARTUP);
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
		PreferencesDialog.preDialogClass = PreferencesDialog2.class;
		OnTheFlyClientQuad client = new OnTheFlyClientQuad(url);
		client.start();
	}
	
}

public static class OTFPopShowListener extends OTFControlerListener implements 
StartupListener,
BeforeMobsimListener, 
AfterMobsimListener, 
QueueSimulationInitializedListener,
QueueSimulationAfterSimStepListener {
	@Override
	protected void startupClient(String url) {
		OTFVisConfig visconf = new OTFVisConfig();
		if (Gbl.getConfig() == null) Gbl.createConfig(null);
		Gbl.getConfig().addModule(OTFVisConfig.GROUP_NAME, visconf);

		PreferencesDialog.preDialogClass = PreferencesDialog2.class;
		OnTheFlyClientFileQuad client = new OTFVisDualView(url, null, false);
		client.start();
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

