/**
 * 
 */
package playground.yu.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.Count;
import org.matsim.counts.CountControlerListener;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.CountSimComparisonImpl;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;

/**
 * enables {@code Counts} comparison and writing .kmz file to work for each
 * iteration, and tests the counts comparison effects with different
 * countsScaleFactor
 * 
 * @author yu
 * 
 */
public class SingleIterationCountControlerListener extends
		CountControlerListener implements StartupListener,
		IterationEndsListener {
	public class CountsComparisonAlgorithm {
		/**
		 * The LinkAttributes of the simulation
		 */
		private final CalcLinkStats linkStats;
		/**
		 * The counts object
		 */
		private Counts counts;
		/**
		 * The result list
		 */
		private final List<CountSimComparison> countSimComp;

		private Node distanceFilterNode = null;

		private Double distanceFilter = null;

		private final Network network;

		private double countsScaleFactor;

		private final Logger log = Logger
				.getLogger(CountsComparisonAlgorithm.class);

		public CountsComparisonAlgorithm(final CalcLinkStats linkStats,
				final Counts counts, final Network network,
				final double countsScaleFactor) {
			this.linkStats = linkStats;
			this.counts = counts;
			this.countSimComp = new ArrayList<CountSimComparison>();
			this.network = network;
			this.countsScaleFactor = countsScaleFactor;
		}

		/**
		 * Creates the List with the counts vs sim values stored in the
		 * countAttribute Attribute of this class.
		 */
		private void compare() {
			double countValue;

			for (Count count : this.counts.getCounts().values()) {
				if (!isInRange(count.getLocId())) {
					continue;
				}
				double[] volumes = this.linkStats.getAvgLinkVolumes(count
						.getLocId());
				if (volumes.length == 0) {
					log.warn("No volumes for link: "
							+ count.getLocId().toString());
					volumes = new double[24];
					// continue;
				}
				for (int hour = 1; hour <= 24; hour++) {
					// real volumes:
					Volume volume = count.getVolume(hour);
					if (volume != null) {
						countValue = volume.getValue();
						double simValue = volumes[hour - 1];
						simValue *= this.countsScaleFactor;
						this.countSimComp.add(new CountSimComparisonImpl(count
								.getLocId(), hour, countValue, simValue));
					} else {
						countValue = 0.0;
					}
				}
			}
		}

		/**
		 * 
		 * @param linkid
		 * @return <code>true</true> if the Link with the given Id is not farther away than the
		 * distance specified by the distance filter from the center node of the filter.
		 */
		private boolean isInRange(final Id linkid) {
			if ((this.distanceFilterNode == null)
					|| (this.distanceFilter == null)) {
				return true;
			}
			Link l = this.network.getLinks().get(linkid);
			if (l == null) {
				log.warn("Cannot find requested link: " + linkid.toString());
				return false;
			}
			double dist = CoordUtils.calcDistance(l.getCoord(),
					this.distanceFilterNode.getCoord());
			return dist < this.distanceFilter.doubleValue();
		}

		/**
		 * 
		 * @return the result list
		 */
		public List<CountSimComparison> getComparison() {
			return this.countSimComp;
		}

		public void run() {
			this.compare();
		}

		/**
		 * Set a distance filter, dropping everything out which is not in the
		 * distance given in meters around the given Node Id.
		 * 
		 * @param distance
		 * @param nodeId
		 */
		public void setDistanceFilter(final Double distance, final String nodeId) {
			this.distanceFilter = distance;
			this.distanceFilterNode = this.network.getNodes().get(
					new IdImpl(nodeId));
		}

		public void setCountsScaleFactor(final double countsScaleFactor) {
			this.countsScaleFactor = countsScaleFactor;
		}
	}

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
		for (double scaleFactor = this.minScaleFactor; scaleFactor <= this.maxScaleFactor; scaleFactor += this.scaleFactorInterval) {
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

			ControlerIO ctlIO = controler.getControlerIO();

			// String filename = ctlIO.getIterationFilename(iteration, "sf"
			// + scaleFactor + "_countscompare" + ".kmz");

			String path = ctlIO.getIterationPath(iteration) + "/sf"
					+ scaleFactor;
			File itDir = new File(path);
			if (!itDir.mkdir()) {
				if (itDir.exists()) {
					System.out.println("Iteration directory " + path
							+ " exists already.");
				} else {
					System.out.println("Could not create iteration directory "
							+ path + ".");
				}
			}
			String filename = path + "/countscompare.kmz";

			CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
					cca.getComparison(), network,
					TransformationFactory.getCoordinateTransformation(config
							.global().getCoordinateSystem(),
							TransformationFactory.WGS84));
			kmlWriter.setIterationNumber(iteration);
			kmlWriter.writeFile(filename);// biasErrorGraphData.txt will
											// be
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
		controler.setOverwriteFiles(true);
		controler.run();
	}
}
