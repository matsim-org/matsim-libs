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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
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
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;

public final class OTFHostControlBar extends JToolBar implements ActionListener, ItemListener {

	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(OTFHostControlBar.class);

	private static final String TO_START = "to_start";
	private static final String PLAY = "play";
	private static final String STEP_F = "step_f";
	private static final String STEP_FF = "step_ff";
	private static final String SET_TIME = "set_time";
	private static final String TOGGLE_SYNCH = "Synch";
	private static final String STEP_BB = "step_bb";
	private static final String STEP_B = "step_b";
	private static final String SCALE = "scale";

	private JButton playPauseButton;

	private JFormattedTextField timeField;

	private JFormattedTextField scaleField;

	private final OTFHostConnectionManager masterHostConnectionManager;

	private final List <OTFHostConnectionManager> hostConnectionManagers = new ArrayList<OTFHostConnectionManager>();

	private final OTFHostControl hostControl;

	private int gotoIter = 0;

	private boolean synchronizedPlay = true;

	private ImageIcon playIcon = null;

	private ImageIcon pauseIcon = null;

	private OTFAbortGoto progressBar = null;

	private static enum PlayOrPause {PLAY, PAUSE};

	/*
	 * The symbol which the play/pause button currently shows.
	 * (NOT the mode we are in!)
	 */
	private PlayOrPause playOrPause = PlayOrPause.PLAY;

	public OTFHostControlBar(OTFHostConnectionManager masterHostConnectionManager)  {
		this.masterHostConnectionManager = masterHostConnectionManager;
		this.hostConnectionManagers.add(this.masterHostConnectionManager);
		this.hostControl = new OTFHostControl(masterHostConnectionManager, this);
		this.setFloatable(false);

		playIcon = new ImageIcon(MatsimResource.getAsImage("otfvis/buttonPlay.png"), "Play");
		pauseIcon = new ImageIcon(MatsimResource.getAsImage("otfvis/buttonPause.png"), "Pause");

		if (!this.masterHostConnectionManager.isLiveHost()) {
			add(createButton("Restart", TO_START, "buttonRestart", "restart the server/simulation"));
			add(createButton("<<", STEP_BB, "buttonStepBB", "go several timesteps backwards"));
			add(createButton("<", STEP_B, "buttonStepB", "go one timestep backwards"));
		}

		playPauseButton = createButton("PLAY", PLAY, "buttonPlay", "press to play simulation continuously");
		add(playPauseButton);
		add(createButton(">", STEP_F, "buttonStepF", "go one timestep forward"));
		add(createButton(">>", STEP_FF, "buttonStepFF", "go several timesteps forward"));
		MessageFormat format = new MessageFormat("{0,number,00}:{1,number,00}:{2,number,00}");
		if (this.masterHostConnectionManager.isLiveHost() && (hostControl.getControllerStatus() != OTFVisControlerListener.NOCONTROL)) {
			format = new MessageFormat("{0,number,00}#{0,number,00}:{1,number,00}:{2,number,00}");
		}
		timeField = new JFormattedTextField(format);
		timeField.setMaximumSize(new Dimension(100,30));
		timeField.setMinimumSize(new Dimension(80,30));
		timeField.setActionCommand(SET_TIME);
		timeField.setHorizontalAlignment(JTextField.CENTER);
		add( timeField );
		timeField.addActionListener( this );

		createCheckBoxes();
		add(new JLabel(this.masterHostConnectionManager.getAddress()));

		JLabel lab = new JLabel("Scale: ");
		lab.setMaximumSize(new Dimension(100,30));
		lab.setMinimumSize(new Dimension(80,30));
		lab.setHorizontalAlignment(JLabel.RIGHT);
		add(lab);
		scaleField = new JFormattedTextField("1.0");
		scaleField.setMaximumSize(new Dimension(50,30));
		scaleField.setMinimumSize(new Dimension(30,30));
		scaleField.setActionCommand(SCALE);
		scaleField.setHorizontalAlignment(JTextField.CENTER);
		add( scaleField );
		scaleField.addActionListener( this );
		log.debug("HostControlBar initialized.");
	}

	public void addDrawer(String id, OTFDrawer handler) {
		this.masterHostConnectionManager.getDrawer().put(id, handler);
	}

