package ah2174;

import static java.io.File.separatorChar;

import java.io.File;
import java.io.IOException;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

public class MATSimRunner {

	public static void main(String[] args) throws IOException {

		System.out
				.println(MATSimRunner.class.getSimpleName() + " has started.");

		/*
		 * PREPARE
		 */
		final String configFileName = args[0];
		final String matlabLinkStatsFile = args[1];
		final String matlabPopStatsFile = args[2];

		/*
		 * RUN THE SIMULATION
		 */
		Config config = ConfigUtils.loadConfig(new File(configFileName)
				.getAbsolutePath());
		Controler controler = new Controler(config);
		
		// TODO Saleem, we need a manual workaround to erase old files:
		// ORIGINAL:
		// controler.setOverwriteFiles(true);
		// ------------------------------------------------------------

		controler.run();

		/*
		 * EXTRACT PARAMETERS
		 */
		final String outputFolder = config.getParam("controler",
				"outputDirectory");
		final String lastIteration = config.getParam("controler",
				"lastIteration");

		/*
		 * GENERATE MATLAB LINKSTATS
		 */
		final String zippedLinkStatsFile = new File(outputFolder, "ITERS"
				+ separatorChar + "it." + lastIteration + separatorChar
				+ "run0." + lastIteration + ".linkstats.txt.gz").toString();
		final String unzippedLinkStatsFile = LinkStatsReader
				.newUnzippedLinkStatsFile(zippedLinkStatsFile);
		LinkStatsReader linkStatsReader = new LinkStatsReader(configFileName,
				unzippedLinkStatsFile, matlabLinkStatsFile, 0.01, 0, 24);
		linkStatsReader.run();
		new File(unzippedLinkStatsFile).delete();

		/*
		 * GENERATE MATLAB POPSTATS
		 */
		final PopulationReader popStatsReader = new PopulationReader(
				configFileName, new File(outputFolder, "ITERS" + separatorChar
						+ "it." + lastIteration + separatorChar + "run0."
						+ lastIteration + ".plans.xml.gz").toString(),
				matlabPopStatsFile);
		popStatsReader.run();

		System.out.println(MATSimRunner.class.getSimpleName() + " is done.");
	}
}
