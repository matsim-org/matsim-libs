/* *********************************************************************** *
 * project: org.matsim.*
 * OTFClient
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
package org.matsim.vis.otfvis.gui;

import org.apache.log4j.Logger;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.data.fileio.SettingsSaver;
import org.matsim.vis.otfvis.interfaces.OTFServer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.utils.WGS84ToMercator;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;


/**
 * @author dgrether
 *
 */
public final class OTFVisFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(OTFVisFrame.class);
	private final Component canvas;

	private final OTFOGLDrawer mainDrawer;

	private final OTFServer server;

	private JPanel compositePanel;

	/**
	 * This method statically installs a custom Swing RepaintManager which ties the map component to the JPanel in which it is 
	 * layered under the agent drawer. Otherwise the map would repaint itself when it has finished loading a tile, and the agent drawer
	 * would not notice and would be painted over.
	 * 
	 * This looks dirty and probably does not scale to the case where many components would do this, but it is the only way
	 * I have found, short of patching the JXMapViewer.
	 * 
	 */
	private static void installCustomRepaintManager(final JPanel compositePanel, final JXMapViewer jMapViewer) {
		RepaintManager myManager = new RepaintManager() {
			public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
				// I had the feeling I should call the *previous* RepaintManager here instead of the supertype, but that does not work.
				// So I call the supertype.
				super.addDirtyRegion(c, x, y, w, h); 
				if (c == jMapViewer) {
					addDirtyRegion(compositePanel, x, y, w, h);
				}
			}
		};
		RepaintManager.setCurrentManager(myManager);
	}
	
	private static double log2 (double scale) {
		return Math.log(scale) / Math.log(2);
	}
	
	public OTFVisFrame(Component canvas, OTFServer server, OTFControlBar controlBar, OTFOGLDrawer mainDrawer, SettingsSaver saver) {
		super("MATSim OTFVis");

		// Not considered very nice -- The QSim runs on the main thread, and we effectively kill it
		// when we close the window.
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		JFrame.setDefaultLookAndFeelDecorated(true);
		boolean isMac = System.getProperty("os.name").equals("Mac OS X");
		if (isMac){
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
		}
		//Make sure menus appear above JOGL Layer
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		compositePanel = new JPanel();
		compositePanel.setBackground(Color.white);
		compositePanel.setOpaque(true);
		compositePanel.setLayout(new OverlayLayout(compositePanel));
		this.canvas = canvas;
		compositePanel.add(this.canvas);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		compositePanel.setPreferredSize(new Dimension(screenSize.width/2,screenSize.height/2));
		log.info("created MainFrame");
		this.server = server;
		this.mainDrawer = mainDrawer;
		log.info("got OTFVis config");
		getContentPane().add(controlBar, BorderLayout.NORTH);
		buildMenu(saver);
		log.info("created HostControlBar");
		log.info("created drawer");
		getContentPane().add(compositePanel, BorderLayout.CENTER);
	}

	@SuppressWarnings("serial")
	private void buildMenu(final SettingsSaver save) {
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		Action prefAction = new AbstractAction() {
			{
				putValue(Action.NAME, "Preferences...");
				putValue(Action.MNEMONIC_KEY, 0);
			}

			@Override
			public void actionPerformed(final ActionEvent e) {
				PreferencesDialog preferencesDialog = new PreferencesDialog(OTFClientControl.getInstance().getOTFVisConfig());
				preferencesDialog.setVisible(true);
			}
		};
		fileMenu.add(prefAction);
		Action saveAsAction = new AbstractAction() {
			{
				putValue(Action.NAME, "Save Settings as...");
				putValue(Action.MNEMONIC_KEY, 1);
			}

			@Override
			public void actionPerformed(final ActionEvent e) {
				save.saveSettingsAs(OTFClientControl.getInstance().getOTFVisConfig());
			}
		};
		fileMenu.add(saveAsAction);

		Action openAction = new AbstractAction() {
			{
				putValue(Action.NAME, "Open Settings...");
				putValue(Action.MNEMONIC_KEY, 1);
			}

			@Override
			public void actionPerformed(final ActionEvent e) {
				OTFVisConfigGroup visConfig = save.chooseAndReadSettingsFile();
				OTFClientControl.getInstance().setOTFVisConfig(visConfig);
			}
		};
		fileMenu.add(openAction);

		Action exitAction = new AbstractAction("Quit") {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispatchEvent(new WindowEvent(OTFVisFrame.this, WindowEvent.WINDOW_CLOSING));			}
		};
		fileMenu.add(exitAction);
		setJMenuBar(menuBar);
		SwingUtilities.updateComponentTreeUI(this);
	}

	public void addMapViewer(TileFactory tf) {
		final JXMapViewer jMapViewer = new JXMapViewer();
		jMapViewer.setTileFactory(tf);
		compositePanel.add(jMapViewer);
		installCustomRepaintManager(compositePanel, jMapViewer);
		OTFVisConfigGroup otfVisConfig = OTFClientControl.getInstance().getOTFVisConfig();
		final CoordinateTransformation coordinateTransformation = new WGS84ToMercator.Deproject(otfVisConfig.getMaximumZoom());
		mainDrawer.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				double x = mainDrawer.getViewBoundsAsQuadTreeRect().centerX + mainDrawer.getQuad().offsetEast;
				double y = mainDrawer.getViewBoundsAsQuadTreeRect().centerY + mainDrawer.getQuad().offsetNorth;
				Coord center = coordinateTransformation.transform(new Coord(x, y));
				double scale = mainDrawer.getScale()*2;
				int zoom = (int) log2(scale);
				jMapViewer.setCenterPosition(new GeoPosition(center.getY(), center.getX()));
				jMapViewer.setZoom(zoom);
				compositePanel.repaint();
			}

		});
	}

	private class PreferencesDialog extends JDialog {

		private final OTFVisConfigGroup visConfig;

		public PreferencesDialog(OTFVisConfigGroup config) {
			super(OTFVisFrame.this);
			this.visConfig = config;
			getContentPane().setLayout(null);
			this.setResizable(false);
			setSize(480, 400);

			// Mouse Buttons
			{
				JPanel panel = new JPanel(null);
				getContentPane().add(panel);
				panel.setBorder(BorderFactory.createTitledBorder("Mouse Buttons"));
				panel.setBounds(10, 10, 220, 120);
				{
					JLabel label = new JLabel("Left:", JLabel.RIGHT);
					panel.add(label);
					label.setBounds(10, 20, 55, 27);
				}
				{
					JLabel label = new JLabel("Middle:", JLabel.RIGHT);
					panel.add(label);
					label.setBounds(10, 50, 55, 27);
				}
				{
					JLabel label = new JLabel("Right:", JLabel.RIGHT);
					panel.add(label);
					label.setBounds(10, 80, 55, 27);
				}
				{
					ComboBoxModel<String> leftMFuncModel = new DefaultComboBoxModel<>(new String[] { "Zoom", "Pan", "Select" });
					leftMFuncModel.setSelectedItem(this.visConfig.getLeftMouseFunc());
					final JComboBox leftMFunc = new JComboBox();
					panel.add(leftMFunc);
					leftMFunc.setModel(leftMFuncModel);
					leftMFunc.setBounds(70, 20, 120, 27);
					leftMFunc.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							visConfig.setLeftMouseFunc((String) leftMFunc.getSelectedItem());
						}
					});
				}
				{
					ComboBoxModel<String> jComboBox1Model = new DefaultComboBoxModel<>(new String[]{"Zoom", "Pan", "Select"});
					jComboBox1Model.setSelectedItem(this.visConfig.getMiddleMouseFunc());
					final JComboBox middleMFunc = new JComboBox();
					panel.add(middleMFunc);
					middleMFunc.setModel(jComboBox1Model);
					middleMFunc.setBounds(70, 50, 120, 27);
					middleMFunc.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							visConfig.setMiddleMouseFunc((String) middleMFunc.getSelectedItem());
						}
					});
				}
				{
					ComboBoxModel<String> jComboBox2Model = new DefaultComboBoxModel<>(new String[] { "Menu", "Zoom", "Pan", "Select" });
					jComboBox2Model.setSelectedItem(this.visConfig.getRightMouseFunc());
					final JComboBox rightMFunc = new JComboBox();
					panel.add(rightMFunc);
					rightMFunc.setModel(jComboBox2Model);
					rightMFunc.setBounds(70, 80, 120, 27);
					rightMFunc.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							visConfig.setRightMouseFunc((String) rightMFunc.getSelectedItem());
						}
					});
				}
			}
			{
				JPanel panel = new JPanel(null);
				getContentPane().add(panel);
				panel.setBorder(BorderFactory.createTitledBorder("Switches"));
				panel.setBounds(250, 130, 220, 200);

				JCheckBox synchBox;
				if(server.isLive()) {
					synchBox = new JCheckBox("show non-moving items");
					synchBox.setSelected(visConfig.isDrawNonMovingItems());
					synchBox.addItemListener(new ItemListener() {
						@Override
						public void itemStateChanged(ItemEvent e) {
							visConfig.setDrawNonMovingItems(!visConfig.isDrawNonMovingItems());
							server.setShowNonMovingItems(visConfig.isDrawNonMovingItems());
							canvas.repaint();
						}
					});
					synchBox.setBounds(10, 20, 200, 31);
					synchBox.setVisible(true);
					panel.add(synchBox);
				}
				synchBox = new JCheckBox("show link Ids");
				synchBox.setSelected(visConfig.isDrawingLinkIds());
				synchBox.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						visConfig.setDrawLinkIds(!visConfig.isDrawingLinkIds());
					}
				});
				synchBox.setBounds(10, 40, 200, 31);
				synchBox.setVisible(true);
				panel.add(synchBox);

				synchBox = new JCheckBox("show overlays");
				synchBox.setSelected(visConfig.drawOverlays());
				synchBox.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						visConfig.setDrawOverlays(!visConfig.drawOverlays());
					}
				});
				synchBox.setBounds(10, 60, 200, 31);
				synchBox.setVisible(true);
				panel.add(synchBox);

				synchBox = new JCheckBox("show time GL");
				synchBox.setSelected(visConfig.drawTime());
				synchBox.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						visConfig.setDrawTime(!visConfig.drawTime());
					}
				});
				synchBox.setBounds(10, 80, 200, 31);
				panel.add(synchBox);

				synchBox = new JCheckBox("save jpg frames");
				synchBox.setSelected(visConfig.getRenderImages());
				synchBox.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						visConfig.setRenderImages(!visConfig.getRenderImages());
					}
				});
				synchBox.setBounds(10, 100, 200, 31);
				panel.add(synchBox);

				synchBox = new JCheckBox("show scale bar");
				synchBox.setSelected(visConfig.drawScaleBar());
				synchBox.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						visConfig.setDrawScaleBar(!visConfig.drawScaleBar());
					}
				});
				synchBox.setBounds(10, 140, 200, 31);
				synchBox.setVisible(true);
				panel.add(synchBox);
				if (server.isLive()) {
					synchBox = new JCheckBox("show transit facilities");
					synchBox.setSelected(visConfig.isDrawTransitFacilities());
					synchBox.addItemListener(new ItemListener() {
						@Override
						public void itemStateChanged(ItemEvent e) {
							visConfig.setDrawTransitFacilities(!visConfig.isDrawTransitFacilities());
						}
					});
					synchBox.setBounds(10, 160, 200, 31);
					synchBox.setVisible(true);
					panel.add(synchBox);
				}
			}


			// Colors
			{
				JPanel panel = new JPanel(null);
				getContentPane().add(panel);
				panel.setBorder(BorderFactory.createTitledBorder("Colors"));
				panel.setBounds(250, 10, 220, 120);

				{
					JButton jButton = new JButton("Set Background...");
					panel.add(jButton);
					jButton.setBounds(10, 20, 200, 31);
					jButton.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							JPanel frame = new JPanel();
							Color c = JColorChooser.showDialog(frame, "Choose the background color", visConfig.getBackgroundColor());
							if (c != null) {
								visConfig.setBackgroundColor(c);
								canvas.repaint();
							}
						}
					});
				}

				{
					JButton jButton = new JButton("Set Network...");
					panel.add(jButton);
					jButton.setBounds(10, 50, 200, 31);
					jButton.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							JPanel frame = new JPanel();
							Color c = JColorChooser.showDialog(frame, "Choose the network color", visConfig.getNetworkColor());
							if (c != null) {
								visConfig.setNetworkColor(c);
								canvas.repaint();
							}
						}
					});
				}

			}

			// Agent size
			{
				JLabel label = new JLabel();
				getContentPane().add(label);
				label.setText("AgentSize:");
				label.setBounds(10, 145, 80, 31);
				final JSpinner agentSizeSpinner = new JSpinner();
				SpinnerNumberModel model = new SpinnerNumberModel((int) visConfig.getAgentSize(), 0.1, Double.MAX_VALUE, 10);
				agentSizeSpinner.setModel(model);
				agentSizeSpinner.setBounds(90, 145, 153, 31);
				agentSizeSpinner.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent e) {
						visConfig.setAgentSize(((Double)(agentSizeSpinner.getValue())).floatValue());
						canvas.repaint();
					}
				});
				getContentPane().add(label);
				getContentPane().add(agentSizeSpinner);
			}

			//Link Width
			{
				JLabel label = new JLabel();
				getContentPane().add(label);
				label.setText("LinkWidth:");
				label.setBounds(10, 195, 80, 31);
				final JSpinner linkWidthSpinner = new JSpinner();
				SpinnerNumberModel model2 = new SpinnerNumberModel((int) visConfig.getLinkWidth(), 0.1, Double.MAX_VALUE, 10);
				linkWidthSpinner.setModel(model2);
				linkWidthSpinner.setBounds(90, 195, 153, 31);
				linkWidthSpinner.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent e) {
						visConfig.setLinkWidth(((Double)(linkWidthSpinner.getValue())).floatValue());
						canvas.repaint();
					}
				});
				getContentPane().add(label);
				getContentPane().add(linkWidthSpinner);
			}

			//Delay ms
			{
				JLabel label = new JLabel();
				getContentPane().add(label);
				label.setText("AnimSpeed:");
				label.setBounds(10, 245, 110, 31);
				final JSpinner delaySpinner = new JSpinner();
				SpinnerNumberModel model2 = new SpinnerNumberModel(visConfig.getDelay_ms(), 0, Double.MAX_VALUE, 10);
				delaySpinner.setModel(model2);
				delaySpinner.setBounds(90, 245, 153, 31);
				delaySpinner.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent e) {
						visConfig.setDelay_ms(((Double)(delaySpinner.getValue())).intValue());
						canvas.repaint();
					}
				});
				getContentPane().add(label);
				getContentPane().add(delaySpinner);
			}

		}

	}
}
