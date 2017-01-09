package gunnar.ihop2.scaper;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingDeprecated;
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
					new MatsimNetworkReader(sc.getNetwork()).parse(is);
				}finally {
					;
				} 
			}
		}
		
		Population pop = (Population) sc.getPopulation();
		StreamingDeprecated.setIsStreaming(pop, true);
		StreamingPopulationWriter writer = null;
		try {
			writer = new StreamingPopulationWriter(samplesize);
			writer.startStreaming(outputPopulationFile.getAbsolutePath());
			final PersonAlgorithm algo = writer;
			StreamingDeprecated.addAlgorithm(pop, algo);
			try (FileInputStream fis = new FileInputStream(inputPopulationFile);
				BufferedInputStream is = (inputPopulationFile.getName().toLowerCase(Locale.ROOT).endsWith(".gz")) ? 
					new BufferedInputStream(new UnicodeInputStream(new GZIPInputStream(fis))) :
						new BufferedInputStream(new UnicodeInputStream(fis))
				) {
				try {
					new PopulationReader(sc).parse(is);
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

