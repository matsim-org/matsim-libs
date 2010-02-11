/* *********************************************************************** *
 * project: org.matsim.*
 * OTFHostControlBar.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis.gui;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.border.Border;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.OTFVisControlerListener;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFLiveServerRemote;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;


/**
 * This class is most important for the OTFVis client. It serves as the connector to the actual server.
 * Additionally it is responsible for all actions associated with the control bar on top of the OTFVis' screen.
 * Any communication with the server will run through this class.
 *
 * @author dstrippgen
 *
 */
public class OTFHostControlBar extends JToolBar implements ActionListener, ItemListener {

	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(OTFHostControlBar.class);

	private static final String TO_START = "to_start";
	private static final String PAUSE = "pause";
	private static final String PLAY = "play";
	private static final String STEP_F = "step_f";
	private static final String STEP_FF = "step_ff";
	private static final String STOP = "stop";
	private static final String SET_TIME = "set_time";
	private static final String TOGGLE_SYNCH = "Synch";
	private static final String STEP_BB = "step_bb";
	private static final String STEP_B = "step_b";
	private static final String FULLSCREEN = "fullscreen";

	// -------------------- MEMBER VARIABLES --------------------

	private transient MovieTimer movieTimer = null;
	private JButton playButton;
	private JFormattedTextField timeField;
	private int simTime = 0;
	private boolean synchronizedPlay = true;

	private int controllerStatus = 0;

	private final List <OTFHostConnectionManager> hostControls = new ArrayList<OTFHostConnectionManager>();

	private ImageIcon playIcon = null;
	private ImageIcon pauseIcon = null;

	private JFrame frame = null;

	private Rectangle windowBounds = null;

	private final OTFHostConnectionManager hostControl;
	private int gotoTime = 0;
	private int gotoIter = 0;
	private transient OTFAbortGoto progressBar = null;

	// -------------------- CONSTRUCTION --------------------

	public OTFHostControlBar(String address, JFrame frame) throws RemoteException, InterruptedException, NotBoundException {
		this.hostControl = new OTFHostConnectionManager(address);
		this.hostControls.add(this.hostControl);
	  addButtons();
		this.frame = frame;
	}

	public void addDrawer(String id, OTFDrawer handler) {
		this.hostControl.getDrawer().put(id, handler);
	}

