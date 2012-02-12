/**
 *
 */
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.testC1LT;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import playground.yu.integration.cadyts.CalibrationConfig;

/**
 * seeks fitting step size setting, i.e. the combination of initialStepSize and
 * msaExponent in cadyts (s. section 3.5.1 im Cadyts-manual {@link http
 * ://transp-or.epfl.ch/cadyts/Cadyts_manual_1-1-0.pdf})
 *
 * @author yu
 *
 */
public class StepSizeSettingSeeker implements CalibrationConfig {
	public static class StepSizeSettingSeekerControlerListener implements
			IterationEndsListener {
		private final double preparatoryIteration;
		private final Map<String/* paramName */, Map<Integer
		/* array index e.g. iteration/interval */, double[]>> values;

		// private final String[] toCalibratedParameterNames;

		/**
		 * @param toCalibratedParameterNames
		 * @param preparatoryIteration
		 *            after some warm up iterations, it will be at first
		 *            possible to reckon the prospect of the curve of the
		 *            calibrated parameters
		 */
		public StepSizeSettingSeekerControlerListener(
				String[] toCalibratedParameterNames, int preparatoryIteration) {
			// this.toCalibratedParameterNames = toCalibratedParameterNames;
			this.preparatoryIteration = preparatoryIteration;
			values = new HashMap<String, Map<Integer, double[]>>();

			for (String paramName : toCalibratedParameterNames) {
				values.put(paramName, new HashMap<Integer, double[]>());
			}
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {
			int iteration = event.getIteration();
			Config config = event.getControler().getConfig();
			if (iteration - config.controler().getFirstIteration() >= preparatoryIteration) {
				judgeConvergency();
				judgeConsistency();
			}
		}

		private void judgeConsistency() {
			// TODO Auto-generated method stub

		}

		private void judgeConvergency() {
			// TODO Auto-generated method stub

		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		int preparatoryIteration = 200,
		/* !!!it is NOT the preparatoryIteration in cadyts */convergenceCheckInterval = 100;

		Config config = ConfigUtils.loadConfig(args[0]);

		String initialMsaExponentStr = config.findParam(BSE_CONFIG_MODULE_NAME,
				"msaExponent");
		double initialMsaExponent = initialMsaExponentStr != null ? Double
				.parseDouble(initialMsaExponentStr) : 0d/* default */;

		String initialInitialStepSizeStr = config.findParam(
				BSE_CONFIG_MODULE_NAME, "initialStepSize");
		double initialInitialStepSize = initialInitialStepSizeStr != null ? Double
				.parseDouble(initialInitialStepSizeStr) : 1d/* default */;

		String parameterDimensionStr = config.findParam(BSE_CONFIG_MODULE_NAME,
				"parameterDimension");
		int paramDim;
		if (parameterDimensionStr != null) {
			paramDim = Integer.parseInt(parameterDimensionStr);
			// e.g.=2 -- [traveling,performing]
		} else {
			throw new RuntimeException("bse.parameterDimension muss be filled!");
		}

		String[] paramNames = new String[paramDim];
		for (int i = 0; i < paramNames.length; i++) {
			paramNames[i] = config.findParam(BSE_CONFIG_MODULE_NAME,
					PARAM_NAME_INDEX + i);
			if (paramNames[i] == null) {
				throw new RuntimeException(PARAM_NAME_INDEX + i
						+ " muss be set!!");
			}
		}
		// TODO
		Controler controler = new PCCtlwithLeftTurnPenalty(config);
		controler
				.addControlerListener(new StepSizeSettingSeekerControlerListener(
						paramNames, preparatoryIteration));
		controler.run();
	}
}
