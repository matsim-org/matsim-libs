/* *********************************************************************** *
 * project: org.matsim.*
 * MyMapViewer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.evacuation.evacuationptlineseditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.control.ShapeFactory;
import org.matsim.contrib.evacuation.io.ConfigIO;
import org.matsim.contrib.evacuation.model.AbstractModule;
import org.matsim.contrib.evacuation.model.AbstractToolBox;
import org.matsim.contrib.evacuation.model.Constants;
import org.matsim.contrib.evacuation.model.shape.BoxShape;
import org.matsim.contrib.evacuation.model.shape.Shape;

public class PTLToolBox extends AbstractToolBox {

	private static final long serialVersionUID = 1L;
	private JTextField blockFieldLink1hh;
	private JTextField blockFieldLink1mm;
	private JButton openBtn;
	private JButton saveButton;
	private JPanel compositePanel;

	// STRING COMMANDS
	public static final String RED = "LINK_SELECT_RED";
	public static final String GREEN = "LINK_SELECT_GREEN";

	private Id<Link> currentLinkIdRed = null;
	private Id<Link> currentLinkIdGreen = null;
	private JPanel busStopConfigPanel;
	private JButton blockButtonOK;
	private final Map<Id<Link>, BusStop> busStops = new HashMap<>();
	private JRadioButton redLinkSelct;
	private JRadioButton greenLinkSelct;
	private JSpinner numDepSpinner;
	private JSpinner capSpinner;
	private JCheckBox circCheck;
	private JSpinner numVehSpinner;
	private BusStop currentBusStop;

	private void setBusStopEditorPanelEnabled(boolean toggle) {
		if (!toggle) {
			this.greenLinkSelct.setEnabled(toggle);
			this.redLinkSelct.setEnabled(toggle);
		}
		this.blockFieldLink1hh.setEnabled(toggle);
		this.blockFieldLink1mm.setEnabled(toggle);
		this.numDepSpinner.setEnabled(toggle);
		this.capSpinner.setEnabled(toggle);
		this.circCheck.setEnabled(toggle);
		this.blockButtonOK.setEnabled(toggle);
		this.blockButtonRemove.setEnabled(toggle);
	}

	private JButton blockButtonRemove;

	public PTLToolBox(AbstractModule module, Controller controller) {
		super(module, controller);

		this.setLayout(new BorderLayout());

		JPanel panel = new JPanel();
		this.add(panel, BorderLayout.SOUTH);

		this.busStopConfigPanel = new JPanel(new GridLayout(18, 2));

		this.blockFieldLink1hh = new JTextField("0");
		this.blockFieldLink1mm = new JTextField("1");
		this.blockButtonOK = new JButton(locale.btOK());
		this.blockButtonRemove = new JButton(locale.btRemove());

		this.busStopConfigPanel.setSize(new Dimension(200, 200));

		// add hour / minute input check listeners
		this.blockFieldLink1hh.addKeyListener(new TypeHour());
		this.blockFieldLink1mm.addKeyListener(new TypeMinute());

		this.blockFieldLink1hh.addFocusListener(new CheckHour());
		this.blockFieldLink1mm.addFocusListener(new CheckMinute());

		JPanel ptButtons = new JPanel();

		ptButtons.add(this.blockButtonOK);
		ptButtons.add(this.blockButtonRemove);

		this.blockButtonOK.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				PTLToolBox.this.setBusStopEditorPanelEnabled(false);
				PTLToolBox.this.numVehSpinner.setEnabled(false);
				PTLToolBox.this.currentBusStop.hh = PTLToolBox.this.blockFieldLink1hh
						.getText();
				PTLToolBox.this.currentBusStop.mm = PTLToolBox.this.blockFieldLink1mm
						.getText();
				PTLToolBox.this.currentBusStop.numDepSpinnerValue = PTLToolBox.this.numDepSpinner
						.getValue();
				PTLToolBox.this.currentBusStop.capSpinnerValue = PTLToolBox.this.capSpinner
						.getValue();
				PTLToolBox.this.currentBusStop.circCheckSelected = PTLToolBox.this.circCheck
						.isSelected();
				PTLToolBox.this.currentBusStop.numVehSpinnerValue = PTLToolBox.this.numVehSpinner
						.getValue();
				PTLToolBox.this.currentBusStop.id = PTLToolBox.this.redLinkSelct
						.isSelected() ? PTLToolBox.this.currentLinkIdRed
						: PTLToolBox.this.currentLinkIdGreen;

				PTLToolBox.this.busStops.put(PTLToolBox.this.currentBusStop.id,
						PTLToolBox.this.currentBusStop);

				Link link = PTLToolBox.this.controller.getScenario()
						.getNetwork().getLinks().get(currentBusStop.id);
				Point2D linkPos = PTLToolBox.this.controller.coordToPoint(link
						.getCoord());

				int secondaryLayerID = PTLToolBox.this.controller
						.getVisualizer().getSecondaryShapeRenderLayer().getId();

				BoxShape shape = ShapeFactory.getBusStopShape(
						currentBusStop.id.toString(), secondaryLayerID, linkPos);
				shape.setVisible(true);
				PTLToolBox.this.controller.addShape(shape);
				PTLToolBox.this.controller.getVisualizer()
						.getSecondaryShapeRenderLayer()
						.updatePixelCoordinates(shape);
				PTLToolBox.this.controller.paintLayers();

				PTLToolBox.this.saveButton.setEnabled(true);

				// TODO
				// PTLToolBox.this.jMapViewer.addBusStop(PTLToolBox.this.currentBusStop.id);

			}
		});

		this.blockButtonRemove.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if ((PTLToolBox.this.currentBusStop.id != null)
						&& PTLToolBox.this.busStops
								.containsKey(PTLToolBox.this.currentBusStop.id)) {
					PTLToolBox.this.busStops
							.remove(PTLToolBox.this.currentBusStop.id);
					PTLToolBox.this.controller
							.removeShape(Constants.ID_BUSSTOP_PREFIX
									+ PTLToolBox.this.currentBusStop.id
											.toString());
					PTLToolBox.this.controller.paintLayers();

					PTLToolBox.this.blockFieldLink1hh.setText("0");
					PTLToolBox.this.blockFieldLink1mm.setText("1");
					PTLToolBox.this.numDepSpinner.setValue(1);
					PTLToolBox.this.capSpinner.setValue(1);
					PTLToolBox.this.numVehSpinner.setValue(1);
					PTLToolBox.this.circCheck.setSelected(false);
					PTLToolBox.this.setLink1Id(null);
					PTLToolBox.this.setLink2Id(null);

					Shape shape = PTLToolBox.this.controller
							.getShapeById(Constants.ID_LINK_PRIMARY);
					if (shape != null)
						shape.setVisible(false);
					shape = PTLToolBox.this.controller
							.getShapeById(Constants.ID_LINK_SECONDARY);
					if (shape != null)
						shape.setVisible(false);

					PTLToolBox.this.controller.paintLayers();
					PTLToolBox.this.updateMask();

				}

			}
		});

		JPanel panelLink1 = new JPanel(new GridLayout(1, 3));

		this.redLinkSelct = new JRadioButton();
		this.redLinkSelct.setActionCommand(RED);
		this.redLinkSelct.setSelected(true);
		this.redLinkSelct.addActionListener(this);
		this.redLinkSelct.setEnabled(false);

		this.greenLinkSelct = new JRadioButton();
		this.greenLinkSelct.setActionCommand(GREEN);
		this.greenLinkSelct.setSelected(false);
		this.greenLinkSelct.addActionListener(this);
		this.greenLinkSelct.setEnabled(false);

		ButtonGroup group = new ButtonGroup();
		group.add(this.greenLinkSelct);
		group.add(this.redLinkSelct);

		JPanel redPanel = new JPanel();
		redPanel.setBackground(Color.RED);
		redPanel.add(this.redLinkSelct);

		JPanel greenPanel = new JPanel();
		greenPanel.setBackground(Color.GREEN);
		greenPanel.add(this.greenLinkSelct);

		panelLink1.add(new JLabel("direction"));
		panelLink1.add(greenPanel);
		panelLink1.add(redPanel);

		this.busStopConfigPanel.add(panelLink1);
		this.busStopConfigPanel.add(new JSeparator());

		this.busStopConfigPanel.add(new JLabel("first departure"));
		JPanel depPanel = new JPanel(new GridLayout(1, 3));
		depPanel.add(new JLabel("hh:mm"));
		depPanel.add(this.blockFieldLink1hh);
		depPanel.add(this.blockFieldLink1mm);
		this.busStopConfigPanel.add(depPanel);
		this.busStopConfigPanel.add(new JSeparator());

		JPanel numDepPanel = new JPanel(new GridLayout(1, 2));
		numDepPanel.add(new JLabel("#departures"));
		SpinnerNumberModel spm1 = new SpinnerNumberModel(0, 0, 100, 1);
		this.numDepSpinner = new JSpinner(spm1);
		numDepPanel.add(this.numDepSpinner);
		this.numDepSpinner.setEnabled(false);
		this.busStopConfigPanel.add(numDepPanel);
		this.busStopConfigPanel.add(new JSeparator());
		numDepSpinner.setValue(1);

		JPanel capPanel = new JPanel(new GridLayout(1, 2));
		capPanel.add(new JLabel("capacity/vehicle"));
		SpinnerNumberModel spm2 = new SpinnerNumberModel(0, 0, 100, 1);
		this.capSpinner = new JSpinner(spm2);
		capPanel.add(this.capSpinner);
		this.capSpinner.setEnabled(false);
		this.busStopConfigPanel.add(capPanel);
		this.busStopConfigPanel.add(new JSeparator());
		capSpinner.setValue(1);

		JPanel circPanel = new JPanel(new GridLayout(1, 2));
		circPanel.add(new JLabel("circling"));
		this.circCheck = new JCheckBox();
		circPanel.add(this.circCheck);
		this.circCheck.setEnabled(false);
		this.busStopConfigPanel.add(circPanel);

		this.circCheck.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (PTLToolBox.this.circCheck.isSelected()) {
					PTLToolBox.this.numVehSpinner.setEnabled(true);
					numVehSpinner.setValue(1);
				} else {
					PTLToolBox.this.numVehSpinner.setEnabled(false);
				}
			}
		});

		JPanel numVehPanel = new JPanel(new GridLayout(1, 2));
		numVehPanel.add(new JLabel("#vehicles"));
		SpinnerNumberModel spm3 = new SpinnerNumberModel(0, 0, 100, 1);
		this.numVehSpinner = new JSpinner(spm3);
		numVehPanel.add(this.numVehSpinner);
		this.numVehSpinner.setEnabled(false);
		this.busStopConfigPanel.add(numVehPanel);

		// circling is disabled for now
		circPanel.setVisible(false);
		numVehPanel.setVisible(false);

		this.busStopConfigPanel.add(new JSeparator());

		this.busStopConfigPanel.add(ptButtons);

		this.busStopConfigPanel.setPreferredSize(new Dimension(300, 300));

		this.busStopConfigPanel.setBorder(BorderFactory
				.createLineBorder(Color.black));

		this.blockFieldLink1hh.setEnabled(false);
		this.blockFieldLink1mm.setEnabled(false);

		this.blockButtonOK.setEnabled(false);

		this.add(this.busStopConfigPanel, BorderLayout.EAST);

		this.openBtn = new JButton(locale.btOpen());

		if (this.controller.isStandAlone())
			panel.add(this.openBtn);

		this.saveButton = new JButton(locale.btSave());
		this.saveButton.setEnabled(false);
		this.saveButton.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(this.saveButton);

		this.compositePanel = new JPanel();
		this.compositePanel.setBounds(new Rectangle(0, 0, 800, 800));
		this.add(this.compositePanel, BorderLayout.CENTER);
		this.compositePanel.setLayout(new BorderLayout(0, 0));

		this.openBtn.addActionListener(this);
		this.saveButton.addActionListener(this);

	}

	class TypeHour implements KeyListener {

		@Override
		public void keyTyped(KeyEvent e) {
			if (!Character.toString(e.getKeyChar()).matches("[0-9]"))
				e.consume();
		}

		@Override
		public void keyReleased(KeyEvent e) {

			JTextField src = (JTextField) e.getSource();

			String text = src.getText();

			if (!text.matches("([01]?[0-9]|2[0-3])"))
				src.setText("00");

		}

		@Override
		public void keyPressed(KeyEvent e) {

		}
	}

	class CheckHour implements FocusListener {

		@Override
		public void focusGained(FocusEvent e) {
			JTextField src = (JTextField) e.getSource();
			src.setSelectionStart(0);
			src.setSelectionEnd(src.getText().length());

		}

		@Override
		public void focusLost(FocusEvent e) {
			JTextField src = (JTextField) e.getSource();
			String text = src.getText();

			if (!text.matches("([01]?[0-9]|2[0-3])"))
				src.setText("00");
			else if (text.matches("[0-9]"))
				src.setText("0" + text);

		}

	}

	class CheckMinute implements FocusListener {

		@Override
		public void focusGained(FocusEvent e) {
			JTextField src = (JTextField) e.getSource();
			src.setSelectionStart(0);
			src.setSelectionEnd(src.getText().length());

		}

		@Override
		public void focusLost(FocusEvent e) {
			JTextField src = (JTextField) e.getSource();

			String text = src.getText();

			if ((!text.matches("[0-5][0-9]")) && (!text.matches("[0-9]")))
				src.setText("00");
			else if (text.matches("[0-9]"))
				src.setText("0" + text);

		}

	}

	class TypeMinute implements KeyListener {

		@Override
		public void keyTyped(KeyEvent e) {
			if (!Character.toString(e.getKeyChar()).matches("[0-9]"))
				e.consume();
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

		@Override
		public void keyPressed(KeyEvent e) {
		}
	}

	@Override
	public void updateMask() {

		Id<Link> linkId1 = this.controller.getTempLinkId(0);
		Id<Link> linkId2 = this.controller.getTempLinkId(1);

		setLink1Id(linkId1);
		setLink2Id(linkId2);

		setBusStopEditorPanelEnabled(this.controller.isEditMode());

	}

	/**
	 * save and open events
	 * 
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == "LINK_SELECT_RED") {
			BusStop bs = this.busStops.get(this.currentLinkIdRed);
			if (bs != null) {
				this.currentBusStop = bs;
			} else {
				bs = new BusStop();
				this.busStops.put(this.currentLinkIdRed, bs);
			}
			updateControlPanel(bs);

		} else if (e.getActionCommand() == "LINK_SELECT_GREEN") {
			BusStop bs = this.busStops.get(this.currentLinkIdGreen);
			if (bs != null) {
				this.currentBusStop = bs;
			} else {
				bs = new BusStop();
				this.busStops.put(this.currentLinkIdGreen, bs);
			}
			updateControlPanel(bs);
		} else if (e.getActionCommand() == locale.btSave()) {
			createAndSavePTLines();
		} else if (e.getActionCommand() == locale.btOpen()) {
			// TODO default open
		}

	}

	private void createAndSavePTLines() {
		ConfigIO.savePTLines(this.controller, this.busStops);
		// TODO confirmation dialog
	}

	/**
	 * set id #1 of the first selected link (in gui and object data) checks if
	 * there is any data for the link prior to this selection
	 * 
	 * @param id
	 */
	public void setLink1Id(Id<Link> id) {
		this.currentLinkIdRed = id;
		if (id != null) {
			if (this.busStops.containsKey(id)) {
				BusStop bs = this.busStops.get(id);
				this.currentBusStop = bs;
				updateControlPanel(bs);
			} else {

				this.currentBusStop = new BusStop();
				this.currentBusStop.id = id;
				this.busStops.put(id, this.currentBusStop);
				updateControlPanel(this.currentBusStop);
			}

			this.redLinkSelct.setSelected(true);
			this.greenLinkSelct.setSelected(false);
		}
		this.redLinkSelct.setEnabled(id != null);
	}

	private void updateControlPanel(BusStop bs) {
		if (bs != null) {
			this.currentBusStop = bs;
			this.blockFieldLink1hh.setText(bs.hh);
			this.blockFieldLink1mm.setText(bs.mm);
			this.numDepSpinner.setValue(bs.numDepSpinnerValue);
			this.capSpinner.setValue(bs.capSpinnerValue);
			this.numVehSpinner.setValue(bs.numVehSpinnerValue);
			this.circCheck.setSelected(bs.circCheckSelected);
		}

	}

	/**
	 * set id #2 of the second selected link (in gui and object data) checks if
	 * there is any data for the link prior to this selection
	 * 
	 * @param id
	 */
	public void setLink2Id(Id<Link> id) {
		this.currentLinkIdGreen = id;
		this.greenLinkSelct.setEnabled(id != null);
	}

}