	public void invalidateDrawers() {
		try {
			for (OTFHostConnectionManager slave : hostControls) {
				for (OTFDrawer handler : slave.getDrawer().values()) {
					handler.invalidate(simTime);
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void redrawDrawers() {
		for(OTFHostConnectionManager slave : hostControls) {
				for (OTFDrawer handler : slave.getDrawer().values()) {
					handler.redraw();
			}
		}
	}

	public void clearCaches() {
		for(OTFHostConnectionManager slave : hostControls) {
			for (OTFDrawer handler : slave.getDrawer().values()) {
				handler.clearCache();
			}
		}
	}

	private void addButtons() throws RemoteException {
		this.setFloatable(false);

		playIcon = new ImageIcon(MatsimResource.getAsImage("otfvis/buttonPlay.png"), "Play");
		pauseIcon = new ImageIcon(MatsimResource.getAsImage("otfvis/buttonPause.png"), "Pause");

		add(createButton("Restart", TO_START, "buttonRestart", "restart the server/simulation"));
		if (!this.hostControl.getOTFServer().isLive()) {
			add(createButton("<<", STEP_BB, "buttonStepBB", "go several timesteps backwards"));
			add(createButton("<", STEP_B, "buttonStepB", "go one timestep backwards"));
		}

		playButton = createButton("PLAY", PLAY, "buttonPlay", "press to play simulation continuously");
		add(playButton);
		add(createButton(">", STEP_F, "buttonStepF", "go one timestep forward"));
		add(createButton(">>", STEP_FF, "buttonStepFF", "go several timesteps forward"));
		MessageFormat format = new MessageFormat("{0,number,00}:{1,number,00}:{2,number,00}");
		if(this.hostControl.getOTFServer().isLive()) {
			 if(controllerStatus != OTFVisControlerListener.NOCONTROL) {
					 format = new MessageFormat("{0,number,00}#{0,number,00}:{1,number,00}:{2,number,00}");
			 }
		}
		timeField = new JFormattedTextField(format);
		timeField.setMaximumSize(new Dimension(100,30));
		timeField.setMinimumSize(new Dimension(80,30));
		timeField.setActionCommand(SET_TIME);
		timeField.setHorizontalAlignment(JTextField.CENTER);
		add( timeField );
		timeField.addActionListener( this );

		createCheckBoxes();

		add(new JLabel(this.hostControl.getAddress()));
	}

	private JButton createButton(String altText, String actionCommand, String imageName, final String toolTipText) {
		BorderlessButton button = new BorderlessButton();
		button.putClientProperty("JButton.buttonType","icon");
		button.setActionCommand(actionCommand);
		button.addActionListener(this);
	  button.setToolTipText(toolTipText);

	  if (imageName != null) {
	  	// with image
	  	//Look for the image.
	  	String imgLocation = "otfvis/" + imageName + ".png";
	  	ImageIcon icon =new ImageIcon(MatsimResource.getAsImage(imgLocation), altText);
	  	if(icon.getIconHeight() != -1) button.setIcon(icon);
	  	else button.setText(altText);
	  } else {
	  	// without image
	  	button.setText(altText);
	  }

	  return button;
	}

	public void updateTimeLabel() throws RemoteException {
		simTime = this.hostControl.getOTFServer().getLocalTime();
		if(controllerStatus != OTFVisControlerListener.NOCONTROL){
			controllerStatus = ((OTFLiveServerRemote)this.hostControl.getOTFServer()).getControllerStatus();
		}

		switch (OTFVisControlerListener.getStatus(controllerStatus)) {
		case OTFVisControlerListener.STARTUP:
			timeField.setText(OTFVisControlerListener.getIteration(controllerStatus) +"#Preparing...#");
			break;
		case (OTFVisControlerListener.RUNNING + OTFVisControlerListener.PAUSED):
			if((movieTimer != null) && !synchronizedPlay) stopMovie();
		case OTFVisControlerListener.RUNNING:
			timeField.setText(OTFVisControlerListener.getIteration(controllerStatus) +"#" +Time.writeTime(simTime));
			break;
		case OTFVisControlerListener.REPLANNING:
			timeField.setText(OTFVisControlerListener.getIteration(controllerStatus) +"#Replanning...#");
			break;
		case OTFVisControlerListener.CANCEL:
			timeField.setText(OTFVisControlerListener.getIteration(controllerStatus) +"#Cancelling...#");
			break;

		default:
			timeField.setText(Time.writeTime(simTime));
			break;
		}
	}

	// ---------- IMPLEMENTATION OF ActionListener INTERFACE ----------

	public void stopMovie() {
		if (movieTimer != null) {
			movieTimer.terminate();
			movieTimer = null;
			playButton.setIcon(playIcon);
		}
	}

	private void pressed_TO_START() throws IOException {
		stopMovie();
		if(this.hostControl.getOTFServer().isLive()) {
			((OTFLiveServerRemote)this.hostControl.getOTFServer()).requestControllerStatus(OTFVisControlerListener.CANCEL);
			requestTimeStep(0, OTFServerRemote.TimePreference.LATER);
			simTime = 0;
			updateTimeLabel();
			repaint();
		} else {
			requestTimeStep(loopStart, OTFServerRemote.TimePreference.LATER);
			log.debug("To start...");
		}
	}

	private void pressed_PAUSE() throws IOException {
		log.debug("Pressed PAUSE.");
		stopMovie();
		if (hostControl.getOTFServer().isLive()) {
			pressPauseOnServer();
		}
	}

	private void pressed_PLAY() throws IOException {
		if (movieTimer == null) {
			log.debug("Pressed PLAY, creating movie timer.");
			movieTimer = new MovieTimer();
			playButton.setIcon(pauseIcon);
			movieTimer.start();
			if (!synchronizedPlay) {
				pressPlayOnServer();
			}
		} else {
			log.debug("Pressed PLAY, but there already is a movie timer.");
			pressed_PAUSE();
		}
	}

	private void pressed_FULLSCREEN() {
		if (this.frame == null) {
			return;
		}
		GraphicsDevice gd = this.frame.getGraphicsConfiguration().getDevice();
		if (gd.getFullScreenWindow() == null) {
			log.debug("enter fullscreen");
			this.windowBounds = frame.getBounds();
	  	frame.dispose();
	  	frame.setUndecorated(true);
	  	gd.setFullScreenWindow(frame);
		} else {
			log.debug("exit fullscreen");
			gd.setFullScreenWindow(null);
			frame.dispose();
			frame.setUndecorated(false);
			frame.setBounds(this.windowBounds);
			frame.setVisible(true);
		}
		float linkWidth = OTFClientControl.getInstance().getOTFVisConfig().getLinkWidth();
		OTFClientControl.getInstance().getOTFVisConfig().setLinkWidth(linkWidth + 0.01f);// forces redraw of network, haven't found a better way to do it. marcel/19apr2009
		SimpleStaticNetLayer.marktex = null;
		redrawDrawers();
	}

	private boolean requestTimeStep(int newTime, OTFServerRemote.TimePreference prefTime)  throws IOException {
		if (hostControl.getOTFServer().requestNewTime(newTime, prefTime)) {
			simTime = hostControl.getOTFServer().getLocalTime();

			for(OTFHostConnectionManager slave : hostControls) {
				if (!slave.equals(this.hostControl))
					slave.getOTFServer().requestNewTime(newTime, prefTime);
			}
			invalidateDrawers();
			return true;
		}
		if (prefTime == OTFServerRemote.TimePreference.EARLIER) {
			log.info("No previous timestep found");
		} else {
			log.info("No succeeding timestep found");
		}
		return false;
	}

	private void pressed_STEP_F() throws IOException {
		if(movieTimer != null) pressed_PAUSE();
		else requestTimeStep(simTime+1, OTFServerRemote.TimePreference.LATER);
	}

	private void pressed_STEP_FF() throws IOException {
		int bigStep = OTFClientControl.getInstance().getOTFVisConfig().getBigTimeStep();
		if(movieTimer != null) pressed_PAUSE();
		else requestTimeStep(simTime+bigStep, OTFServerRemote.TimePreference.LATER);
	}

	private void pressed_STEP_B() throws IOException {
		requestTimeStep(simTime-1, OTFServerRemote.TimePreference.EARLIER);
	}

	private void pressed_STEP_BB() throws IOException {
		int bigStep = OTFClientControl.getInstance().getOTFVisConfig().getBigTimeStep();
		requestTimeStep(simTime-bigStep, OTFServerRemote.TimePreference.EARLIER);
}

	private void pressed_STOP() throws IOException {
		pressed_PAUSE();
	}


	private void gotoTime() {
		boolean restart = (OTFVisControlerListener.getIteration(controllerStatus) == gotoIter) && (gotoTime < simTime);

		try {
			// in case of live host, additionally request iteration
			if(hostControl.getOTFServer().isLive()) {
				((OTFLiveServerRemote)hostControl.getOTFServer()).requestControllerStatus(gotoIter);
			}

			synchronized(hostControl.blockReading) {
			if(restart){
			  requestTimeStep(gotoTime, OTFServerRemote.TimePreference.RESTART);
			}
			else if (!requestTimeStep(gotoTime, OTFServerRemote.TimePreference.EARLIER)) {
				requestTimeStep(gotoTime, OTFServerRemote.TimePreference.LATER);
			}

			if (progressBar != null) {
			  progressBar.terminate = true;
			}
			simTime = hostControl.getOTFServer().getLocalTime();
			updateTimeLabel();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setNEWTime(int newTime_s) {
		if (newTime_s == simTime) return;

		stopMovie();
		gotoTime = newTime_s;
		gotoTime();
	}

	private void changed_SET_TIME(ActionEvent event) {
		String newTime = ((JFormattedTextField) event.getSource()).getText();
		int index = newTime.indexOf('#');
		String tmOfDay = newTime.substring(index + 1);
		if ((index != -1) && (controllerStatus != OTFVisControlerListener.NOCONTROL)) {
			gotoIter = Integer.parseInt(newTime.substring(0, index));
		}
		int newTime_s = (int) Time.parseTime(tmOfDay);
		progressBar = new OTFAbortGoto(hostControl.getOTFServer(), newTime_s, gotoIter);
		progressBar.start();
		gotoTime = newTime_s;
		new Thread() {
			@Override
			public void run() {
				gotoTime();
			}
		}.start();
	}

	public void actionPerformed(ActionEvent event) {
		String command = event.getActionCommand();
		try {
			if (TO_START.equals(command))
				pressed_TO_START();
			else if (PAUSE.equals(command))
				pressed_PAUSE();
			else if (PLAY.equals(command))
				pressed_PLAY();
			else if (STEP_F.equals(command))
				pressed_STEP_F();
			else if (STEP_FF.equals(command))
				pressed_STEP_FF();
			else if (STEP_B.equals(command))
				pressed_STEP_B();
			else if (STEP_BB.equals(command))
				pressed_STEP_BB();
			else if (STOP.equals(command))
				pressed_STOP();
			else if (FULLSCREEN.equals(command)) {
				pressed_FULLSCREEN();
			} else if (command.equals(SET_TIME))
				changed_SET_TIME(event);
		} catch (IOException e) {
			log.error("ControlToolbar encountered problem.");
			e.printStackTrace();
		}
		try {
			updateTimeLabel();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		repaint();
	}

	private void createCheckBoxes() {
		if (hostControl.isLiveHost()) {
			JCheckBox synchBox = new JCheckBox(TOGGLE_SYNCH);
			synchBox.setMnemonic(KeyEvent.VK_V);
			synchBox.setSelected(synchronizedPlay);
			synchBox.addItemListener(this);
			add(synchBox);
		}

	}

	public void itemStateChanged(ItemEvent e) {
		JCheckBox source = (JCheckBox) e.getItemSelectable();
		if (source.getText().equals(TOGGLE_SYNCH)) {
			synchronizedPlay = source.isSelected();
			if (movieTimer != null) {
				movieTimer.updateSyncPlay();
			}
		}
		repaint();
	}

	private int loopStart = 0;
	private int loopEnd = Integer.MAX_VALUE;

	/**
	 *  sets the loop that the movieplayer should loop
	 * @param min either sec for startloop or -1 for leave unchanged default =0
	 * @param max either sec for endloop or -1 for leave unchanged default = Integer.MAX_VALUE
	 */
	public void setLoopBounds(int min, int max) {
		if (min != -1) {
			loopStart = min;
		}
		if (max != -1) {
			loopEnd = max;
		}
	}

	class MovieTimer extends Thread {
		private boolean terminate = false;

		public MovieTimer() {
			setDaemon(true);
		}

		private synchronized void updateSyncPlay() {
			try {
				if (!hostControl.isLiveHost()) {
					return;
				}
				if (synchronizedPlay) {
					pressPauseOnServer();
				} else {
					pressPlayOnServer();
				}
				simTime = hostControl.getOTFServer().getLocalTime();
			} catch (RemoteException e) {
				e.printStackTrace();
			}

		}

		public synchronized void terminate() {
			this.terminate = true;
		}

		@Override
		public void run() {
			int delay = 30;
			int actTime = 0;
			while (!terminate) {
				try {
					delay = OTFClientControl.getInstance().getOTFVisConfig().getDelay_ms();
					sleep(delay);
					synchronized (hostControl.blockReading) {
						if (synchronizedPlay
								&& ((simTime >= loopEnd) || !hostControl
										.getOTFServer()
										.requestNewTime(
												simTime + 1,
												OTFServerRemote.TimePreference.LATER))) {
							hostControl.getOTFServer().requestNewTime(
									loopStart,
									OTFServerRemote.TimePreference.LATER);
						}
						actTime = simTime;
						simTime = hostControl.getOTFServer().getLocalTime();
						for (OTFHostConnectionManager slave : hostControls) {
							if (!slave.equals(hostControl))
								slave.getOTFServer().requestNewTime(simTime, OTFServerRemote.TimePreference.LATER);
						}
						updateTimeLabel();
						if (simTime != actTime) {
							repaint();
							invalidateDrawers();
						}
					}
				} catch (RemoteException e) {
					e.printStackTrace();
					stopMovie();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void finishedInitialisition() {
		this.hostControl.finishedInitialisition();
	}


	private static class BorderlessButton extends JButton {

		private static final long serialVersionUID = 1L;

		public BorderlessButton() {
			super();
			super.setBorder(null);
		}
		@Override
		public void setBorder(final Border border) {
			// ignore border setting to overwrite specific look&feel
		}
	}

	public void addSlave(OTFHostConnectionManager slave) {
		this.hostControls.add(slave);
	}

	/**
	 * Method should be removed again when we once finish the refactoring
	 */
	public OTFHostConnectionManager getOTFHostControl(){
		return this.hostControl;
	}

	private void pressPlayOnServer() throws RemoteException {
		((OTFLiveServerRemote) hostControl.getOTFServer()).play();
	}

	private void pressPauseOnServer() throws RemoteException {
		((OTFLiveServerRemote) hostControl.getOTFServer()).pause();
	}

}
