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
import java.awt.event.KeyEvent;
import java.text.MessageFormat;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.interfaces.OTFServer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;

public final class OTFControlBar extends JToolBar {

	private static final long serialVersionUID = 1L;

	private final JButton playPauseButton;

	private final JFormattedTextField timeField;

	private final JFormattedTextField scaleField;

	private final OTFHostControl hostControl;

	private boolean synchronizedPlay = true;

	private final ImageIcon playIcon;

	private final ImageIcon pauseIcon;

	private OTFAbortGoto progressBar = null;

	private enum PlayOrPause {PLAY, PAUSE}

	private final OTFOGLDrawer drawer;

	/*
	 * The symbol which the play/pause button currently shows.
	 * (NOT the mode we are in!)
	 */
	private PlayOrPause playOrPauseButtonMode = PlayOrPause.PLAY;

	private final OTFServer server;

	public OTFControlBar(OTFServer server, final OTFHostControl hostControl, OTFOGLDrawer mainDrawer) {
		this.server = server;
		this.hostControl = hostControl;
		this.drawer = mainDrawer;
		this.setFloatable(false);

		playIcon = new ImageIcon(MatsimResource.getAsImage("otfvis/buttonPlay.png"), "Play");
		pauseIcon = new ImageIcon(MatsimResource.getAsImage("otfvis/buttonPause.png"), "Pause");

		if (!server.isLive()) {
			JButton restartButton = new JButton();
			restartButton.putClientProperty("JButton.buttonType", "icon");
			restartButton.addActionListener(e -> {
				OTFControlBar.this.hostControl.toStart();
				repaint();
			});
			restartButton.setToolTipText("restart the server/simulation");
			String imgLocation2 = "otfvis/" + "buttonRestart" + ".png";
			ImageIcon icon2 = new ImageIcon(MatsimResource.getAsImage(imgLocation2), "Restart");
			restartButton.setIcon(icon2);
			add(restartButton);
			JButton multiStepsBackwardButton = new JButton();
			multiStepsBackwardButton.putClientProperty("JButton.buttonType", "icon");
			multiStepsBackwardButton.addActionListener(e -> {
				int bigStep = OTFClientControl.getInstance().getOTFVisConfig().getBigTimeStep();
				hostControl.requestTimeStep(OTFControlBar.this.hostControl.getSimTime() - bigStep);
			});
			multiStepsBackwardButton.setToolTipText("go several timesteps backwards");
			String imgLocation1 = "otfvis/" + "buttonStepBB" + ".png";
			ImageIcon icon1 = new ImageIcon(MatsimResource.getAsImage(imgLocation1), "<<");
			multiStepsBackwardButton.setIcon(icon1);
			add(multiStepsBackwardButton);
			JButton singleStepBackwardButton = new JButton();
			singleStepBackwardButton.putClientProperty("JButton.buttonType", "icon");
			singleStepBackwardButton.addActionListener(
					e -> hostControl.requestTimeStep(OTFControlBar.this.hostControl.getSimTime() - 1));
			singleStepBackwardButton.setToolTipText("go one timestep backwards");
			String imgLocation = "otfvis/" + "buttonStepB" + ".png";
			ImageIcon icon = new ImageIcon(MatsimResource.getAsImage(imgLocation), "<");
			singleStepBackwardButton.setIcon(icon);
			add(singleStepBackwardButton);
		}

		JButton playOrPauseButton = new JButton();
		playOrPauseButton.putClientProperty("JButton.buttonType", "icon");
		playOrPauseButton.setToolTipText("press to play simulation continuously");
		String imgLocation2 = "otfvis/" + "buttonPlay" + ".png";
		ImageIcon icon2 = new ImageIcon(MatsimResource.getAsImage(imgLocation2), "PLAY");
		playOrPauseButton.setIcon(icon2);
		this.playPauseButton = playOrPauseButton;
		add(this.playPauseButton);
		JButton singleStepForwardButton = new JButton();
		singleStepForwardButton.putClientProperty("JButton.buttonType", "icon");
		singleStepForwardButton.addActionListener(
				e -> hostControl.requestTimeStep(OTFControlBar.this.hostControl.getSimTime() + 1));
		singleStepForwardButton.setToolTipText("go one timestep forward");
		String imgLocation1 = "otfvis/" + "buttonStepF" + ".png";
		ImageIcon icon1 = new ImageIcon(MatsimResource.getAsImage(imgLocation1), ">");
		singleStepForwardButton.setIcon(icon1);
		add(singleStepForwardButton);
		JButton multiStepForwardButton = new JButton();
		multiStepForwardButton.putClientProperty("JButton.buttonType", "icon");
		multiStepForwardButton.addActionListener(e -> {
			int bigStep = OTFClientControl.getInstance().getOTFVisConfig().getBigTimeStep();
			hostControl.requestTimeStep(OTFControlBar.this.hostControl.getSimTime() + bigStep);
		});
		multiStepForwardButton.setToolTipText("go several timesteps forward");
		String imgLocation = "otfvis/" + "buttonStepFF" + ".png";
		ImageIcon icon = new ImageIcon(MatsimResource.getAsImage(imgLocation), ">>");
		multiStepForwardButton.setIcon(icon);
		add(multiStepForwardButton);

		playOrPauseButton.addActionListener(e -> {
			if (playOrPauseButtonMode == PlayOrPause.PLAY) {
				this.playPauseButton.setIcon(pauseIcon);
				//PlayPauseSimulationControl.doStep() cannot be called while playing in async mode
				singleStepForwardButton.setEnabled(synchronizedPlay);
				multiStepForwardButton.setEnabled(synchronizedPlay);
				OTFControlBar.this.hostControl.play(synchronizedPlay);
				playOrPauseButtonMode = PlayOrPause.PAUSE;
			} else if (playOrPauseButtonMode == PlayOrPause.PAUSE) {
				this.playPauseButton.setIcon(playIcon);
				OTFControlBar.this.hostControl.pause();
				playOrPauseButtonMode = PlayOrPause.PLAY;
				singleStepForwardButton.setEnabled(true);
				multiStepForwardButton.setEnabled(true);
			}
		});

		MessageFormat format = new MessageFormat("{0,number,00}:{1,number,00}:{2,number,00}");
		timeField = new JFormattedTextField(format);
		timeField.setMaximumSize(new Dimension(100, 30));
		timeField.setMinimumSize(new Dimension(80, 30));
		timeField.setHorizontalAlignment(JTextField.CENTER);
		add(timeField);
		timeField.addActionListener(e -> {
			final int newTime_s = (int)Time.parseTime(timeField.getText());
			progressBar = new OTFAbortGoto(OTFControlBar.this.server, newTime_s);
			progressBar.start();
			new Thread(() -> {
				OTFControlBar.this.hostControl.requestTimeStep(newTime_s);
				progressBar.terminate = true;
			}).start();
		});
		hostControl.getSimTimeModel()
				.addChangeListener(e -> SwingUtilities.invokeLater(
						() -> timeField.setText(Time.writeTime(hostControl.getSimTime()))));
		timeField.setText(Time.writeTime(hostControl.getSimTime()));

		if (this.server.isLive()) {
			final JCheckBox synchBox = new JCheckBox("Synch");
			synchBox.setMnemonic(KeyEvent.VK_V);
			synchBox.setSelected(synchronizedPlay);
			synchBox.addItemListener(e -> {
				OTFControlBar.this.hostControl.pause();
				synchronizedPlay = synchBox.isSelected();
				if (playOrPauseButtonMode == PlayOrPause.PAUSE) { // means: playing
					//PlayPauseSimulationControl.doStep() cannot be called while playing in async mode
					singleStepForwardButton.setEnabled(synchronizedPlay);
					multiStepForwardButton.setEnabled(synchronizedPlay);
					OTFControlBar.this.hostControl.play(synchronizedPlay);
				}
			});
			add(synchBox);
		}

		JLabel lab = new JLabel("Scale: ");
		lab.setMaximumSize(new Dimension(100, 30));
		lab.setMinimumSize(new Dimension(80, 30));
		lab.setHorizontalAlignment(JLabel.RIGHT);
		add(lab);
		scaleField = new JFormattedTextField("1.0");
		scaleField.setMaximumSize(new Dimension(50, 30));
		scaleField.setMinimumSize(new Dimension(30, 30));
		scaleField.setHorizontalAlignment(JTextField.CENTER);
		add(scaleField);
		scaleField.addActionListener(e -> drawer.setScale(Float.parseFloat(scaleField.getText())));
		drawer.addChangeListener(e -> updateScaleField());
		updateScaleField();
	}

	private void updateScaleField() {
		scaleField.setText(String.valueOf((double)((float)(Math.round(drawer.getScale() * 100)) / 100)));
	}

}
