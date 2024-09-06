
/* *********************************************************************** *
 * project: org.matsim.*
 * Gui.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.run.gui;

import com.google.common.base.Preconditions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.RunMatsim;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

/**
 * @author mrieser / Senozon AG
 */
public class Gui extends JFrame {

	private static final long serialVersionUID = 1L;

	private static final JLabel lblFilepaths = new JLabel(
			"Filepaths must either be absolute or relative to the location of the config file.");
	private static final JLabel lblJavaLocation = new JLabel("Java Location:");
	private static final JLabel lblConfigurationFile = new JLabel("Configuration file:");
	private static final JLabel lblOutputDirectory = new JLabel("Output Directory:");
	private static final JLabel lblMemory = new JLabel("Memory:");
	private static final JLabel lblMb = new JLabel("MB");
	private static final JLabel lblYouAreUsingJavaVersion = new JLabel("You are using Java version:");
	private static final JLabel lblYouAreUsingMATSimVersion = new JLabel("You are using MATSim version:");

	private JTextField txtConfigfilename;
	private JTextField txtMatsimversion;
	private JTextField txtRam;
	private JTextField txtJvmversion;
	private JTextField txtJvmlocation;
	private JTextField txtOutput;

	private JButton btnStartMatsim;
	private JProgressBar progressBar;

	private JTextArea textStdOut;
	private JScrollPane scrollPane;
	private JButton btnEdit;

	Map<String, JButton> preprocessButtons = new LinkedHashMap<>();
	Map<String, JButton> postprocessButtons = new LinkedHashMap<>();

	// volatile because set outside EDT (AWT thread), but EDT checks if it is null
	private volatile ExeRunner exeRunner = null;

	private JMenuBar menuBar;
	private JMenu mnTools;
	private JMenuItem mntmCompressFile;
	private JMenuItem mntmUncompressFile;
	private JMenuItem mntmCreateDefaultConfig;
	private JMenuItem mntmCreateSamplePopulation;

	private PopulationSampler popSampler = null;
	private JTextArea textErrOut;
	private final String mainClass;

	private File configFile;
	private File lastUsedDirectory;

	/**
	 * This is the working directory for the simulation. If it is null, the working directory is the directory of the config file.
	 */
	private File workingDirectory = null;
	private ConfigEditor editor = null;
	private ScheduleValidatorWindow transitValidator = null;

