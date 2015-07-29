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

package org.matsim.contrib.evacuation.roadclosureseditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.control.ShapeFactory;
import org.matsim.contrib.evacuation.io.ConfigIO;
import org.matsim.contrib.evacuation.model.AbstractModule;
import org.matsim.contrib.evacuation.model.AbstractToolBox;
import org.matsim.contrib.evacuation.model.Constants;
import org.matsim.contrib.evacuation.model.shape.LineShape;

public class RCEToolBox extends AbstractToolBox {

	private static final long serialVersionUID = 1L;
	private JTextField blockFieldLink1hh;
	private JPanel blockPanel;
	private JTextField blockFieldLink1mm;
	private JTextField blockFieldLink2hh;
	private JTextField blockFieldLink2mm;
	private JButton blockButtonOK;
	private JCheckBox cbLink1;
	private JCheckBox cbLink2;
	private JPanel panelDescriptions;
	private JPanel panelLink1;
	private JPanel panelLink2;
	private JButton openBtn;
	private JButton saveBtn;

	private boolean saveLink1;
	private boolean saveLink2;
	private boolean blockButtonOkClicked;

	private Id<Link> currentLinkId1 = null;
	private Id<Link> currentLinkId2 = null;
	private HashMap<Id<Link>, String> roadClosures;

