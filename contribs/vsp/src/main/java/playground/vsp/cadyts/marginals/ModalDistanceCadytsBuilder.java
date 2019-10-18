package playground.vsp.cadyts.marginals;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.measurements.SingleLinkMeasurement;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.misc.Time;

class ModalDistanceCadytsBuilder {

	static final String MARGINALS = "_marginals";

	private static final Logger logger = Logger.getLogger(ModalDistanceCadytsBuilder.class);

	private Config config = null;
	private DistanceDistribution expectedDistanceDistribution;

	ModalDistanceCadytsBuilder setConfig(Config config) {
		this.config = config;
		return this;
	}

	ModalDistanceCadytsBuilder setExpectedDistanceDistribution(DistanceDistribution distanceDistribution) {
		this.expectedDistanceDistribution = distanceDistribution;
		return this;
	}

	AnalyticalCalibrator<Id<DistanceDistribution.DistanceBin>> build() {

		if (config == null || expectedDistanceDistribution == null) {
			throw new IllegalArgumentException("config and expectedDistanceDistribution must be set!");
		}

		CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
		validateTimeBinSize(cadytsConfig.getTimeBinSize());

		AnalyticalCalibrator<Id<DistanceDistribution.DistanceBin>> calibrator = new AnalyticalCalibrator<>(
				config.controler().getOutputDirectory() + "/cadyts" + MARGINALS + ".log",
				MatsimRandom.getLocalInstance().nextLong(),
				cadytsConfig.getTimeBinSize()
		);

		calibrator.setRegressionInertia(cadytsConfig.getRegressionInertia()) ;
		calibrator.setMinStddev(cadytsConfig.getMinFlowStddev_vehPerHour(), SingleLinkMeasurement.TYPE.FLOW_VEH_H);
		calibrator.setMinStddev(cadytsConfig.getMinFlowStddev_vehPerHour(), SingleLinkMeasurement.TYPE.COUNT_VEH);
		calibrator.setFreezeIteration(cadytsConfig.getFreezeIteration());
		calibrator.setPreparatoryIterations(cadytsConfig.getPreparatoryIterations());
		calibrator.setVarianceScale(cadytsConfig.getVarianceScale());
		calibrator.setStatisticsFile(config.controler().getOutputDirectory() + "/calibration-stats"+MARGINALS+".txt");

		calibrator.setBruteForce(cadytsConfig.useBruteForce());
		// I don't think this has an influence on any of the variants we are using. (Has an influence only when plan choice is left
		// completely to cadyts, rather than just taking the score offsets.) kai, dec'13
		// More formally, one would need to use the selectPlan() method of AnalyticalCalibrator which we are, however, not using. kai, mar'14
		if ( calibrator.getBruteForce() ) {
			logger.warn("setting bruteForce==true for calibrator, but this won't do anything in the way the cadyts matsim integration is set up. kai, mar'14") ;
		}

		for (DistanceDistribution.DistanceBin distanceBin : expectedDistanceDistribution.getDistanceBins()) {
			calibrator.addMeasurement(distanceBin.getId(), 0, 86400, distanceBin.getValue(), distanceBin.getStandardDeviation(), SingleLinkMeasurement.TYPE.COUNT_VEH);
		}

		return calibrator;
	}

	private void validateTimeBinSize(double timeBinSize) {
		//get timeBinSize_s and validate it
		if (Time.MIDNIGHT % timeBinSize != 0 ){
			throw new RuntimeException("Cadyts requires a divisor of 86400 as time bin size value .");
		}
		if (timeBinSize % 3600 != 0) {
			throw new RuntimeException("At this point, time bin sizes need to be multiples of 3600.  This is not a restriction " +
					"of Cadyts, but of the counts file format, which only allows for hourly inputs") ;
		}
	}
}
