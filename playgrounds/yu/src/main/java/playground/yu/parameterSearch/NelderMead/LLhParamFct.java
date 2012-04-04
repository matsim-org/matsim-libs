/**
 *
 */
package playground.yu.parameterSearch.NelderMead;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.MultivariateRealFunction;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import playground.yu.integration.cadyts.CalibrationConfig;
import playground.yu.parameterSearch.ParametersSetter;
import playground.yu.scoring.withAttrRecorder.leftTurn.LeftTurnPenaltyControler;
import playground.yu.tests.parameterCalibration.naiveWithoutUC.SimCntLogLikelihoodCtlListener;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author C
 *
 */
public class LLhParamFct implements MultivariateRealFunction {
	private final Config cfg;
	private final String[] paramNames;
	private final SimpleWriter writer;

	public LLhParamFct(String configFilename) {
		cfg = ConfigUtils.loadConfig(configFilename);
		String parameterDimensionStr = cfg.findParam(
				CalibrationConfig.BSE_CONFIG_MODULE_NAME, "parameterDimension");

		int paramDim;
		if (parameterDimensionStr != null) {
			paramDim = Integer.parseInt(parameterDimensionStr);
		} else {
			throw new RuntimeException(
					"bse.parameterDimension should NOT be null!");
		}

		paramNames = new String[paramDim];
		for (int i = 0; i < paramDim; i++) {
			paramNames[i] = cfg.findParam(
					CalibrationConfig.BSE_CONFIG_MODULE_NAME,
					CalibrationConfig.PARAM_NAME_INDEX + i);
			if (paramNames[i] == null) {
				throw new RuntimeException("bse.parameterName_" + i
						+ " should NOT be null!");
			}
		}

		writer = new SimpleWriter(cfg.controler().getOutputDirectory()
				+ "/optimization.log");
	}

	@Override
	public double value(double[] point) throws FunctionEvaluationException,
			IllegalArgumentException {
		// check dimension of point and parameters
		int dim = paramNames.length;
		if (dim != point.length) {
			throw new RuntimeException(
					"The point dimension should equals the number of parameters that are being searching.");
		}

		Map<String, Double> nameParameters = new TreeMap<String, Double>();
		for (int i = 0; i < dim; i++) {
			nameParameters.put(paramNames[i], point[i]);
		}
		ParametersSetter.setParametersInConfig(cfg, nameParameters);

		LeftTurnPenaltyControler controler = new LeftTurnPenaltyControler(cfg);

		SimCntLogLikelihoodCtlListener llhListener = new SimCntLogLikelihoodCtlListener();
		controler.addControlerListener(llhListener);

		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);

		controler.run();

		writer.write("point:");
		for (int i = 0; i < dim; i++) {
			writer.write("\t" + point[i]);
		}
		double avgLlh = llhListener.getAverageLoglikelihood();
		writer.writeln("\t--> avg. Log-likelihood:\t" + avgLlh);
		writer.flush();

		return avgLlh;
	}

}
