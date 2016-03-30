/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.dgrether.xvis;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;

import playground.dgrether.xvis.control.XVisControl;
import playground.dgrether.xvis.gui.MainFrame;
import playground.dgrether.xvis.gui.MainFrameWindowListener;
import playground.dgrether.xvis.gui.Menu;
import playground.dgrether.xvis.gui.PanelManager;
import playground.dgrether.xvis.gui.PanelManager.Area;
import playground.dgrether.xvis.gui.processing.ControlButtonPanel;
import playground.dgrether.xvis.gui.processing.PWorldPanel;
import playground.dgrether.xvis.gui.signalstreepanel.SignalsTreePanel;
import playground.dgrether.xvis.vismodel.VisScenario;

/**
 * @author dgrether
 */
public class XVisStartup {
	
	private static final Logger log = Logger.getLogger(XVisStartup.class);

	private PanelManager panelManager = new PanelManager();
	
	private PWorldPanel worldPanel;

	
	private void initializeGui(DataManager dataManager) {
		this.setLookAndFeel();
		log.info("initializing...");
		JFrame frame = this.initMainFrame();
		new Menu().init(frame);
		this.initPanels(frame, dataManager);
		frame.setVisible(true);
		log.info("XVis initialized!");
	}
	

	private void initPanels(JFrame frame, DataManager dataManager) {
		JPanel mainPanel = this.panelManager.createMainPanel();
		frame.add(mainPanel, BorderLayout.CENTER);
		
		ControlButtonPanel controlPanel = new ControlButtonPanel();
		this.panelManager.addContainer(controlPanel, Area.BUTTON);
		
		if (dataManager.getVisScenario() != null){
			this.worldPanel = new PWorldPanel();
			this.worldPanel.init();
			XVisControl.getInstance().getControlEventsManager().addControlListener(worldPanel);
			this.panelManager.addContainer(worldPanel, Area.CENTER);
			this.displayNetwork(dataManager.getVisScenario());
		}
		
		if (dataManager.getSignalsData() != null){
			JPanel signalControlPanel = 	new SignalsTreePanel(dataManager.getSignalsData());
			this.panelManager.addContainer(signalControlPanel, Area.LEFT);
			
//			SignalSystemControllerData ss = this.dataManager.getSignalsData().getSignalControlData().getSignalSystemControllerDataBySystemId().get(new IdImpl("17"));
//			SignalPlanData plan = ss.getSignalPlanData().get(new IdImpl("sylvia_plan_1"));
//			XVisControl.getInstance().getControlEventsManager().fireShowPanelEvent(new ShowPanelEvent(new SignalPlanPanel(new IdImpl("17"), plan), PanelManager.Area.RIGHT));
		}
	}


	private JFrame initMainFrame(){
		MainFrame mainFrame = new MainFrame("XVis");
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		mainFrame.setSize(screenSize.width/2,screenSize.height/2);
		mainFrame.addWindowListener( new MainFrameWindowListener());
		return mainFrame;
	}
	
	
	public void runVisualizer(Scenario scenario) {
		DataManager dataManager = new DataManager(scenario);
		dataManager.createVisScenario();
		this.initializeGui(dataManager);
	}	
	
	private void displayNetwork(VisScenario networkData) {
		this.worldPanel.showNetwork(networkData);
	}

	
	private void setLookAndFeel(){
		try {
	    // Set System L&F
        UIManager.setLookAndFeel(
            UIManager.getSystemLookAndFeelClassName());
//        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        log.debug("Setting look and feel to " + UIManager.getSystemLookAndFeelClassName());
		} 
    catch (UnsupportedLookAndFeelException e) {
    	e.printStackTrace();
    }
    catch (ClassNotFoundException e) {
    	e.printStackTrace();
    }
    catch (InstantiationException e) {
    	e.printStackTrace();
    }
    catch (IllegalAccessException e) {
    	e.printStackTrace();
    }
	}
}
