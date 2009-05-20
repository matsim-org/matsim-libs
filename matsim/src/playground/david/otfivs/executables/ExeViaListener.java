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
import java.util.UUID;

import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
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

//import playground.david.prefuse.OTFVisDualView;

public class ExeViaListener {
	
public static class OTFControlerListener implements 
StartupListener,
BeforeMobsimListener, 
AfterMobsimListener, 
QueueSimulationInitializedListener,
QueueSimulationAfterSimStepListener {

	private QueueNetwork queueNetwork;
	private OnTheFlyServer myOTFServer;
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
	}

	public void notifySimulationInitialized(QueueSimulationInitializedEvent e) {
		QueueSimulation q = e.getQueueSimulation();
		myOTFServer.events = QueueSimulation.getEvents();
		myOTFServer.replaceQueueNetwork(q.getQueueNetwork());
	}

	public void notifyStartup(StartupEvent event) {
		UUID idOne = UUID.randomUUID();
		Controler cont = event.getControler();
		this.population = cont.getPopulation();
		this.events = cont.getEvents();
		this.queueNetwork = new QueueNetwork(cont.getNetwork());
		// DSTODO take care of Event somewhat later
		this.myOTFServer = OnTheFlyServer.createInstance("OTFServer_" + idOne.toString(), this.queueNetwork, this.population, this.events, false);
		myOTFServer.setControllerStatus(OTFVisController.STARTUP);
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

		Controler controler = new Controler(cfg);
		controlListener = new OTFControlerListener();
//		controlListener = new OTFPopShowListener();
		controler.addControlerListener(controlListener);
		controler.getQueueSimulationListener().add(controlListener);
		controler.run();
	}

}

