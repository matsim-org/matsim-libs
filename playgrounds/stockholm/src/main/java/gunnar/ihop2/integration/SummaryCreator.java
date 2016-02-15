package gunnar.ihop2.integration;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.matsim.core.config.ConfigUtils;

class SummaryCreator {

	public SummaryCreator() {
	}

	static void run(final int maxIterations) {
		final String lastMATSimIteration = ConfigUtils
				.loadConfig("./input/matsim-config.xml").getModule("controler")
				.getValue("lastIteration");

		for (int iteration = 1; iteration <= maxIterations; iteration++) {
			final String fromPath = "./matsim-output." + iteration + "/";
			final String toPath = "./summary/iteration-" + iteration + "/";
			try {
				FileUtils.copyDirectory(new File(fromPath + "ITERS/it."
						+ lastMATSimIteration), new File(toPath + "it."
						+ lastMATSimIteration));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				FileUtils.copyFileToDirectory(new File(
						"./departure-time-histograms." + iteration + ".txt"),
						new File("./summary/"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				FileUtils.copyFileToDirectory(new File(
						"./travel-cost-statistics." + iteration + ".txt"),
						new File("./summary/"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				FileUtils.copyFileToDirectory(
						new File(fromPath + "logfile.log"), new File(toPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				FileUtils.copyFileToDirectory(new File(fromPath
						+ "logfileWarningsErrors.log"), new File(toPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				FileUtils.copyFileToDirectory(new File(fromPath
						+ "stopwatch.png"), new File(toPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				FileUtils.copyFileToDirectory(new File(fromPath
						+ "scorestats.png"), new File(toPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				FileUtils.copyFileToDirectory(new File(fromPath
						+ "stopwatch.txt"), new File(toPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				FileUtils.copyFileToDirectory(new File(fromPath
						+ "scorestats.txt"), new File(toPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			FileUtils.copyFileToDirectory(new File("./log.txt"), new File(
					"./summary/"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			FileUtils.copyFileToDirectory(new File("./config.xml"), new File(
					"./summary/"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			FileUtils.copyFileToDirectory(
					new File("./input/matsim-config.xml"), new File(
							"./summary/"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		run(1);
	}

}
