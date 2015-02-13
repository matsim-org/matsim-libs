/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * OTFVisGUI.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.contrib.otfvis;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("serial")
public class OTFVisGUI extends JDialog implements ActionListener {
	JTabbedPane tabPane = new JTabbedPane();

	public static void main(String[] args) {
		runDialog();
	}

	public static void runDialog() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				OTFVisGUI meinJDialog = new OTFVisGUI();
				meinJDialog.setTitle("OTFVis Open Dialog...");
				JTabbedPane tabPane = meinJDialog.tabPane;

				// Create and initialize the buttons.
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(meinJDialog);

				final JButton setButton = new JButton("Load...");
				setButton.setActionCommand("Load");
				setButton.addActionListener(meinJDialog);
				meinJDialog.getRootPane().setDefaultButton(setButton);
				// Lay out the buttons from left to right.
				JPanel buttonPane = new JPanel();
				buttonPane.setLayout(new BoxLayout(buttonPane,
						BoxLayout.LINE_AXIS));
				buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10,
						10));
				buttonPane.add(Box.createHorizontalGlue());
				buttonPane.add(cancelButton);
				buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
				buttonPane.add(setButton);

				// First Tab MVI playback
				ActionPanel panel = meinJDialog.new ActionPanel(true) {
					public void execute() {
						String file = ((JTextField) getComponent(1)).getText();
						OTFVis.playMVI(file);
					}
				};
				panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
				panel.setAlignmentY(TOP_ALIGNMENT);
				panel.add(new JLabel("MVI File:  "));
				meinJDialog.addTextFieldButton(panel, "MVI");
				panel.add(Box.createVerticalGlue());
				tabPane.addTab("Load & Display MVI File", panel);

				// Second Tab show network
				panel = meinJDialog.new ActionPanel(true) {
					public void execute() {
						String file = ((JTextField) getComponent(1)).getText();
						OTFVis.playNetwork(file);
					}
				};
				panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
				panel.add(new JLabel("Network File: "));
				meinJDialog.addTextFieldButton(panel, "Net");
				tabPane.addTab("Load & Display Network File", panel);

				// Third Tab run mobsim config file
				panel = meinJDialog.new ActionPanel(true) {
					public void execute() {
						final String file = ((JTextField) getComponent(1))
								.getText();
						// Mobsim needs to run in own thread!
						Thread queryThread = new Thread() {
							public void run() {
								OTFVis.playConfig(file);
							}
						};
						queryThread.start();
					}
				};

				panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
				panel.add(new JLabel("Config File: "));
				meinJDialog.addTextFieldButton(panel, "Config");
				tabPane.addTab("Load & Run MATSIM Configuration", panel);

				// Fourth Tab convert an event file with optional playback
				panel = meinJDialog.new ActionPanel(true) {
					public boolean isFileValid() {
						boolean valid = false;
						// Get Time Resolution for format testing
						JTextField rateT = (JTextField) getComponent(getComponentCount() - 2);
						try {
							Integer.parseInt(rateT.getText());
							valid = true;
						} catch (NumberFormatException e) {

						}
						valid &= isFieldValid((JTextField) getComponent(1),
								true);
						valid &= isFieldValid((JTextField) getComponent(4),
								true);
						// MVI file may not exist yet, should be asked for if
						// overwrite
						valid &= isFieldValid((JTextField) getComponent(10),
								false);
						return valid;
					}

					public void execute() {
						String[] strings = new String[5];
						strings[0] = "";
						strings[1] = ((JTextField) getComponent(1)).getText();
						strings[2] = ((JTextField) getComponent(4)).getText();
						strings[3] = ((JTextField) getComponent(10)).getText();
						strings[4] = ((JTextField) getComponent(getComponentCount() - 2))
								.getText();
						OTFVis.convert(strings);
						boolean openMVI = ((JCheckBox) getComponent(getComponentCount() - 1))
								.isSelected();
						if (openMVI) {
							OTFVis.playMVI(strings[3]);
						}
					}
				};

				panel.setLayout(new GridLayout(5, 4));
				panel.add(new JLabel("Event File: "));
				meinJDialog.addTextFieldButton(panel, "Event");
				panel.add(new JLabel("Network File: "));
				meinJDialog.addTextFieldButton(panel, "Net");
				panel.add(new JLabel(" "));
				panel.add(new JLabel(" "));
				panel.add(new JLabel(" "));
				panel.add(new JLabel("MVI Result File: "));
				meinJDialog.addTextFieldButton(panel, "MVI");
				panel.add(new JLabel("Time Resolution (sec): "));
				addTextField(panel, "600");
				panel.add(new JCheckBox("Open MVI afterwards"));
				tabPane.addTab("Convert Event File To MVI File", panel);

				Container contentPane = meinJDialog.getContentPane();
				JPanel panelM = new JPanel();
				panelM.setLayout(new BoxLayout(panelM, BoxLayout.LINE_AXIS));
				JLabel label = new JLabel(
						"Choose One of the Following Tabs, Select the Files and Press \"Load\" to Procede.");

				panelM.add(label);
				panelM.add(Box.createHorizontalGlue());
				panelM.add(Box.createRigidArea(new Dimension(20, 20)));
				contentPane.add(panelM, BorderLayout.PAGE_START);
				contentPane.add(tabPane, BorderLayout.CENTER);
				contentPane.add(buttonPane, BorderLayout.PAGE_END);

				meinJDialog.setMinimumSize(new Dimension(800, 300));
				meinJDialog.pack();
				meinJDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				meinJDialog.setVisible(true);
			}
		});
	}

	private void addTextFieldButton(ActionPanel panel, String type) {
		JTextField text = new JTextField("");
		text.setMaximumSize(new Dimension(600, 27));
		text.setMinimumSize(new Dimension(300, 27));
		panel.add(text);
		panel.addTextField(text); // This must be valid
		JButton button = new JButton("Browse... ");
		BrowseAction actio = new BrowseAction(text, type);
		button.setAction(actio);
		panel.add(button);

	}

	private static void addTextField(JPanel panel, String value) {
		JTextField text = new JTextField(value);
		text.setMaximumSize(new Dimension(300, 27));
		panel.add(text);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("Cancel")) {
			System.exit(0);
		} else if (command.equals("Load")) {
			// Check configuration
			ActionPanel panel = (ActionPanel) tabPane.getSelectedComponent();
			if (panel.isFileValid()) {
				this.setVisible(false); // We are done here
				panel.execute();
			} else {
				JOptionPane.showMessageDialog(this,
						"Not all Filenames given/valid!");
			}
		}
	}

	class ActionPanel extends JPanel {
		private boolean mustExist;
		private List<JTextField> fields = new ArrayList<JTextField>();

		public ActionPanel(boolean mustExist) {
			this.mustExist = mustExist;
		}

		public void addTextField(JTextField text) {
			fields.add(text);
		}

		//
		protected boolean isFieldValid(JTextField field, boolean mustExist) {
			boolean valid = true;
			String name = field.getText();
			// Check if all textfields have been set
			valid &= name.length() > 0;
			// If they are valid filenames
			if (valid) {
				File file = new File(name);
				boolean exists = file.exists();
				if (mustExist) {
					valid &= exists;
				} else {
					int answer = JOptionPane.showConfirmDialog(this, name
							+ " exists!\nDo you want to overwrite?");
					valid = (answer == 0);
				}
			}
			return valid;
		}

		protected boolean isFieldValid2(JTextField field, boolean mustExist) {
			boolean valid = true;
			// Check if all textfields have been set
			valid &= field.getText().length() > 0;
			// If they are valid filenames
			if (valid && mustExist) {
				File file = new File(field.getText());
				valid &= file.exists();
			}
			return valid;
		}

		public boolean isFileValid() {
			boolean valid = true;
			for (JTextField field : fields) {
				valid &= isFieldValid(field, mustExist);
			}
			return valid;
		}

		public void execute() {
		};
	}

	class BrowseAction extends AbstractAction {
		String type;
		JTextField text;

		private void setFilter(JFileChooser fc, final String suffix,
				final String description) {
			fc.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					return f.isDirectory()
							|| f.getName().toLowerCase(Locale.ROOT)
									.endsWith(suffix);
				}

				@Override
				public String getDescription() {
					return description;
				}
			});
		}

		public BrowseAction(JTextField text, String type) {
			super("Browse...", null);
			this.text = text;
			this.type = type;
			putValue(SHORT_DESCRIPTION, "Browse...");
		}

		public void actionPerformed(ActionEvent e) {
			// Which file to browse?
			JFileChooser fc = new JFileChooser();
			if (type.equals("Net")) {
				setFilter(fc, ".xml", "MATSim net file (*.xml)");
			} else if (type.equals("Config")) {
				setFilter(fc, ".xml", "MATSim config file (*.xml)");
			} else if (type.equals("Event")) {
				setFilter(fc, ".events.xml.gz",
						"MATSim Event file (*.events.xml.gz)");
			} else if (type.equals("MVI")) {
				setFilter(fc, ".mvi", "OTFVis movie file (*.mvi)");
			}

			JButton button = (JButton) e.getSource();
			Component parent = button.getParent();
			int state = fc.showOpenDialog(parent);
			if (state == JFileChooser.APPROVE_OPTION) {
				String filename = fc.getSelectedFile().getAbsolutePath();
				// Set the appropriate text field... we know its a browse button
				// parent.get
				// filename.;
				this.text.setText(filename);
			}
			System.out.println("No file selected.");

		}
	}
}
