package org.matsim.run.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.misc.ExeRunner;

/**
 * @author mrieser / Senozon AG
 */
public class Gui extends JFrame {
	private JTextField txtConfigfilename;
	private JTextField txtMatsimversion;
	private JTextField txtRam;
	private JTextField txtJvmversion;
	private JTextField txtJvmlocation;
	private JTextField txtOutput;
	
	private JButton btnStartMatsim;
	private JProgressBar progressBar;

	public Gui() {
		setTitle("MATSim");
		
		JLabel lblConfigurationFile = new JLabel("Configuration file:");
		
		txtConfigfilename = new JTextField();
		txtConfigfilename.setText("");
		txtConfigfilename.setColumns(10);
		
		btnStartMatsim = new JButton("Start MATSim");
		btnStartMatsim.setEnabled(false);
		
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
		txtOutput.setText("output");
		txtOutput.setColumns(10);
		
		progressBar = new JProgressBar();
		progressBar.setEnabled(false);
		progressBar.setIndeterminate(true);
		progressBar.setVisible(false);
		
		btnStartMatsim.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				startMATSim();
			}
		});

		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addContainerGap()
							.addComponent(lblJavaLocation))
						.addGroup(groupLayout.createSequentialGroup()
							.addContainerGap()
							.addComponent(lblConfigurationFile))
						.addGroup(groupLayout.createSequentialGroup()
							.addContainerGap()
							.addComponent(lblOutputDirectory))
						.addGroup(groupLayout.createSequentialGroup()
							.addContainerGap()
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
								.addComponent(txtMatsimversion, GroupLayout.DEFAULT_SIZE, 482, Short.MAX_VALUE)
								.addComponent(txtJvmversion, GroupLayout.DEFAULT_SIZE, 482, Short.MAX_VALUE)
								.addComponent(txtJvmlocation, GroupLayout.DEFAULT_SIZE, 482, Short.MAX_VALUE)
								.addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
									.addComponent(txtConfigfilename, GroupLayout.DEFAULT_SIZE, 385, Short.MAX_VALUE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(btnChoose))
								.addComponent(txtOutput, GroupLayout.DEFAULT_SIZE, 482, Short.MAX_VALUE)
								.addComponent(progressBar, GroupLayout.DEFAULT_SIZE, 496, Short.MAX_VALUE))))
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
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblOutputDirectory)
						.addComponent(txtOutput, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblMemory)
						.addComponent(txtRam, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblMb))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(btnStartMatsim)
						.addComponent(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addContainerGap(178, Short.MAX_VALUE))
		);

		getContentPane().setLayout(groupLayout);
	}
	
	public void startMATSim() {
		progressBar.setVisible(true);
		progressBar.setEnabled(true);
		this.btnStartMatsim.setEnabled(false);

		new Thread(new Runnable() {
			@Override
			public void run() {
				String classpath = System.getProperty("java.class.path");
//		String cmd = "\"" + this.txtJvmlocation.getText() + "\" -cp \"" + classpath + "\" -Xmx" + this.txtRam + "m org.matsim.run.Controler \"" + this.txtConfigfilename.getText() + "\"";
				String[] cmdArgs = new String[] {
						txtJvmlocation.getText(),
						"-cp",
						classpath,
						"-Xmx" + txtRam.getText() + "m",
						"org.matsim.run.Controler",
						txtConfigfilename.getText()
				};
				System.out.println(Arrays.toString(cmdArgs));
				int exitcode = ExeRunner.run(cmdArgs, "matsim.log", Integer.MAX_VALUE, new File(txtConfigfilename.getText()).getParent());
				
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						progressBar.setVisible(false);
						btnStartMatsim.setEnabled(true);
					}
				});

				if (exitcode != 0) {
					throw new RuntimeException("There was a problem running MATSim. exit code: " + exitcode);
				}
			}
		}).start();
		
		
		
	}
	
	public static void main(String[] args) {
		Gui gui = new Gui();
		gui.pack();
		gui.setLocationByPlatform(true);
		gui.setVisible(true);
	}
}
