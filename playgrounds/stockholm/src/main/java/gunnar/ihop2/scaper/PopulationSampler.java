package gunnar.ihop2.scaper;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import javax.swing.JOptionPane;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.UnicodeInputStream;

public final class PopulationSampler{

	//Call this method with null if network file is not required, otherwise pass network file path
	public void createSample(String filename, String networkfilename, double samplesize, String destFilename) {
		final String srcFilename = filename;
		File srcFile = new File(srcFilename);
		File networkfile = null;
		if(!(networkfilename.equals("") || networkfilename.equals(null))){
			networkfile = new File(networkfilename);;
		}
		if (!srcFile.exists()) {
			System.out.println("The specified file could not be found: " + srcFilename);
			return;
		}
		File destFile = new File(destFilename);
		try {
			createSample(srcFile, networkfile, samplesize, destFile);
		} catch (RuntimeException | IOException ex) {
			destFile.delete();
			System.out.println("Failed...");
			return;
		}
		System.out.println("Done...");
	}
	
	public void createSample(final File inputPopulationFile, final File networkFile, final double samplesize, final File outputPopulationFile) throws RuntimeException, IOException {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		if (networkFile != null) {
			try (FileInputStream fis = new FileInputStream(networkFile);
				BufferedInputStream is = (networkFile.getName().toLowerCase(Locale.ROOT).endsWith(".gz")) ? 
					new BufferedInputStream(new UnicodeInputStream(new GZIPInputStream(fis))) :
						new BufferedInputStream(new UnicodeInputStream(fis))
				) {
				try {
					new MatsimNetworkReader(sc).parse(is);
				}finally {
					;
				} 
			}
		}
		
		PopulationImpl pop = (PopulationImpl) sc.getPopulation();
		pop.setIsStreaming(true);
		PopulationWriter writer = null;
		try {
			writer = new PopulationWriter(pop, null, samplesize);
			writer.startStreaming(outputPopulationFile.getAbsolutePath());
			pop.addAlgorithm(writer);
			try (FileInputStream fis = new FileInputStream(inputPopulationFile);
				BufferedInputStream is = (inputPopulationFile.getName().toLowerCase(Locale.ROOT).endsWith(".gz")) ? 
					new BufferedInputStream(new UnicodeInputStream(new GZIPInputStream(fis))) :
						new BufferedInputStream(new UnicodeInputStream(fis))
				) {
				try {
					new MatsimPopulationReader(sc).parse(is);
				} finally {
					;
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