	private Gui(final String title, final Class<?> mainClass) {
		setTitle(title);
		this.mainClass = mainClass.getCanonicalName();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	private void createLayout() {
		this.lastUsedDirectory = new File(".");

		txtConfigfilename = new JTextField();
		txtConfigfilename.setText("");
		txtConfigfilename.setColumns(10);

		btnStartMatsim = new JButton("Start MATSim");
		btnStartMatsim.setEnabled(false);

		for (JButton button : preprocessButtons.values()) {
			button.setEnabled(false);
		}
		for (JButton button : postprocessButtons.values()) {
			button.setEnabled(false);
		}

		JButton btnChoose = new JButton("Choose");
		btnChoose.addActionListener(e -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(lastUsedDirectory);
			int result = chooser.showOpenDialog(null);
			if (result == JFileChooser.APPROVE_OPTION) {
				File f = chooser.getSelectedFile();
				lastUsedDirectory = f.getParentFile();
				loadConfigFile(f);
				if (editor != null) {
					editor.closeEditor();
					editor = null;
				}
			}
		});

		this.btnEdit = new JButton("Edit…");
		this.btnEdit.setEnabled(false);
		this.btnEdit.addActionListener(e -> {
			if (editor == null) {
				this.editor = new ConfigEditor(this, configFile, Gui.this::loadConfigFile);
			}
			editor.showEditor();
			editor.toFront();
		});

		txtMatsimversion = new JTextField();
		txtMatsimversion.setEditable(false);
		txtMatsimversion.setText(Gbl.getBuildInfoString());
		txtMatsimversion.setColumns(10);

		txtRam = new JTextField();
		txtRam.setText("1024");
		txtRam.setColumns(10);

		String javaVersion = System.getProperty("java.version")
				+ "; "
				+ System.getProperty("java.vm.vendor")
				+ "; "
				+ System.getProperty("java.vm.info")
				+ "; "
				+ System.getProperty("sun.arch.data.model")
				+ "-bit";

		txtJvmversion = new JTextField();
		txtJvmversion.setEditable(false);
		txtJvmversion.setText(javaVersion);
		txtJvmversion.setColumns(10);

		String jvmLocation;
		if (System.getProperty("os.name").startsWith("Win")) {
			jvmLocation = System.getProperties().getProperty("java.home")
					+ File.separator
					+ "bin"
					+ File.separator
					+ "java.exe";
		} else {
			jvmLocation = System.getProperties().getProperty("java.home")
					+ File.separator
					+ "bin"
					+ File.separator
					+ "java";
		}

		txtJvmlocation = new JTextField();
		txtJvmlocation.setEditable(false);
		txtJvmlocation.setText(jvmLocation);
		txtJvmlocation.setColumns(10);

		txtOutput = new JTextField();
		txtOutput.setEditable(false);
		txtOutput.setText("");
		txtOutput.setColumns(10);

		progressBar = new JProgressBar();
		progressBar.setEnabled(false);
		progressBar.setIndeterminate(true);
		progressBar.setVisible(false);

		btnStartMatsim.addActionListener(e -> {
			if (exeRunner == null) {
				startMATSim();
			} else {
				stopMATSim();
			}
		});

		JButton btnOpen = new JButton("Open");
		btnOpen.addActionListener(e -> {
			if (!txtOutput.getText().isEmpty()) {
				try {
					File f = new File(txtOutput.getText());
					Desktop.getDesktop().open(f);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		});

		JButton btnDelete = new JButton("Delete");
		btnDelete.addActionListener(e -> {
			if (!txtOutput.getText().isEmpty()) {
				int i = JOptionPane.showOptionDialog(Gui.this,
						"Do you really want to delete the output directory? This action cannot be undone.",
						"Delete Output Directory", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
						new String[] { "Cancel", "Delete" }, "Cancel");
				if (i == 1) {
					try {
						IOUtils.deleteDirectoryRecursively(new File(txtOutput.getText()).toPath());
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		});

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		//		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		GroupLayout groupLayout = new GroupLayout(getContentPane());

		final GroupLayout.SequentialGroup prebuttonsSequentialGroup = groupLayout.createSequentialGroup();
		final GroupLayout.ParallelGroup prebuttonsParallelGroup = groupLayout.createParallelGroup();
		for (JButton button : preprocessButtons.values()) {
			prebuttonsSequentialGroup.addComponent(button);
			prebuttonsParallelGroup.addComponent(button);
		}

		final GroupLayout.SequentialGroup postbuttonsSequentialGroup = groupLayout.createSequentialGroup();
		final GroupLayout.ParallelGroup postbuttonsParallelGroup = groupLayout.createParallelGroup();
		for (JButton button : postprocessButtons.values()) {
			postbuttonsSequentialGroup.addComponent(button);
			postbuttonsParallelGroup.addComponent(button);
		}

		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
						.addContainerGap()
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								// a bunch of stuff that can in principle stretch from left to right (although most of it won't):
								.addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 729, Short.MAX_VALUE)
								.addComponent(lblFilepaths) // "Filepaths must either ..."
								.addGroup(prebuttonsSequentialGroup)
								.addGroup(postbuttonsSequentialGroup)
								.addGroup(groupLayout.createSequentialGroup()
										.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
												// some stuff that stretches from left to somewhere1:
												.addComponent(lblYouAreUsingMATSimVersion)
												.addComponent(lblYouAreUsingJavaVersion)
												.addComponent(lblJavaLocation)
												.addComponent(lblConfigurationFile)
												.addComponent(lblOutputDirectory)
												.addComponent(lblMemory)
												.addComponent(btnStartMatsim))
										// a gap from somewhere1 to somewhere2:
										.addPreferredGap(ComponentPlacement.RELATED)
										.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
												// stuff that stretches from somewhere2 to right:
												.addGroup(groupLayout.createSequentialGroup()
														.addComponent(txtRam, GroupLayout.PREFERRED_SIZE, 69,
																GroupLayout.PREFERRED_SIZE)
														.addPreferredGap(ComponentPlacement.RELATED)
														.addComponent(lblMb))
												.addComponent(txtMatsimversion, GroupLayout.DEFAULT_SIZE, 285,
														Short.MAX_VALUE)
												.addComponent(txtJvmversion, GroupLayout.DEFAULT_SIZE, 285,
														Short.MAX_VALUE)
												.addComponent(txtJvmlocation, GroupLayout.DEFAULT_SIZE, 285,
														Short.MAX_VALUE)
												.addGroup(groupLayout.createSequentialGroup()
														.addComponent(txtConfigfilename, GroupLayout.DEFAULT_SIZE, 188,
																Short.MAX_VALUE)
														.addPreferredGap(ComponentPlacement.RELATED)
														.addComponent(btnChoose)
														.addPreferredGap(ComponentPlacement.RELATED)
														.addComponent(btnEdit))
												.addComponent(progressBar, GroupLayout.DEFAULT_SIZE, 285,
														Short.MAX_VALUE)
												.addGroup(groupLayout.createSequentialGroup()
														.addComponent(txtOutput, GroupLayout.DEFAULT_SIZE, 112,
																Short.MAX_VALUE)
														.addPreferredGap(ComponentPlacement.RELATED)
														.addComponent(btnOpen)
														.addPreferredGap(ComponentPlacement.RELATED)
														.addComponent(btnDelete)))))
						.addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
						.addContainerGap()
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblYouAreUsingMATSimVersion)
								.addComponent(txtMatsimversion, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblYouAreUsingJavaVersion)
								.addComponent(txtJvmversion, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblJavaLocation)
								.addComponent(txtJvmlocation, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblConfigurationFile)
								.addComponent(txtConfigfilename, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(btnChoose)
								.addComponent(btnEdit))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(lblFilepaths)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblOutputDirectory)
								.addComponent(txtOutput, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(btnDelete)
								.addComponent(btnOpen))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblMemory)
								.addComponent(txtRam, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(lblMb))
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addGroup(prebuttonsParallelGroup)
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(btnStartMatsim)
								.addComponent(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addGroup(postbuttonsParallelGroup)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE)
						.addContainerGap()));

		textStdOut = new JTextArea();
		textStdOut.setWrapStyleWord(true);
		textStdOut.setTabSize(4);
		textStdOut.setEditable(false);
		scrollPane = new JScrollPane(textStdOut);
		tabbedPane.addTab("Output", null, scrollPane, null);

		JScrollPane scrollPane_1 = new JScrollPane();
		tabbedPane.addTab("Warnings & Errors", null, scrollPane_1, null);

		textErrOut = new JTextArea();
		textErrOut.setWrapStyleWord(true);
		textErrOut.setTabSize(4);
		textErrOut.setEditable(false);
		scrollPane_1.setViewportView(textErrOut);

		getContentPane().setLayout(groupLayout);

		menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		mnTools = new JMenu("Tools");
		menuBar.add(mnTools);

		mntmCompressFile = new JMenuItem("Compress File…");
		mnTools.add(mntmCompressFile);
		mntmCompressFile.addActionListener(e -> GUnZipper.gzipFile());

		mntmUncompressFile = new JMenuItem("Uncompress File…");
		mnTools.add(mntmUncompressFile);
		mntmUncompressFile.addActionListener(e -> GUnZipper.gunzipFile());

		mnTools.addSeparator();

		mntmCreateDefaultConfig = new JMenuItem("Create Default config.xml…");
		mnTools.add(mntmCreateDefaultConfig);
		mntmCreateDefaultConfig.addActionListener(e -> {
			SaveFileSaver chooser = new SaveFileSaver();
			chooser.setSelectedFile(new File("defaultConfig.xml"));
			int saveResult = chooser.showSaveDialog(null);
			if (saveResult == JFileChooser.APPROVE_OPTION) {
				File destFile = chooser.getSelectedFile();
				Config config = ConfigUtils.createConfig();
				new ConfigWriter(config).write(destFile.getAbsolutePath());
			}
		});

		mntmCreateSamplePopulation = new JMenuItem("Create Sample Population…");
		mnTools.add(mntmCreateSamplePopulation);
		mntmCreateSamplePopulation.addActionListener(e -> {
			if (popSampler == null) {
				popSampler = new PopulationSampler(this);
				popSampler.pack();
			}
			popSampler.setVisible(true);
		});

		JMenuItem mntmTransitValidator = new JMenuItem("Validate TransitSchedule…");
		this.mnTools.add(mntmTransitValidator);
		mntmTransitValidator.addActionListener(e -> {
			if (this.transitValidator == null) {
				this.transitValidator = new ScheduleValidatorWindow(this);
			}
			String configFilename = this.txtConfigfilename.getText();
			if (!configFilename.isEmpty()) {
				Config config = ConfigUtils.createConfig();
				try {
					ConfigUtils.loadConfig(config, configFilename);
					this.transitValidator.loadFromConfig(config, new File(configFilename).getParentFile());
				} catch (Exception ignore) {
				}
			}
			this.transitValidator.setVisible(true);
		});
	}

	private void startMATSim() {
		progressBar.setVisible(true);
		progressBar.setEnabled(true);
		this.btnStartMatsim.setEnabled(false);

		textStdOut.setText("");
		textErrOut.setText("");

		String cwd = workingDirectory == null ? new File(txtConfigfilename.getText()).getParent() : workingDirectory.getAbsolutePath();

		new Thread(() -> {
			String classpath = System.getProperty("java.class.path");
			String[] cpParts = classpath.split(File.pathSeparator);
			StringBuilder absoluteClasspath = new StringBuilder();
			for (String cpPart : cpParts) {
				if (absoluteClasspath.length() > 0) {
					absoluteClasspath.append(File.pathSeparatorChar);
				}
				absoluteClasspath.append(new File(cpPart).getAbsolutePath());
			}
			String[] cmdArgs = new String[] { txtJvmlocation.getText(),
					"-cp", absoluteClasspath.toString(),
					"-Xmx" + txtRam.getText() + "m",
					"--add-exports", "java.base/java.lang=ALL-UNNAMED",
					"--add-exports", "java.desktop/sun.awt=ALL-UNNAMED",
					"--add-exports", "java.desktop/sun.java2d=ALL-UNNAMED",
					mainClass, txtConfigfilename.getText() };
			// see https://jogamp.org/bugzilla/show_bug.cgi?id=1317#c21 and/or https://github.com/matsim-org/matsim-libs/pull/2940
			exeRunner = ExeRunner.run(cmdArgs, textStdOut, textErrOut, cwd);
			int exitcode = exeRunner.waitForFinish();
			exeRunner = null;

			SwingUtilities.invokeLater(() -> {
				progressBar.setVisible(false);
				btnStartMatsim.setText("Start MATSim");
				btnStartMatsim.setEnabled(true);
				if (exitcode != 0) {
					textStdOut.append("\n");
					textStdOut.append("The simulation did not run properly. Error/Exit code: " + exitcode);
					textStdOut.setCaretPosition(textStdOut.getDocument().getLength());
					textErrOut.append("\n");
					textErrOut.append("The simulation did not run properly. Error/Exit code: " + exitcode);
					textErrOut.setCaretPosition(textErrOut.getDocument().getLength());
				}
			});

			if (exitcode != 0) {
				throw new RuntimeException("There was a problem running MATSim. exit code: " + exitcode);
			}

		}).start();

		btnStartMatsim.setText("Stop MATSim");
		btnStartMatsim.setEnabled(true);
	}

	public void loadConfigFile(final File configFile) {
		this.configFile = configFile;
		String configFilename = configFile.getAbsolutePath();

		Config config = ConfigUtils.createConfig();
		try {
			ConfigUtils.loadConfig(config, configFilename);
		} catch (Exception e) {
			textStdOut.setText("");
			textStdOut.append("The configuration file could not be loaded. Error message:\n");
			textStdOut.append(e.getMessage());
			textErrOut.setText("");
			textErrOut.append("The configuration file could not be loaded. Error message:\n");
			textErrOut.append(e.getMessage());
			return;
		}
		txtConfigfilename.setText(configFilename);

		File par = configFile.getParentFile();
		File outputDir = new File(par, config.controller().getOutputDirectory());
		try {
			txtOutput.setText(outputDir.getCanonicalPath());
		} catch (IOException e1) {
			txtOutput.setText(outputDir.getAbsolutePath());
		}

		btnStartMatsim.setEnabled(true);
		btnEdit.setEnabled(true);
		for (JButton button : preprocessButtons.values()) {
			button.setEnabled(true);
		}
		for (JButton button : postprocessButtons.values()) {
			button.setEnabled(true);
		}
	}

	private void stopMATSim() {
		ExeRunner runner = this.exeRunner;
		if (runner != null) {
			runner.killProcess();

			//TODO Double check if this works
			exeRunner = null;

			progressBar.setVisible(false);
			btnStartMatsim.setText("Start MATSim");
			btnStartMatsim.setEnabled(true);

			textStdOut.append("\n");
			textStdOut.append("The simulation was stopped forcefully.");
			textStdOut.setCaretPosition(textStdOut.getDocument().getLength());
			textErrOut.append("\n");
			textErrOut.append("The simulation was stopped forcefully.");
			textErrOut.setCaretPosition(textErrOut.getDocument().getLength());
		}
	}

	public static Future<Gui> show(final String title, final Class<?> mainClass) {
		return show(title, mainClass, null);
	}

	/**
	 * Create the GUI and return it as future when created.
	 */
	public static Future<Gui> show(final String title, final Class<?> mainClass, File configFile) {
		System.setProperty("apple.laf.useScreenMenuBar", "true");

		RunnableFuture<Gui> rf = new FutureTask<>(() -> {
			Gui gui = new Gui(title, mainClass);
			gui.createLayout();
			gui.pack();
			gui.setLocationByPlatform(true);
			gui.setVisible(true);
			if (configFile != null && configFile.exists()) {
				gui.loadConfigFile(configFile);
			}

			return gui;
		});

		SwingUtilities.invokeLater(rf);
		return rf;
	}

	public static void main(String[] args) {
		Preconditions.checkArgument(args.length < 2);
		Gui.show("MATSim", RunMatsim.class, args.length == 1 ? new File(args[0]) : null);
	}

	public void setWorkingDirectory(File cwd) {
		this.workingDirectory = cwd;
	}

	// Is it a problem to make the following available to the outside?  If so, why?  Would it
	// be better to rather copy/paste the above code and start from there?  kai, jun/aug'18

	//	final JMenu getToolsMenu() {
	//		// Is it a problem to make this available?  If so, why?  kai, jun'18
	//		return this.mnTools ;
	//	}
	//	final void addToMenuBar(JMenu menuItem) {
	//		this.menuBar.add(menuItem) ;
	//	}
	//	final void addPreprocessButton( String str, JButton button ) {
	//		this.preprocessButtons.put( str, button );
	//	}
	//
	//	/**
	//	 * @param str -- name.  These are named so that they can be replaced, and in theory removed.  Maybe not necessary. kai, sep'18
	//	 * @param button
	//	 */
	//	final void addPostprocessButton( String str, JButton button ) {
	//		this.postprocessButtons.put( str, button );
	//	}
	//	JTextArea getTextStdOut() {
	//		return textStdOut;
	//	}
	//	JTextArea getTextErrOut() {
	//		return textErrOut;
	//	}
	//	JTextField getTxtJvmlocation() {
	//		return txtJvmlocation ;
	//	}
	//	JTextField getTxtRam() {
	//		return this.txtRam ;
	//	}
	//	@Deprecated // this should not be necessary. kai, aug'18
	//	String getMainClass() {
	//		return this.mainClass ;
	//	}
	//	JTextField getTxtConfigfilename() {
	//		return this.txtConfigfilename ;
	//	}

}