	public void invalidateDrawers() {
		try {
			for (OTFHostConnectionManager slave : hostConnectionManagers) {
				for (OTFDrawer handler : slave.getDrawer().values()) {
					handler.invalidate(hostControl.getSimTime());
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void redrawDrawers() {
		for(OTFHostConnectionManager slave : hostConnectionManagers) {
			for (OTFDrawer handler : slave.getDrawer().values()) {
				handler.redraw();
			}
		}
	}

	private boolean requestTimeStep(int newTime, OTFServerRemote.TimePreference prefTime) {
		boolean result = hostControl.requestTimeStep(newTime, prefTime);
		if (result) {
			invalidateDrawers();
		}
		return result;
	}

	public void clearCaches() {
		for(OTFHostConnectionManager slave : hostConnectionManagers) {
			for (OTFDrawer handler : slave.getDrawer().values()) {
				handler.clearCache();
			}
		}
	}

	private JButton createButton(String altText, String actionCommand, String imageName, final String toolTipText) {
		BorderlessButton button = new BorderlessButton();
		button.putClientProperty("JButton.buttonType","icon");
		button.setActionCommand(actionCommand);
		button.addActionListener(this);
		button.setToolTipText(toolTipText);
		String imgLocation = "otfvis/" + imageName + ".png";
		ImageIcon icon = new ImageIcon(MatsimResource.getAsImage(imgLocation), altText);
		button.setIcon(icon);
		return button;
	}

	public void updateTimeLabel() {
		hostControl.fetchTimeAndStatus();
		timeField.setText(Time.writeTime(hostControl.getSimTime()));
	}

	public void stopMovie() {
		hostControl.stopMovie();
		playPauseButton.setIcon(playIcon);
	}

	public void updateScaleLabel() {
		float scale = 1.f;
		for(OTFDrawer drawer: this.masterHostConnectionManager.getDrawer().values()){
			if (drawer.getScale() != 0){
				scale = drawer.getScale();
			}
		}
		scale = (float)(Math.round(scale*100))/100;
		this.scaleField.setText(String.valueOf(scale));
	}

	private void pressed_PLAY() {
		if (playOrPause == PlayOrPause.PLAY) {
			playPauseButton.setIcon(pauseIcon);
			hostControl.play(synchronizedPlay);
			playOrPause = PlayOrPause.PAUSE;
		} else if (playOrPause == PlayOrPause.PAUSE) {
			playPauseButton.setIcon(playIcon);
			hostControl.pause();
			playOrPause = PlayOrPause.PLAY;
		} else {
			throw new RuntimeException();
		}
	}

	private void pressed_STEP_F() {
		requestTimeStep(hostControl.getSimTime() + 1, OTFServerRemote.TimePreference.LATER);
	}

	private void pressed_STEP_FF() {
		int bigStep = OTFClientControl.getInstance().getOTFVisConfig().getBigTimeStep();
		requestTimeStep(hostControl.getSimTime() + bigStep, OTFServerRemote.TimePreference.LATER);
	}

	private void pressed_STEP_B() {
		requestTimeStep(hostControl.getSimTime() - 1, OTFServerRemote.TimePreference.EARLIER);
	}

	private void pressed_STEP_BB() {
		int bigStep = OTFClientControl.getInstance().getOTFVisConfig().getBigTimeStep();
		requestTimeStep(hostControl.getSimTime() - bigStep, OTFServerRemote.TimePreference.EARLIER);
	}

	private void forwardToTime(String newTime) {
		int index = newTime.indexOf('#');
		String tmOfDay = newTime.substring(index + 1);
		if ((index != -1) && (hostControl.getControllerStatus() != OTFVisControlerListener.NOCONTROL)) {
			gotoIter = Integer.parseInt(newTime.substring(0, index));
		}
		final int newTime_s = (int) Time.parseTime(tmOfDay);
		progressBar = new OTFAbortGoto(masterHostConnectionManager.getOTFServer(), newTime_s, gotoIter);
		progressBar.start();
		new Thread() {
			@Override
			public void run() {
				hostControl.gotoTime(newTime_s, progressBar);
			}
		}.start();
	}

	private void changed_SCALE(ActionEvent event) {
		String newScale = ((JFormattedTextField) event.getSource()).getText();
		for(OTFDrawer drawer: this.masterHostConnectionManager.getDrawer().values()){
			drawer.setScale(Float.parseFloat(newScale));
		}
	}

	public void actionPerformed(ActionEvent event) {
		String command = event.getActionCommand();
		if (TO_START.equals(command)) {
			toStart();
		} else if (PLAY.equals(command)) {
			pressed_PLAY();
		} else if (STEP_F.equals(command)) {
			pressed_STEP_F();
		} else if (STEP_FF.equals(command)) {
			pressed_STEP_FF();
		} else if (STEP_B.equals(command)) {
			pressed_STEP_B();
		} else if (STEP_BB.equals(command)) {
			pressed_STEP_BB();
		} else if (command.equals(SET_TIME)) {
			String newTime = ((JFormattedTextField) event.getSource()).getText();
			forwardToTime(newTime);
		} else if (command.equals(SCALE)) {
			changed_SCALE(event);
		}
		updateTimeLabel();
		repaint();
	}

	private void toStart() {
		this.hostControl.toStart();
		if(this.hostControl.isLive()) {
			updateTimeLabel();
			repaint();
		}
	}

	private void createCheckBoxes() {
		if (masterHostConnectionManager.isLiveHost()) {
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
			hostControl.updateSyncPlay(synchronizedPlay);
		}
		repaint();
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
		this.hostConnectionManagers.add(slave);
	}

	/**
	 * Method should be removed again when we once finish the refactoring
	 */
	public OTFHostConnectionManager getOTFHostConnectionManager(){
		return this.masterHostConnectionManager;
	}

	public OTFHostControl getOTFHostControl() {
		return hostControl;
	}

	public boolean isSynchronizedPlay() {
		return synchronizedPlay;
	}

}
