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

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.interfaces.OTFServer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;

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

	private final OTFHostControl hostControl;

    private boolean synchronizedPlay = true;

	private ImageIcon playIcon = null;

	private ImageIcon pauseIcon = null;

	private OTFAbortGoto progressBar = null;

	private static enum PlayOrPause {PLAY, PAUSE}

    private OTFOGLDrawer drawer;

	/*
	 * The symbol which the play/pause button currently shows.
	 * (NOT the mode we are in!)
	 */
	private PlayOrPause playOrPause = PlayOrPause.PLAY;

	private OTFServer server;

	public OTFHostControlBar(OTFServer server)  {
		this.server = server;
		this.hostControl = new OTFHostControl(server, this);
		this.setFloatable(false);

		playIcon = new ImageIcon(MatsimResource.getAsImage("otfvis/buttonPlay.png"), "Play");
		pauseIcon = new ImageIcon(MatsimResource.getAsImage("otfvis/buttonPause.png"), "Pause");

		if (!server.isLive()) {
			add(createButton("Restart", TO_START, "buttonRestart", "restart the server/simulation"));
			add(createButton("<<", STEP_BB, "buttonStepBB", "go several timesteps backwards"));
			add(createButton("<", STEP_B, "buttonStepB", "go one timestep backwards"));
		}

		playPauseButton = createButton("PLAY", PLAY, "buttonPlay", "press to play simulation continuously");
		add(playPauseButton);
		add(createButton(">", STEP_F, "buttonStepF", "go one timestep forward"));
		add(createButton(">>", STEP_FF, "buttonStepFF", "go several timesteps forward"));
		MessageFormat format = new MessageFormat("{0,number,00}:{1,number,00}:{2,number,00}");
		timeField = new JFormattedTextField(format);
		timeField.setMaximumSize(new Dimension(100,30));
		timeField.setMinimumSize(new Dimension(80,30));
		timeField.setActionCommand(SET_TIME);
		timeField.setHorizontalAlignment(JTextField.CENTER);
		add( timeField );
		timeField.addActionListener( this );

		createCheckBoxes();

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
		updateTimeLabel();
	}

	public void redrawDrawers() {
		drawer.redraw();
	}

	private boolean requestTimeStep(int newTime, OTFServer.TimePreference prefTime) {
        return hostControl.requestTimeStep(newTime, prefTime);
	}

	public void clearCaches() {
		drawer.clearCache();
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

	void updateTimeLabel() {
		String text = Time.writeTime(hostControl.getSimTime());
		timeField.setText(text);
	}

    public void updateScaleLabel() {
		double scale = drawer.getScale();
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
		requestTimeStep(hostControl.getSimTime() + 1, OTFServer.TimePreference.LATER);
	}

	private void pressed_STEP_FF() {
		int bigStep = OTFClientControl.getInstance().getOTFVisConfig().getBigTimeStep();
		requestTimeStep(hostControl.getSimTime() + bigStep, OTFServer.TimePreference.LATER);
	}

	private void pressed_STEP_B() {
		requestTimeStep(hostControl.getSimTime() - 1, OTFServer.TimePreference.EARLIER);
	}

	private void pressed_STEP_BB() {
		int bigStep = OTFClientControl.getInstance().getOTFVisConfig().getBigTimeStep();
		requestTimeStep(hostControl.getSimTime() - bigStep, OTFServer.TimePreference.EARLIER);
	}

	private void forwardToTime(String newTime) {
		final int newTime_s = (int) Time.parseTime(newTime);
        int gotoIter = 0;
        progressBar = new OTFAbortGoto(server, newTime_s, gotoIter);
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
		drawer.setScale(Float.parseFloat(newScale));
	}

	@Override
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
		repaint();
	}

	private void toStart() {
		this.hostControl.toStart();
		if(this.hostControl.isLive()) {
			repaint();
		}
	}

	private void createCheckBoxes() {
		if (server.isLive()) {
			JCheckBox synchBox = new JCheckBox(TOGGLE_SYNCH);
			synchBox.setMnemonic(KeyEvent.VK_V);
			synchBox.setSelected(synchronizedPlay);
			synchBox.addItemListener(this);
			add(synchBox);
		}

	}

	@Override
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

	public OTFHostControl getOTFHostControl() {
		return hostControl;
	}

	public boolean isSynchronizedPlay() {
		return synchronizedPlay;
	}

	public void setDrawer(OTFOGLDrawer mainDrawer) {
		this.drawer = mainDrawer;
	}

}
