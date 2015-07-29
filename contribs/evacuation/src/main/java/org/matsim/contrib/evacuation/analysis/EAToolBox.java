/* *********************************************************************** *
c * project: org.matsim.*
 * RoadClosuresEditor.java
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

package org.matsim.contrib.evacuation.analysis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.evacuation.analysis.control.TiffExporter;
import org.matsim.contrib.evacuation.analysis.gui.AbstractDataPanel;
import org.matsim.contrib.evacuation.analysis.gui.EvacuationTimeGraphPanel;
import org.matsim.contrib.evacuation.analysis.gui.KeyPanel;
import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.model.AbstractToolBox;
import org.matsim.contrib.evacuation.model.Constants.Mode;
import org.matsim.contrib.evacuation.model.Constants.Unit;
import org.matsim.contrib.evacuation.view.DefaultSaveDialog;
import org.matsim.contrib.evacuation.view.renderer.GridRenderer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;

import com.vividsolutions.jts.geom.Polygon;

public class EAToolBox extends AbstractToolBox {
	private static final long serialVersionUID = 1L;

	private int exportSize;

	private static final Logger log = Logger.getLogger(EvacuationAnalysis.class);
	private JPanel compositePanel;
	private JButton saveButton;
	private JButton openBtn;
	private JButton openOTFVisBtn;
	private JPanel blockPanel;
	private Scenario sc;
	private String configFile;
	private Polygon areaPolygon;
	private double cellSize = 10;
	private AbstractDataPanel graphPanel;
	private JPanel controlPanel;
	private JButton calcButton;
	private ArrayList<File> eventFiles;
	private JComboBox iterationsList;
	private JSlider gridSizeSlider;
	private JComboBox modeList;
	private JSlider transparencySlider;
	private float cellTransparency;
	private String itersOutputDir;
	private boolean firstLoad;
	private Mode mode = Mode.EVACUATION;
	private KeyPanel keyPanel;
	private JLabel gridSizeLabel;
	private String cellSizeText = " number of cells: ";
	private boolean useCalculateButton = false;
	private GeotoolsTransformation ctInverse;

	private EvacuationAnalysis module;

	private GridRenderer gridRenderer;

	public EAToolBox(EvacuationAnalysis module, Controller controller) {
		super(module, controller);

		this.module = module;

		this.firstLoad = true;
		this.cellTransparency = .5f;

		// the panel on the right hand side, to display graphs etc.
		JPanel panel = new JPanel();
		this.blockPanel = new JPanel();
		this.blockPanel.setLayout(new BoxLayout(this.blockPanel, BoxLayout.Y_AXIS));
		this.blockPanel.setSize(new Dimension(400, 450));

		// ////////////////////////////////////////////////////////////////////////////
		// DESCRIPTIONS
		// ////////////////////////////////////////////////////////////////////////////

		// this.panelDescriptions = new JPanel(new GridLayout(1, 3));
		// this.panelDescriptions.add(new JLabel("graph"));

		// ////////////////////////////////////////////////////////////////////////////
		// PANELS
		// ////////////////////////////////////////////////////////////////////////////

		this.graphPanel = new EvacuationTimeGraphPanel(360, 280);
		this.keyPanel = new KeyPanel(this.mode, 360, 160);
		this.keyPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		this.controlPanel = new JPanel(new GridLayout(7, 3));
		this.controlPanel.setPreferredSize(new Dimension(360, 220));
		this.controlPanel.setSize(new Dimension(360, 220));

		this.blockPanel.add(graphPanel);
		this.blockPanel.add(keyPanel);
		this.blockPanel.add(controlPanel);
		this.blockPanel.add(panel);

		this.blockPanel.setPreferredSize(new Dimension(360, 700));
		this.blockPanel.setBorder(BorderFactory.createLineBorder(Color.black));

		this.compositePanel = new JPanel();
		this.compositePanel.setBounds(new Rectangle(0, 0, 800, 800));
		this.compositePanel.setLayout(new BorderLayout(0, 0));

		// ////////////////////////////////////////////////////////////////////////////
		// CONTROL
		// ////////////////////////////////////////////////////////////////////////////

		this.add(this.blockPanel, BorderLayout.EAST);
		this.add(this.compositePanel, BorderLayout.CENTER);

		this.openBtn = new JButton("Open");
		this.saveButton = new JButton("Save");
		// this.saveButton.setEnabled(false);
		this.saveButton.setHorizontalAlignment(SwingConstants.RIGHT);

		this.calcButton = new JButton("calculate");
		this.calcButton.setEnabled(false);
		this.calcButton.addActionListener(this);
		this.calcButton.setPreferredSize(new Dimension(100, 20));
		this.calcButton.setSize(new Dimension(100, 24));

		this.openOTFVisBtn = new JButton("OTFVis");
		this.openOTFVisBtn.setEnabled(false);
		this.openOTFVisBtn.addActionListener(this);
		this.openOTFVisBtn.setPreferredSize(new Dimension(100, 20));
		this.openOTFVisBtn.setSize(new Dimension(100, 24));

		JPanel iterationSelectionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		iterationSelectionPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		this.iterationsList = new JComboBox();
		this.iterationsList.addActionListener(this);
		this.iterationsList.setActionCommand("changeIteration");
		this.iterationsList.setPreferredSize(new Dimension(220, 24));
		iterationSelectionPanel.add(new JLabel(" event file: ", SwingConstants.RIGHT));
		iterationSelectionPanel.add(this.iterationsList);

		JPanel gridSizeSelectionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		gridSizeSelectionPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		this.gridSizeSlider = new JSlider(SwingConstants.HORIZONTAL, 10, 100, (int) this.cellSize);
		// this.gridSizeSlider.setMinorTickSpacing(2);
		this.gridSizeSlider.setPaintTicks(true);
		this.gridSizeSlider.setSnapToTicks(true);
		this.gridSizeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int value = ((JSlider) (e.getSource())).getValue();
				// value = value - (value % 10);
				updateCellSize(value);
			}
		});

		this.gridSizeSlider.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent arg0) {
				if (!useCalculateButton) {
					int value = ((JSlider) (arg0.getSource())).getValue();
					updateCellSize(value);
					//
					EAToolBox.this.module.runCalculation();
				}
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
			}

			@Override
			public void mouseClicked(MouseEvent arg0) {
			}
		});

		this.gridSizeSlider.setPreferredSize(new Dimension(220, 24));

		gridSizeLabel = new JLabel(cellSizeText + "10 ", SwingConstants.RIGHT);
		gridSizeSelectionPanel.add(gridSizeLabel);
		gridSizeSelectionPanel.add(this.gridSizeSlider);

		JPanel modeSelectionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		modeSelectionPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		this.modeList = new JComboBox();
		this.modeList.addItem(Mode.EVACUATION);
		this.modeList.addItem(Mode.CLEARING);
		this.modeList.addItem(Mode.UTILIZATION);
		this.modeList.setActionCommand("changeMode");
		this.modeList.addActionListener(this);
		this.modeList.setPreferredSize(new Dimension(220, 24));
		modeSelectionPanel.add(new JLabel(" mode: ", SwingConstants.RIGHT));
		modeSelectionPanel.add(this.modeList);

		JPanel calculateButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		calculateButtonPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 7));
		calculateButtonPanel.add(new JLabel(""));
		if (useCalculateButton)
			calculateButtonPanel.add(calcButton);

		calculateButtonPanel.setPreferredSize(new Dimension(220, 40));

		JPanel transparencySliderPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		transparencySliderPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		transparencySlider = new JSlider(SwingConstants.HORIZONTAL, 1, 100, 50);
		transparencySlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				updateTransparency((((JSlider) e.getSource()).getValue()) / 100f);
			}
		});
		transparencySlider.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent arg0) {
				// if (!useCalculateButton)
				// {
				EAToolBox.this.module.setCellTransparency((((JSlider) arg0.getSource()).getValue()) / 100f);
				EAToolBox.this.module.runCalculation();
				// }
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
			}

			@Override
			public void mouseClicked(MouseEvent arg0) {
			}
		});
		transparencySlider.setPreferredSize(new Dimension(220, 24));
		transparencySliderPanel.add(new JLabel(" cell transparency: ", SwingConstants.RIGHT));
		transparencySliderPanel.add(transparencySlider);

		this.controlPanel.add(new JLabel(""));
		this.controlPanel.add(iterationSelectionPanel);
		this.controlPanel.add(gridSizeSelectionPanel);
		this.controlPanel.add(modeSelectionPanel);
		this.controlPanel.add(calculateButtonPanel);
		this.controlPanel.add(new JSeparator());
		this.controlPanel.add(transparencySliderPanel);

		if (this.controller.isStandAlone())
			panel.add(this.openBtn);

		panel.add(this.saveButton);
		// panel.add(this.openOTFVisBtn); //TODO

		this.openBtn.addActionListener(this);
		this.saveButton.addActionListener(this);

		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentResized(ComponentEvent evt) {
				Component src = (Component) evt.getSource();
				Dimension newSize = src.getSize();
				updateMapViewerSize(newSize.width - 200, newSize.height);
			}

			@Override
			public void componentMoved(ComponentEvent e) {
			}

			@Override
			public void componentShown(ComponentEvent e) {
			}

			@Override
			public void componentHidden(ComponentEvent e) {
			}
		});

	}

	protected void updateCellSize(int value) {
		this.cellSize = (double) value;
		this.gridSizeLabel.setText(cellSizeText + value + " ");
		// this.gridSizeLabel.setText(cellSizeText + value + "m* ");
		this.module.setGridSize(value);

	}

	public void setGridRenderer(GridRenderer gridRenderer) {
		this.gridRenderer = gridRenderer;
	}

	public void updateTransparency(float transparency) {
		this.cellTransparency = transparency;
		this.gridRenderer.setTransparency(transparency);
	}

	/**
	 * save, open and (re)calculate events
	 * 
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		/**
		 * Save heatmap shape and graphs
		 * 
		 */
		if (e.getActionCommand() == "Save") {
			if (this.module.getEventHandler() != null) {

				this.gridRenderer.setEnabled(false);

				DefaultSaveDialog saveDialog = new DefaultSaveDialog(controller, "tiff", "tiff file (*.tiff)", false);

				int returnVal = saveDialog.showSaveDialog(this.controller.getParentComponent());

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					// save file
					File directory = saveDialog.getSelectedFile();
					final String sourceCRS = this.controller.getSourceCoordinateSystem();
					final String targetCRS = this.controller.getTargetCoordinateSystem();
					this.ctInverse = new GeotoolsTransformation(targetCRS, sourceCRS);

					Rectangle2D bbox = controller.getBoundingBox();
					Rect boundingBox = new Rect(bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY());
					double gridSize = this.module.getEventHandler().getData().getCellSize();

					Coord minValues = this.ctInverse.transform(new CoordImpl(boundingBox.minX - gridSize / 2, boundingBox.minY - gridSize / 2));
					Coord maxValues = this.ctInverse.transform(new CoordImpl(boundingBox.maxX + gridSize / 2, boundingBox.maxY + gridSize / 2));

					double westmost = minValues.getX();
					double soutmost = minValues.getY();
					double eastmost = maxValues.getX();
					double northmost = maxValues.getY();

					Envelope2D env = new Envelope2D(DefaultGeographicCRS.WGS84, westmost, soutmost, eastmost - westmost, northmost - soutmost);

					BufferedImage imgEvacuation = this.gridRenderer.getGridAsImage(Mode.EVACUATION, exportSize, exportSize);
					BufferedImage imgClearing = this.gridRenderer.getGridAsImage(Mode.CLEARING, exportSize, exportSize);
					BufferedImage imgUtilization = this.gridRenderer.getGridAsImage(Mode.UTILIZATION, exportSize, exportSize);

					String filePrefix = directory.toString() + "/" + this.module.getCurrentEventFile().getName() + "_2_";

					try {
						TiffExporter.writeGEOTiff(env, filePrefix + Mode.EVACUATION + ".tiff", imgEvacuation);
						TiffExporter.writeGEOTiff(env, filePrefix + Mode.CLEARING + ".tiff", imgClearing);
						TiffExporter.writeGEOTiff(env, filePrefix + Mode.UTILIZATION + ".tiff", imgUtilization);
					} catch (IOException e1) {
						e1.printStackTrace();
					} finally {
						this.gridRenderer.setEnabled(true);
						this.controller.getParentComponent().setCursor(Cursor.getDefaultCursor());
					}
				}

				this.gridRenderer.setEnabled(true);
				this.controller.getParentComponent().setCursor(Cursor.getDefaultCursor());

			}

		}

		/**
		 * Open MATSim config file.
		 * 
		 */
		else if (e.getActionCommand() == "Open") {
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

			int returnVal = fc.showOpenDialog(this.controller.getParentComponent());

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				// open file
				File file = fc.getSelectedFile();
				log.info("Opening: " + file.getAbsolutePath() + ".");
				this.configFile = file.getAbsolutePath();
				file.getParent();
				Config c = ConfigUtils.loadConfig(this.configFile);
				this.sc = ScenarioUtils.loadScenario(c);

				setIterations();

				// read events
				this.module.readEvents();

				// update buttons
				this.openBtn.setEnabled(false);
				this.saveButton.setEnabled(true);
				this.calcButton.setEnabled(true);

				final String sourceCRS = this.controller.getSourceCoordinateSystem();

				this.ctInverse = new GeotoolsTransformation(this.getScenario().getConfig().global().getCoordinateSystem(), sourceCRS);

				this.firstLoad = false;

			} else {
				log.info("Open command cancelled by user.");
			}
		}

		else if (e.getActionCommand() == "calculate") {
			this.module.runCalculation();
		}

		else if ((e.getActionCommand() == "changeIteration") && (!firstLoad)) {
			File newFile = this.module.getEventPathFromName("" + iterationsList.getSelectedItem());
			int index = iterationsList.getSelectedIndex();

			if (newFile != null) {
				this.module.setCurrentEventFile(newFile);
			}

			if (!useCalculateButton)
				this.module.runCalculation();

			iterationsList.setSelectedIndex(index);
		}

		else if (e.getActionCommand() == "changeMode") {
			this.module.setMode((Mode) modeList.getSelectedItem());

		} else if (e.getActionCommand() == "OTFVis") {
			// TODO
		}

	}

	public void setIterations() {
		// get events file, check if there is at least the very first
		// iteration
		this.itersOutputDir = this.controller.getIterationsOutputDirectory();

		// get all available events
		eventFiles = this.module.getAvailableEventFiles(this.itersOutputDir);

		this.module.setCurrentEventFile(eventFiles.get(0));

	}

	public void setEventFileItems(ArrayList<File> items) {
		iterationsList.removeAllItems();
		for (File eventFile : items) {
			String shortenedFileName = eventFile.getName();
			iterationsList.addItem(shortenedFileName);
		}
	}

	public void updateMapViewerSize(int width, int height) {
		if (this.gridRenderer != null)
			this.gridRenderer.setBounds(0, 0, width, height);
	}

	public void setSaveButtonEnabled(boolean enabled) {
		this.saveButton.setEnabled(enabled);
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

	class TypeNumber implements KeyListener {
		@Override
		public void keyTyped(KeyEvent e) {
			if (!Character.toString(e.getKeyChar()).matches("[.0-9]"))
				e.consume();
		}

		@Override
		public void keyReleased(KeyEvent e) {
			JTextField textField = ((JTextField) e.getSource());
			if (!(textField.getText().matches("^[0-9]{0,4}\\.?[0-9]{0,4}$")))
				textField.setText("" + getGridSize());
			else {
				if (textField.getText().length() > 0)
					setGridSize(Double.parseDouble(textField.getText()));
			}

		}

		@Override
		public void keyPressed(KeyEvent e) {
		}
	}

	public Scenario getScenario() {
		return this.sc;
	}

	public Polygon getAreaPolygon() {
		return areaPolygon;
	}

	public void setGridSize(double gridSize) {
		this.cellSize = gridSize;
	}

	public double getGridSize() {
		return this.cellSize;
	}

	public float getCellTransparency() {
		return this.cellTransparency;
	}

	public static String getReadableTime(double value, Unit unit) {
		if (unit.equals(Unit.PEOPLE))
			return " " + (int) value + " people";

		double minutes = 0;
		double hours = 0;
		double seconds = 0;

		if (value < 0d)
			return "";
		else {
			if (value / 60 > 1d) // check if minutes need to be displayed
			{
				if (value / 3600 > 1d) // check if hours need to be displayed
				{
					hours = Math.floor(value / 3600);
					minutes = Math.floor((value - hours * 3600) / 60);
					seconds = Math.floor((value - (hours * 3600) - (minutes * 60)));
					return " > " + (int) hours + "h, " + (int) minutes + "m, " + (int) seconds + "s";
				} else {
					minutes = Math.floor(value / 60);
					seconds = Math.floor((value - (minutes * 60)));
					return " > " + (int) minutes + "m, " + (int) seconds + "s";

				}

			} else {
				return " > " + (int) seconds + "s";
			}
		}
	}

	public KeyPanel getKeyPanel() {
		return keyPanel;
	}

	public AbstractDataPanel getGraphPanel() {
		return graphPanel;
	}

	public void setFirstLoad(boolean b) {
		this.firstLoad = b;

	}

	public JButton getOpenOTFVisBtn() {
		return openOTFVisBtn;
	}

}
