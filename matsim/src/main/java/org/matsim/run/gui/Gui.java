package org.matsim.run.gui;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author mrieser / Senozon AG
 */
public class Gui extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private JTextField txtConfigfilename;
	private JTextField txtMatsimversion;
	private JTextField txtRam;
	private JTextField txtJvmversion;
	private JTextField txtJvmlocation;
	private JTextField txtOutput;
	private JLabel lblWorkDirectory;
	
	private JButton btnStartMatsim;
	private JProgressBar progressBar;
	private JTextArea textArea;
	private JScrollPane scrollPane;
	
	private ExeRunner exeRunner = null;
	private JMenuBar menuBar;
	private JMenu mnTools;
	private JMenuItem mntmCompressFile;
	private JMenuItem mntmUncompressFile;
	private JMenuItem mntmCreateSamplePopulation;

	private PopulationSampler popSampler = null;
	
	public Gui() {
		setTitle("MATSim");
		
		JLabel lblConfigurationFile = new JLabel("Configuration file:");
		
		txtConfigfilename = new JTextField();
		txtConfigfilename.setText("");
		txtConfigfilename.setColumns(10);
		
		btnStartMatsim = new JButton("Start MATSim");
		btnStartMatsim.setEnabled(false);
		
		this.lblWorkDirectory = new JLabel("Filepaths must either be absolute or relative to the location of the config file.");
		
		JButton btnChoose = new JButton("Choose");
		btnChoose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				int result = chooser.showOpenDialog(null);
				if (result == JFileChooser.APPROVE_OPTION) {
					File f = chooser.getSelectedFile();
					String filename = f.getAbsolutePath();
					txtConfigfilename.setText(filename);
					
					Config config = ConfigUtils.createConfig();
					ConfigUtils.loadConfig(config, filename);
					
					File par = f.getParentFile();
					File outputDir = new File(par, config.controler().getOutputDirectory());
					try {
						txtOutput.setText(outputDir.getCanonicalPath());
					} catch (IOException e1) {
						txtOutput.setText(outputDir.getAbsolutePath());
					}
					
					btnStartMatsim.setEnabled(true);
				}
			}
		});
		
		JLabel lblYouAreRunning = new JLabel("You are using MATSim version:");
		
		txtMatsimversion = new JTextField();
		txtMatsimversion.setEditable(false);
		txtMatsimversion.setText(Gbl.getBuildInfoString());
		txtMatsimversion.setColumns(10);
		
		JLabel lblOutputDirectory = new JLabel("Output Directory:");
		
		JLabel lblMemory = new JLabel("Memory:");
		
		txtRam = new JTextField();
		txtRam.setText("1024");
		txtRam.setColumns(10);
		
		JLabel lblMb = new JLabel("MB");
		
		JLabel lblYouAreUsing = new JLabel("You are using Java version:");
		
		String javaVersion = System.getProperty("java.version") + "; "
				+ System.getProperty("java.vm.vendor") + "; "
				+ System.getProperty("java.vm.info") + "; "
				+ System.getProperty("sun.arch.data.model") + "-bit";
		
		txtJvmversion = new JTextField();
		txtJvmversion.setEditable(false);
		txtJvmversion.setText(javaVersion);
		txtJvmversion.setColumns(10);
		
		JLabel lblJavaLocation = new JLabel("Java Location:");
		
		String jvmLocation;
		if (System.getProperty("os.name").startsWith("Win")) {
			jvmLocation = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe";
		} else {
			jvmLocation = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java";
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
		
		btnStartMatsim.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (exeRunner == null) {
					startMATSim();
				} else {
					stopMATSim();
				}
			}
		});
		
		JButton btnOpen = new JButton("Open");
		btnOpen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!Gui.this.txtOutput.getText().isEmpty()) {
					try {
						File f = new File(Gui.this.txtOutput.getText());
						Desktop.getDesktop().open(f);
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
		});
		
		JButton btnDelete = new JButton("Delete");
		btnDelete.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!Gui.this.txtOutput.getText().isEmpty()) {
					int i = JOptionPane.showOptionDialog(Gui.this, "Do you really want to delete the output directory? This action cannot be undone.", "Delete Output Directory", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[] {"Cancel", "Delete"}, "Cancel");
					if (i == 1) {
						try {
							IOUtils.deleteDirectory(new File(Gui.this.txtOutput.getText()));
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			}
		});
		
		textArea = new JTextArea();
		textArea.setWrapStyleWord(true);
		textArea.setTabSize(4);
		textArea.setEditable(false);
		scrollPane = new JScrollPane(textArea);