	public RCEToolBox(AbstractModule module, Controller controller) {
		super(module, controller);

		this.setLayout(new BorderLayout());

		JPanel panel = new JPanel();
		this.add(panel, BorderLayout.SOUTH);

		this.blockPanel = new JPanel(new GridLayout(18, 2));

		this.blockFieldLink1hh = new JTextField("0");
		this.blockFieldLink1mm = new JTextField("0");
		this.blockFieldLink2hh = new JTextField("0");
		this.blockFieldLink2mm = new JTextField("0");
		this.blockButtonOK = new JButton(locale.btOK());

		this.blockPanel.setSize(new Dimension(200, 200));

		// add hour / minute input check listeners
		this.blockFieldLink1hh.addKeyListener(new TypeHour());
		this.blockFieldLink1mm.addKeyListener(new TypeMinute());
		this.blockFieldLink2hh.addKeyListener(new TypeHour());
		this.blockFieldLink2mm.addKeyListener(new TypeMinute());
		this.blockFieldLink1hh.addFocusListener(new CheckHour());
		this.blockFieldLink1mm.addFocusListener(new CheckMinute());
		this.blockFieldLink2hh.addFocusListener(new CheckHour());
		this.blockFieldLink2mm.addFocusListener(new CheckMinute());

		this.cbLink1 = new JCheckBox("link 1");
		this.cbLink2 = new JCheckBox("link 2");

		this.roadClosures = new HashMap<>();

		this.blockButtonOK.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateRoadClosure();
				blockButtonOkClicked = true;
				saveBtn.setEnabled(true);
			}
		});

		this.cbLink1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				RCEToolBox.this.saveLink1 = !RCEToolBox.this.saveLink1;

				if (RCEToolBox.this.saveLink1) {
					RCEToolBox.this.blockFieldLink1hh.setEnabled(true);
					RCEToolBox.this.blockFieldLink1mm.setEnabled(true);

				} else {
					RCEToolBox.this.blockFieldLink1hh.setEnabled(false);
					RCEToolBox.this.blockFieldLink1mm.setEnabled(false);
				}
			}
		});

		this.cbLink2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				RCEToolBox.this.saveLink2 = !RCEToolBox.this.saveLink2;

				if (RCEToolBox.this.saveLink2) {
					RCEToolBox.this.blockFieldLink2hh.setEnabled(true);
					RCEToolBox.this.blockFieldLink2mm.setEnabled(true);
				} else {
					RCEToolBox.this.blockFieldLink2hh.setEnabled(false);
					RCEToolBox.this.blockFieldLink2mm.setEnabled(false);
				}

			}
		});

		this.cbLink1.setBackground(Color.red);
		this.cbLink2.setBackground(Color.green);

		this.cbLink1.setSelected(false);
		this.cbLink2.setSelected(false);

		this.panelDescriptions = new JPanel(new GridLayout(1, 3));
		this.panelLink1 = new JPanel(new GridLayout(1, 3));
		this.panelLink2 = new JPanel(new GridLayout(1, 3));

		this.panelDescriptions.add(new JLabel("Road ID"));
		this.panelDescriptions.add(new JLabel("HH"));
		this.panelDescriptions.add(new JLabel("MM"));

		this.panelLink1.add(this.cbLink1);
		this.panelLink1.add(this.blockFieldLink1hh);
		this.panelLink1.add(this.blockFieldLink1mm);
		this.panelLink1.setBackground(Color.red);

		this.panelLink2.add(this.cbLink2);
		this.panelLink2.add(this.blockFieldLink2hh);
		this.panelLink2.add(this.blockFieldLink2mm);
		this.panelLink2.setBackground(Color.green);

		this.blockPanel.add(this.panelDescriptions);
		this.blockPanel.add(this.panelLink1);
		this.blockPanel.add(this.panelLink2);
		this.blockPanel.add(this.blockButtonOK);

		this.blockPanel.setPreferredSize(new Dimension(300, 300));

		this.blockPanel.setBorder(BorderFactory.createLineBorder(Color.black));

		this.cbLink1.setEnabled(false);
		this.cbLink2.setEnabled(false);

		this.blockFieldLink1hh.setEnabled(false);
		this.blockFieldLink1mm.setEnabled(false);
		this.blockFieldLink2hh.setEnabled(false);
		this.blockFieldLink2mm.setEnabled(false);

		this.blockButtonOK.setEnabled(false);

		// this.blockFieldLink1hh.setSelectedTextColor(Color.red);
		// this.blockFieldLink1mm.setSelectedTextColor(Color.green);

		this.add(this.blockPanel, BorderLayout.CENTER);

		this.openBtn = new JButton(locale.btOpen());

		if (this.controller.isStandAlone())
			panel.add(this.openBtn);

		this.saveBtn = new JButton(locale.btSave());
		this.saveBtn.setEnabled(false); 
		this.saveBtn.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(this.saveBtn);
		// this.add(this.compositePanel, BorderLayout.CENTER);

		this.openBtn.addActionListener(this);
		this.saveBtn.addActionListener(this);
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

	public void updateRoadClosure() {
		String shapeID;
		int secondaryLayerID = this.controller.getVisualizer()
				.getSecondaryShapeRenderLayer().getId();

		if ((this.currentLinkId1 != null)) {
			if (this.cbLink1.isSelected()) {
				if (this.roadClosures.get(this.currentLinkId1) == null) {
					Link link = this.controller.getScenario().getNetwork()
							.getLinks().get(currentLinkId1);
					Point2D c0 = this.controller.coordToPoint(link
							.getFromNode().getCoord());
					Point2D c1 = this.controller.coordToPoint(link.getToNode()
							.getCoord());
					LineShape shape = ShapeFactory
							.getRoadClosureShape(secondaryLayerID,
									currentLinkId1.toString(), c0, c1);
					this.controller.addShape(shape);
					this.controller.getVisualizer()
							.getSecondaryShapeRenderLayer()
							.updatePixelCoordinates(shape);
				}
				this.roadClosures.put(this.currentLinkId1,
						this.blockFieldLink1hh.getText() + ":"
								+ this.blockFieldLink1mm.getText());

			} else {
				shapeID = Constants.ID_ROADCLOSURE_PREFIX
						+ currentLinkId1.toString();

				if (this.roadClosures.get(this.currentLinkId1) != null) {
					this.controller.removeShape(shapeID);
				}

				this.roadClosures.remove(this.currentLinkId1);
			}

		}

		if ((this.currentLinkId2 != null)) {
			if (this.cbLink2.isSelected()) {
				if (this.roadClosures.get(this.currentLinkId2) == null) {
					Link link = this.controller.getScenario().getNetwork()
							.getLinks().get(currentLinkId2);
					Point2D c0 = this.controller.coordToPoint(link
							.getFromNode().getCoord());
					Point2D c1 = this.controller.coordToPoint(link.getToNode()
							.getCoord());
					LineShape shape = ShapeFactory
							.getRoadClosureShape(secondaryLayerID,
									currentLinkId2.toString(), c1, c0);
					this.controller.addShape(shape);
					this.controller.getVisualizer()
							.getSecondaryShapeRenderLayer()
							.updatePixelCoordinates(shape);
				}

				this.roadClosures.put(this.currentLinkId2,
						this.blockFieldLink2hh.getText() + ":"
								+ this.blockFieldLink2mm.getText());
			} else {
				shapeID = Constants.ID_ROADCLOSURE_PREFIX
						+ currentLinkId2.toString();
				if (this.roadClosures.get(this.currentLinkId2) != null) {
					this.controller.removeShape(shapeID);
				}

				this.roadClosures.remove(this.currentLinkId2);
			}
		}

		this.controller.paintLayers();
		// only enable Save Button if OK was clicked before
		boolean enableSave = blockButtonOkClicked && (this.roadClosures.size() > 0) ;
		this.saveBtn.setEnabled(enableSave);

	}

	@Override
	public void updateMask() {

		if (this.controller.isEditMode()) {
			this.cbLink1.setEnabled(true);
			this.cbLink2.setEnabled(true);
			this.blockButtonOK.setEnabled(true);

			setLink1Id(this.controller.getTempLinkId(0));
			setLink2Id(this.controller.getTempLinkId(1));

		} else {
			this.cbLink1.setEnabled(false);
			this.cbLink2.setEnabled(false);
			this.blockButtonOK.setEnabled(false);

			this.blockFieldLink1hh.setText("0");
			this.blockFieldLink1mm.setText("0");
			this.blockFieldLink1hh.setEnabled(false);
			this.blockFieldLink1mm.setEnabled(false);

			this.blockFieldLink2hh.setText("0");
			this.blockFieldLink2mm.setText("0");
			this.blockFieldLink2hh.setEnabled(false);
			this.blockFieldLink2mm.setEnabled(false);

			this.cbLink1.setSelected(false);
			this.cbLink2.setSelected(false);
			this.cbLink1.setText("-");
			this.cbLink2.setText("-");
			this.saveLink1 = false;
			this.saveLink2 = false;

		}

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == locale.btOpen()) {
			final JFileChooser fc = new JFileChooser();
			fc.setFileFilter(new FileFilter() {

				@Override
				public String getDescription() {
					return "MATSim config file";
				}

				@Override
				public boolean accept(File f) {
					if (f.isDirectory()) {
						return true;
					}
					if (f.getName().endsWith("xml")) {
						return true;
					}
					return false;
				}
			});

		} else if (e.getActionCommand() == locale.btSave()) {
			if (this.roadClosures.size() > 0) {
				boolean saved = ConfigIO.saveRoadClosures(controller,
						roadClosures);
				// TODO add confirmation dialog?

			}
		}
	}

	/**
	 * set id #1 of the first selected link (in gui and object data) checks if
	 * there is any data for the link prior to this selection
	 * 
	 * @param id
	 */
	public void setLink1Id(Id id) {
		if (id != null) {
			this.cbLink1.setText(id.toString());
			this.currentLinkId1 = id;

			if (this.roadClosures.containsKey(id)) {
				this.cbLink1.setSelected(true);
				this.blockFieldLink1hh.setEnabled(true);
				this.blockFieldLink1mm.setEnabled(true);

				this.blockFieldLink1hh.setText(this.roadClosures.get(id)
						.substring(0, 2));
				this.blockFieldLink1mm.setText(this.roadClosures.get(id)
						.substring(3, 5));

				this.saveLink1 = true;
			}
		} else {
			this.blockFieldLink1hh.setText("--");
			this.blockFieldLink1mm.setText("--");
			this.blockFieldLink1hh.setEnabled(false);
			this.blockFieldLink2hh.setEnabled(false);
			this.cbLink1.setEnabled(false);

		}

	}

	/**
	 * set id #2 of the second selected link (in gui and object data) checks if
	 * there is any data for the link prior to this selection
	 * 
	 * @param id
	 */
	public void setLink2Id(Id id) {
		if (id != null) {
			this.cbLink2.setText(id.toString());
			this.currentLinkId2 = id;

			if (this.roadClosures.containsKey(id)) {
				this.cbLink2.setSelected(true);
				this.blockFieldLink2hh.setEnabled(true);
				this.blockFieldLink2mm.setEnabled(true);

				this.blockFieldLink2hh.setText(this.roadClosures.get(id)
						.substring(0, 2));
				this.blockFieldLink2mm.setText(this.roadClosures.get(id)
						.substring(3, 5));

				this.saveLink2 = true;
			}
		} else {
			this.blockFieldLink2hh.setText("--");
			this.blockFieldLink2mm.setText("--");
			this.blockFieldLink2hh.setEnabled(false);
			this.blockFieldLink2mm.setEnabled(false);
			this.cbLink2.setEnabled(false);
		}

	}

}
