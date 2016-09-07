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

import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.interfaces.OTFServer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;

public final class OTFControlBar extends JToolBar {

	private static final long serialVersionUID = 1L;

	private JButton playPauseButton;

	private JFormattedTextField timeField;

	private JFormattedTextField scaleField;

	private final OTFHostControl hostControl;

    private boolean synchronizedPlay = true;

	private ImageIcon playIcon = null;

	private ImageIcon pauseIcon = null;

	private OTFAbortGoto progressBar = null;

	private enum PlayOrPause {PLAY, PAUSE}

    private final OTFOGLDrawer drawer;

	/*
	 * The symbol which the play/pause button currently shows.
	 * (NOT the mode we are in!)
	 */
	private PlayOrPause playOrPause = PlayOrPause.PLAY;

	private OTFServer server;

	public OTFControlBar(OTFServer server, final OTFHostControl hostControl, OTFOGLDrawer mainDrawer)  {
		this.server = server;
		this.hostControl = hostControl;
		this.drawer = mainDrawer;
		this.setFloatable(false);

		playIcon = new ImageIcon(MatsimResource.getAsImage("otfvis/buttonPlay.png"), "Play");
		pauseIcon = new ImageIcon(MatsimResource.getAsImage("otfvis/buttonPause.png"), "Pause");

		if (!server.isLive()) {
			JButton button2 = new JButton();
			button2.putClientProperty("JButton.buttonType","icon");
			button2.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					OTFControlBar.this.hostControl.toStart();
					repaint();
				}
			});
			button2.setToolTipText("restart the server/simulation");
			String imgLocation2 = "otfvis/" + "buttonRestart" + ".png";
			ImageIcon icon2 = new ImageIcon(MatsimResource.getAsImage(imgLocation2), "Restart");
			button2.setIcon(icon2);
			add(button2);
			JButton button1 = new JButton();
			button1.putClientProperty("JButton.buttonType","icon");
			button1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int bigStep = OTFClientControl.getInstance().getOTFVisConfig().getBigTimeStep();
					hostControl.requestTimeStep(OTFControlBar.this.hostControl.getSimTime() - bigStep);
				}
			});
			button1.setToolTipText("go several timesteps backwards");
			String imgLocation1 = "otfvis/" + "buttonStepBB" + ".png";
			ImageIcon icon1 = new ImageIcon(MatsimResource.getAsImage(imgLocation1), "<<");
			button1.setIcon(icon1);
			add(button1);
			JButton button = new JButton();
			button.putClientProperty("JButton.buttonType","icon");
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					hostControl.requestTimeStep(OTFControlBar.this.hostControl.getSimTime() - 1);
				}
			});
			button.setToolTipText("go one timestep backwards");
			String imgLocation = "otfvis/" + "buttonStepB" + ".png";
			ImageIcon icon = new ImageIcon(MatsimResource.getAsImage(imgLocation), "<");
			button.setIcon(icon);
			add(button);
		}

		JButton button2 = new JButton();
		button2.putClientProperty("JButton.buttonType","icon");
		button2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (playOrPause == PlayOrPause.PLAY) {
					playPauseButton.setIcon(pauseIcon);
					OTFControlBar.this.hostControl.play(synchronizedPlay);
					playOrPause = PlayOrPause.PAUSE;
				} else if (playOrPause == PlayOrPause.PAUSE) {
					playPauseButton.setIcon(playIcon);
					OTFControlBar.this.hostControl.pause();
					playOrPause = PlayOrPause.PLAY;
				}
			}
		});
		button2.setToolTipText("press to play simulation continuously");
		String imgLocation2 = "otfvis/" + "buttonPlay" + ".png";
		ImageIcon icon2 = new ImageIcon(MatsimResource.getAsImage(imgLocation2), "PLAY");
		button2.setIcon(icon2);
		playPauseButton = button2;
		add(playPauseButton);
		JButton button1 = new JButton();
		button1.putClientProperty("JButton.buttonType","icon");
		button1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				hostControl.requestTimeStep(OTFControlBar.this.hostControl.getSimTime() + 1);
			}
		});
		button1.setToolTipText("go one timestep forward");
		String imgLocation1 = "otfvis/" + "buttonStepF" + ".png";
		ImageIcon icon1 = new ImageIcon(MatsimResource.getAsImage(imgLocation1), ">");
		button1.setIcon(icon1);
		add(button1);
		JButton button = new JButton();
		button.putClientProperty("JButton.buttonType","icon");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int bigStep = OTFClientControl.getInstance().getOTFVisConfig().getBigTimeStep();
				hostControl.requestTimeStep(OTFControlBar.this.hostControl.getSimTime() + bigStep);
			}
		});
		button.setToolTipText("go several timesteps forward");
		String imgLocation = "otfvis/" + "buttonStepFF" + ".png";
		ImageIcon icon = new ImageIcon(MatsimResource.getAsImage(imgLocation), ">>");
		button.setIcon(icon);
		add(button);
		MessageFormat format = new MessageFormat("{0,number,00}:{1,number,00}:{2,number,00}");
		timeField = new JFormattedTextField(format);
		timeField.setMaximumSize(new Dimension(100,30));
		timeField.setMinimumSize(new Dimension(80,30));
		timeField.setHorizontalAlignment(JTextField.CENTER);
		add( timeField );
		timeField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final int newTime_s = (int) Time.parseTime(timeField.getText());
				progressBar = new OTFAbortGoto(OTFControlBar.this.server, newTime_s);
				progressBar.start();
				new Thread() {
					@Override
					public void run() {
						OTFControlBar.this.hostControl.requestTimeStep(newTime_s);
						progressBar.terminate = true;
					}
				}.start();
			}
		});
		hostControl.getSimTimeModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						timeField.setText(Time.writeTime(hostControl.getSimTime()));
					}
				});
			}
		});
		timeField.setText(Time.writeTime(hostControl.getSimTime()));

		if (this.server.isLive()) {
			final JCheckBox synchBox = new JCheckBox("Synch");
			synchBox.setMnemonic(KeyEvent.VK_V);
			synchBox.setSelected(synchronizedPlay);
			synchBox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					OTFControlBar.this.hostControl.pause();
					synchronizedPlay = synchBox.isSelected();
					if (playOrPause == PlayOrPause.PAUSE) { // means: playing
						OTFControlBar.this.hostControl.play(synchronizedPlay);
					}
				}
			});
			add(synchBox);
		}

		JLabel lab = new JLabel("Scale: ");
		lab.setMaximumSize(new Dimension(100,30));
		lab.setMinimumSize(new Dimension(80,30));
		lab.setHorizontalAlignment(JLabel.RIGHT);
		add(lab);
		scaleField = new JFormattedTextField("1.0");
		scaleField.setMaximumSize(new Dimension(50,30));
		scaleField.setMinimumSize(new Dimension(30,30));
		scaleField.setHorizontalAlignment(JTextField.CENTER);
		add( scaleField );
		scaleField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				drawer.setScale(Float.parseFloat(scaleField.getText()));
			}
		});
		drawer.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				updateScaleField();
			}
		});
		updateScaleField();
	}

	private void updateScaleField() {
		scaleField.setText(String.valueOf((double) ((float)(Math.round(drawer.getScale()*100))/100)));
	}

}