//		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
						.addComponent(scrollPane, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 697, Short.MAX_VALUE)
						.addComponent(lblWorkDirectory, Alignment.LEADING)
						.addComponent(lblJavaLocation, Alignment.LEADING)
						.addComponent(lblConfigurationFile, Alignment.LEADING)
						.addComponent(lblOutputDirectory, Alignment.LEADING)
						.addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(lblYouAreRunning)
								.addComponent(lblYouAreUsing)
								.addComponent(lblMemory)
								.addComponent(btnStartMatsim))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addGroup(groupLayout.createSequentialGroup()
									.addComponent(txtRam, GroupLayout.PREFERRED_SIZE, 69, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(lblMb))
								.addComponent(txtMatsimversion, GroupLayout.DEFAULT_SIZE, 496, Short.MAX_VALUE)
								.addComponent(txtJvmversion, GroupLayout.DEFAULT_SIZE, 496, Short.MAX_VALUE)
								.addComponent(txtJvmlocation, GroupLayout.DEFAULT_SIZE, 496, Short.MAX_VALUE)
								.addGroup(groupLayout.createSequentialGroup()
									.addComponent(txtConfigfilename, GroupLayout.DEFAULT_SIZE, 399, Short.MAX_VALUE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(btnChoose))
								.addComponent(progressBar, GroupLayout.DEFAULT_SIZE, 496, Short.MAX_VALUE)
								.addGroup(groupLayout.createSequentialGroup()
									.addComponent(txtOutput, GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(btnOpen)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(btnDelete)))))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblYouAreRunning)
						.addComponent(txtMatsimversion, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblYouAreUsing)
						.addComponent(txtJvmversion, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblJavaLocation)
						.addComponent(txtJvmlocation, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblConfigurationFile)
						.addComponent(txtConfigfilename, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnChoose))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(lblWorkDirectory)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblOutputDirectory)
						.addComponent(txtOutput, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnDelete)
						.addComponent(btnOpen))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblMemory)
						.addComponent(txtRam, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblMb))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(btnStartMatsim)
						.addComponent(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE)
					.addContainerGap())
		);

		getContentPane().setLayout(groupLayout);
		
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		mnTools = new JMenu("Tools");
		menuBar.add(mnTools);
		
		mntmCompressFile = new JMenuItem("Compress File…");
		mnTools.add(mntmCompressFile);
		mntmCompressFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GUnZipper.gzipFile();
			}
		});
		
		mntmUncompressFile = new JMenuItem("Uncompress File…");
		mnTools.add(mntmUncompressFile);
		mntmUncompressFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GUnZipper.gunzipFile();
			}
		});
		
		mnTools.addSeparator();
		
		mntmCreateSamplePopulation = new JMenuItem("Create Sample Population…");
		mnTools.add(mntmCreateSamplePopulation);
		mntmCreateSamplePopulation.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (popSampler == null) {
					popSampler = new PopulationSampler();
					popSampler.pack();
				}
				popSampler.setVisible(true);
			}
		});
	}
	
	public void startMATSim() {
		progressBar.setVisible(true);
		progressBar.setEnabled(true);
		this.btnStartMatsim.setEnabled(false);

		new Thread(new Runnable() {
			@Override
			public void run() {
				String classpath = System.getProperty("java.class.path");
				String[] cpParts = classpath.split(File.pathSeparator);
				StringBuilder absoluteClasspath = new StringBuilder();
				for (String cpPart : cpParts) {
					if (absoluteClasspath.length() > 0) {
						absoluteClasspath.append(File.pathSeparatorChar);
					}
					absoluteClasspath.append(new File(cpPart).getAbsolutePath());
				}
				String[] cmdArgs = new String[] {
						txtJvmlocation.getText(),
						"-cp",
						absoluteClasspath.toString(),
						"-Xmx" + txtRam.getText() + "m",
						"org.matsim.run.Controler",
						txtConfigfilename.getText()
				};
				Gui.this.textArea.setText("");
				Gui.this.exeRunner = ExeRunner.run(cmdArgs, Gui.this.textArea, new File(txtConfigfilename.getText()).getParent());
				Gui.this.btnStartMatsim.setText("Stop MATSim");
				Gui.this.btnStartMatsim.setEnabled(true);
				int exitcode = exeRunner.waitForFinish();
				Gui.this.exeRunner = null;

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						progressBar.setVisible(false);
						btnStartMatsim.setText("Start MATSim");
						btnStartMatsim.setEnabled(true);
					}
				});

				if (exitcode != 0) {
					Gui.this.textArea.append("\n");
					Gui.this.textArea.append("The simulation did not run properly. Error/Exit code: " + exitcode);
					Gui.this.textArea.setCaretPosition(Gui.this.textArea.getDocument().getLength());
					throw new RuntimeException("There was a problem running MATSim. exit code: " + exitcode);
				}
			}
		}).start();
		
	}
	
	public void stopMATSim() {
		ExeRunner runner = this.exeRunner;
		if (runner != null) {
			runner.killProcess();
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					progressBar.setVisible(false);
					btnStartMatsim.setText("Start MATSim");
					btnStartMatsim.setEnabled(true);
					
					Gui.this.textArea.append("\n");
					Gui.this.textArea.append("The simulation was stopped forcefully.");
					Gui.this.textArea.setCaretPosition(Gui.this.textArea.getDocument().getLength());
				}
			});
		}
	}
	
	public static void main(String[] args) {
		
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		
		Gui gui = new Gui();
		gui.pack();
		gui.setLocationByPlatform(true);
		gui.setVisible(true);
	}
}
