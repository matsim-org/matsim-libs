/**
 * 
 */
package playground.yu.utils;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.CountControlerListener;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;

/**
 * enables {@code Counts} comparison and writing .kmz file to work for each
 * iteration, and tests the counts comparison effects with different
 * countsScaleFactor
 * 
 * @author yu
 * 
 */
public class SingleIterationCountControlerListener extends
		CountControlerListener {
	private final double minScaleFactor, maxScaleFactor, scaleFactorInterval;

	/**
	 * @param config
	 */
	public SingleIterationCountControlerListener(Config config,
			double minScaleFactor, double maxScaleFactor,
			double scaleFactorInterval) {
		super(config);
		this.minScaleFactor = minScaleFactor;
		this.maxScaleFactor = maxScaleFactor;
		this.scaleFactorInterval = scaleFactorInterval;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		// SET COUNTS_SCALE_FACTOR
		for (double scaleFactor = this.minScaleFactor; scaleFactor < this.maxScaleFactor; scaleFactor += this.scaleFactorInterval) {
			this.runCountsComparisonAlgorithmAndOutput(event, scaleFactor);
		}

	}

	private void runCountsComparisonAlgorithmAndOutput(
			IterationEndsEvent event, double scaleFactor) {
		Controler controler = event.getControler();
		Config config = controler.getConfig();
		Network network = controler.getNetwork();
		CountsConfigGroup countsConfigGroup = config.counts();

		controler.stopwatch
				.beginOperation("compare with counts, scaleFactor =\t"
						+ scaleFactor);

		CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(
				controler.getLinkStats(), getCounts(), network, scaleFactor);

		if ((countsConfigGroup.getDistanceFilter() != null)
				&& (countsConfigGroup.getDistanceFilterCenterNode() != null)) {
			cca.setDistanceFilter(countsConfigGroup.getDistanceFilter(),
					countsConfigGroup.getDistanceFilterCenterNode());
		}

		cca.setCountsScaleFactor(scaleFactor);

		cca.run();

		String outputFormat = countsConfigGroup.getOutputFormat();
		if (outputFormat.contains("kml") || outputFormat.contains("all")) {
			int iteration = event.getIteration();

			String filename = controler.getControlerIO().getIterationFilename(
					iteration, "countscompare" + ".sf" + scaleFactor + ".kmz");
			CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
					cca.getComparison(), network,
					TransformationFactory.getCoordinateTransformation(config
							.global().getCoordinateSystem(),
							TransformationFactory.WGS84));
			kmlWriter.setIterationNumber(iteration);
			kmlWriter.writeFile(filename);// biasErrorGraphData.txt will be
											// written here
		}

		// controler.getLinkStats().reset(); // This is, presumably, a good
		// place where CalcLinkStats.reset() could be called.
		// But would need to be tested. kai, jan'11
		controler.stopwatch.endOperation("compare with counts, scaleFactor =\t"
				+ scaleFactor);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler controler = new Controler(args[0]);
		controler
				.addControlerListener(new SingleIterationCountControlerListener(
						controler.getConfig(), Double.parseDouble(args[1]),
						Double.parseDouble(args[2]), Double
								.parseDouble(args[3])));
		controler.run();
	}
}
