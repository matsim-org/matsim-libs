/**
 *
 */
package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.testC1LT;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.collections.Tuple;

import playground.yu.integration.cadyts.CalibrationConfig;
import playground.yu.utils.math.Consistent;
import playground.yu.utils.math.SoonConvergent;

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
		private final Map<String/* paramName */, TreeMap<Integer
		/* array index e.g. iteration/interval */, double[]>> values;
		private int convergencyCheckInterval = 200/* default */, iteration;
		private double amplitudeCriterion = 0.7, avgValueCriterion = 0.1;
		private double consistentCriterion = 0.1;
		private boolean goahead = true;

		// private final String[] toCalibratedParameterNames;

		/**
		 * @param toCalibratedParameterNames
		 * @param preparatoryIteration
		 *            after some warm up iterations, it will be at first
		 *            possible to reckon the prospect of the curve of the
		 *            calibrated parameters, e.g. 400, or bigger
		 */
		public StepSizeSettingSeekerControlerListener(
				String[] toCalibratedParameterNames, int preparatoryIteration) {
			// this.toCalibratedParameterNames = toCalibratedParameterNames;
			this.preparatoryIteration = preparatoryIteration;
			values = new HashMap<String, TreeMap<Integer, double[]>>();

			for (String paramName : toCalibratedParameterNames) {
				values.put(paramName, new TreeMap<Integer, double[]>());
			}
		}

		public boolean isGoahead() {
			return goahead;
		}

		private boolean judgeConsistency() {
			for (String paramName : values.keySet()) {
				TreeMap<Integer, double[]> map = values.get(paramName);
				if (map == null) {
					throw new RuntimeException(
							"This should NOT happen, because the Maps should have been initialized in contructor of StepSizeSettingSeeker!!!");
				}

				int arrayKey/* current or last interval */= iteration
						% convergencyCheckInterval != 0 ? iteration
						/ convergencyCheckInterval : iteration
						/ convergencyCheckInterval - 1;
				double[] paramVals = map.get(arrayKey);
				if (paramVals == null) {
					throw new RuntimeException(
							"The parameter values corresponding to\t"
									+ paramName
									+ "\tand key\t"
									+ arrayKey
									+ "\twere NOT recorded, this should NOT happen!!!");
				}
				double lastParamVal = paramVals[(iteration - 1)
						% convergencyCheckInterval];

				double currentParamVal;
				if (iteration % convergencyCheckInterval != 0) {
					currentParamVal = paramVals[iteration
							% convergencyCheckInterval];
				} else/* (iteration % convergencyCheckInterval == 0) */{
					double[] currentParamVals = map.get(arrayKey + 1);
					if (currentParamVals == null) {
						throw new RuntimeException(
								"The parameter values corresponding to\t"
										+ paramName
										+ "\tand key\t"
										+ (arrayKey + 1)
										+ "\twere NOT recorded, this should NOT happen!!!");
					}
					currentParamVal = currentParamVals[0];
				}
				boolean wasConsistent = Consistent
						.wouldBe(consistentCriterion,
								new Tuple<Double, Double>(lastParamVal,
										currentParamVal));
				if (!wasConsistent) {
					return wasConsistent;
				}
			}
			return true;
		}

		private boolean judgeConvergency() {
			for (String paramName : values.keySet()) {
				TreeMap<Integer, double[]> map = values.get(paramName);
				if (map == null) {
					throw new RuntimeException(
							"\"map<Integer, double[]>==null\" This should NOT happen, because the Maps should have been initialized in contructor of StepSizeSettingSeeker!!!");
				}
				double[] paramVals = map.get(iteration
						/ convergencyCheckInterval - 1);
				if (paramVals == null) {
					throw new RuntimeException(
							"The parameter values corresponding to\t"
									+ paramName
									+ "\tand key\t"
									+ (iteration / convergencyCheckInterval - 1)
									+ "\twere NOT recorded, this should NOT happen!!!");
				}
				boolean soonConvergent = SoonConvergent.wouldBe(
						amplitudeCriterion, avgValueCriterion, paramVals);

				// delete small key-objects
				Set<Integer> keysToDelete = new TreeSet<Integer>();
				for (Integer intg : map.headMap(
						iteration / convergencyCheckInterval).keySet()) {
					keysToDelete.add(intg);
				}
				for (Integer intg : keysToDelete) {
					map.remove(intg);
				}

				if (!soonConvergent) {
					// Do something to quit this turn, because it converges so
					// slowly, that we should try bigger stepSize.
					return soonConvergent;
				}
			}
			return true;
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {
			iteration = event.getIteration();
			Config config = event.getControler().getConfig();
			recordParams(config);
			if (iteration - config.controler().getFirstIteration() >= preparatoryIteration) {
				// by every iteration
				if (!judgeConsistency()) {
					// if false, immediately ends this turn of step size
					// setting trying, if msaExponent < 1.
					String msaExponentStr = config.findParam(
							BSE_CONFIG_MODULE_NAME, "msaExponent");
					if (msaExponentStr == null) {
						throw new RuntimeException(
								"\"msaExpont\" can NOT be found in configfile!!");
					}
					if (Double.parseDouble(msaExponentStr) < 1) {
						System.out.println("we need bigger msaExponent!");
						goahead = false;
						((PCCtlwithLeftTurnPenalty) event.getControler())
								.shutdown(true);
					}
				}
				// by e.g. every 200 Iterations
				if (iteration % convergencyCheckInterval == 0) {
					if (!judgeConvergency()) {
						// if false, immediately ends this turn of step size
						// setting trying
						System.out.println("we need bigger stepSize!");
						goahead = false;
						((PCCtlwithLeftTurnPenalty) event.getControler())
								.shutdown(true);
					}
				}
			}
		}

		private void recordParams(Config config) {
			TreeMap<String, String> scoringParams = config.planCalcScore()
					.getParams();
			for (String paramName : values.keySet()) {
				String paramValStr = scoringParams.get(paramName);
				if (paramValStr == null) /* bse */{
					paramValStr = config.findParam(BSE_CONFIG_MODULE_NAME,
							paramName);
				}
				if (paramValStr == null) {
					throw new RuntimeException("The value of parameter \t\""
							+ paramName + "\"\t can NOT be found!!!");
				}
				double paramVal = Double.parseDouble(paramValStr);

				Map<Integer, double[]> map = values.get(paramName);
				if (map == null) {
					throw new RuntimeException(
							"This should NOT happen, because the Maps should have been initialized in contructor of StepSizeSettingSeeker!!!");
				}
				int arrayKey = iteration / convergencyCheckInterval;
				double[] paramVals = map.get(arrayKey);
				if (paramVals == null) {
					paramVals = new double[convergencyCheckInterval];
					map.put(arrayKey, paramVals);
				}
				paramVals[iteration % convergencyCheckInterval] = paramVal;
			}
		}

		public void setAmplitudeCriterion(double amplitudeCriterion) {
			this.amplitudeCriterion = amplitudeCriterion;
		}

		public void setAvgValueCriterion(double avgValueCriterion) {
			this.avgValueCriterion = avgValueCriterion;
		}

		public void setConsistentCriterion(double consistentCriterion) {
			this.consistentCriterion = consistentCriterion;
		}

		public void setConvergencyCheckInterval(int convergencyCheckInterval) {
			this.convergencyCheckInterval = convergencyCheckInterval;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		int preparatoryIteration = 200,
		/* !!!it is NOT the preparatoryIteration in cadyts */convergenceCheckInterval = 100;

		Config config = ConfigUtils.loadConfig(args[0]);

		String msaExponentStr = config.findParam(BSE_CONFIG_MODULE_NAME,
				"msaExponent");
		double msaExponent = msaExponentStr != null ? Double
				.parseDouble(msaExponentStr) : 0d/* default */;

		String initialStepSizeStr = config.findParam(BSE_CONFIG_MODULE_NAME,
				"initialStepSize");
		double initialStepSize = initialStepSizeStr != null ? Double
				.parseDouble(initialStepSizeStr) : 1d/* default */;

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
		// TODO while
		StepSizeSettingSeekerControlerListener sssscl;
		do {
			msaExponent = (msaExponent + 1d) / 2d;
			initialStepSize *= 5d;
			config.setParam(BSE_CONFIG_MODULE_NAME, "msaExponent",
					Double.toString(msaExponent));
			config.setParam(BSE_CONFIG_MODULE_NAME, "initialStepSize",
					Double.toString(initialStepSize));

			PCCtlwithLeftTurnPenalty controler = new PCCtlwithLeftTurnPenalty(
					config);
			sssscl = new StepSizeSettingSeekerControlerListener(paramNames,
					preparatoryIteration);
			controler.addControlerListener(sssscl);
			controler.run();
		} while (!sssscl.isGoahead());
	}
}
