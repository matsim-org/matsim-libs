package org.matsim.run.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SpinnerNumberModel;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.UnicodeInputStream;

/**
 * @author mrieser / Senozon AG
 */
public final class PopulationSampler extends JFrame {

	private static final long serialVersionUID = 1L;

	private JTextField txtPath;
	private JSpinner pctSpinner;
	private JButton btnChoose;
	private JButton btnCreateSample;
	
	public PopulationSampler() {
		setTitle("Create Population Sample");
		
		this.btnChoose = new JButton("Choose…");
		
		JLabel lblinput = new JLabel("Input Population:");
		this.txtPath = new JTextField("");
		JLabel lblpercentage = new JLabel("Sample Size:");
		this.pctSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
		JLabel lblPercentage = new JLabel("%");
		btnCreateSample = new JButton("Create Sample…");
		
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(lblinput)
								.addComponent(lblpercentage))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addGroup(groupLayout.createSequentialGroup()
									.addComponent(pctSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(lblPercentage, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE)
									.addGap(0, 20, Short.MAX_VALUE))
								.addGroup(groupLayout.createSequentialGroup()
									.addComponent(txtPath, GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(btnChoose))))
						.addComponent(btnCreateSample, Alignment.TRAILING))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(btnChoose)
						.addComponent(lblinput)
						.addComponent(txtPath))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblpercentage)
						.addComponent(lblPercentage)
						.addComponent(pctSpinner))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(btnCreateSample)
					.addContainerGap())
		);
		getContentPane().setLayout(groupLayout);
		
		setupComponents();
	}
	
	private void setupComponents() {
		this.btnChoose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				int result = chooser.showOpenDialog(null);
				if (result == JFileChooser.APPROVE_OPTION) {
					File f = chooser.getSelectedFile();
					String filename = f.getAbsolutePath();
					PopulationSampler.this.txtPath.setText(filename);
				}
			}
		});

		this.btnCreateSample.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						createSample();
					}
				}, "SampleCreator").start();
				PopulationSampler.this.setVisible(false);
			}
		});
	}

	private void createSample() {
		final String srcFilename = PopulationSampler.this.txtPath.getText();
		File srcFile = new File(srcFilename);
		if (!srcFile.exists()) {
			JOptionPane.showMessageDialog(null,
			    "The specified file could not be found: " + srcFilename,
			    "File not found!",
			    JOptionPane.ERROR_MESSAGE);
			return;
		}

		final String namePart = srcFilename.substring(0, srcFilename.toLowerCase(Locale.ROOT).lastIndexOf(".xml"));
		final int percentage = ((Integer) PopulationSampler.this.pctSpinner.getValue()).intValue();
		final double samplesize = percentage / 100.0;

		JFileChooser chooser = new SaveFileSaver();
		chooser.setCurrentDirectory(srcFile.getParentFile());
		chooser.setSelectedFile(new File(srcFile.getParentFile(), namePart + "." + Integer.toString(percentage) + "pct.xml.gz"));
		int saveResult = chooser.showSaveDialog(PopulationSampler.this);
		if (saveResult == JFileChooser.APPROVE_OPTION) {
			File destFile = chooser.getSelectedFile();
			try {
				createSample(srcFile, null, samplesize, destFile);
			} catch (RuntimeException | IOException ex) {
				destFile.delete();
				JOptionPane.showMessageDialog(null,
				    "<html>It looks like the population file cannot be parsed without a network file.<br />Please select a matching network file in the next dialog.</html>",
				    "Problems creating pouplation sample",
				    JOptionPane.WARNING_MESSAGE);
				
				JFileChooser netChooser = new JFileChooser();
				netChooser.setCurrentDirectory(srcFile.getParentFile());
				int result = netChooser.showOpenDialog(PopulationSampler.this);
				if (result == JFileChooser.APPROVE_OPTION) {
					File networkFile = netChooser.getSelectedFile();
					try {
						createSample(srcFile, networkFile, samplesize, destFile);
					} catch (RuntimeException | IOException ex2) {
						destFile.delete();
						JOptionPane.showMessageDialog(null,
						    "The population sample cannot be created, as not all necessary data is available.",
						    "Cannot create population sample",
						    JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}		
		
	}
	
	private static void createSample(final File inputPopulationFile, final File networkFile, final double samplesize, final File outputPopulationFile) throws RuntimeException, IOException {
		MutableScenario sc = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		
		if (networkFile != null) {
			try (FileInputStream fis = new FileInputStream(networkFile);
					BufferedInputStream is = (networkFile.getName().toLowerCase(Locale.ROOT).endsWith(".gz")) ? 
							new BufferedInputStream(new UnicodeInputStream(new GZIPInputStream(fis))) :
								new BufferedInputStream(new UnicodeInputStream(fis))
					) {
				AsyncFileInputProgressDialog gui = new AsyncFileInputProgressDialog(fis, "Loading Network…");
				try {
					new MatsimNetworkReader(sc.getNetwork()).parse(is);
				} finally {
					gui.close();
				}
			}
		}
		
//		Population pop = (Population) sc.getPopulation();
		StreamingPopulationReader reader = new StreamingPopulationReader( sc ) ;
		StreamingDeprecated.setIsStreaming(reader, true);
		
		StreamingPopulationWriter writer = null;
		try {
		
			writer = new StreamingPopulationWriter(null, samplesize);
			writer.startStreaming(outputPopulationFile.getAbsolutePath());
			final PersonAlgorithm algo = writer;
			
			reader.addAlgorithm(algo);
			
			try (FileInputStream fis = new FileInputStream(inputPopulationFile);
				BufferedInputStream is = (inputPopulationFile.getName().toLowerCase(Locale.ROOT).endsWith(".gz")) ? 
						new BufferedInputStream(new UnicodeInputStream(new GZIPInputStream(fis))) :
						new BufferedInputStream(new UnicodeInputStream(fis))
				) {
				AsyncFileInputProgressDialog gui = new AsyncFileInputProgressDialog(fis, "Creating Population Sample…");
				try {
//					new MatsimPopulationReader(sc).parse(is);
					reader.parse(is);
				} finally {
					gui.close();
				}
			}
		} catch (NullPointerException e) {
			throw new RuntimeException(e);
		} finally {
			if (writer != null) {
				writer.closeStreaming();
			}
		}
		
	}
}
