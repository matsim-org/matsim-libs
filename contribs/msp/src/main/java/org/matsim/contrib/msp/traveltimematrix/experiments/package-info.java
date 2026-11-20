/**
 * The experiments of the study: each one sweeps a parameter of the matrix and reports what it buys and
 * what it costs.
 * <p>
 * Separate from {@code traveltimematrix.evaluation} on purpose, and the difference is worth stating
 * because the two do overlap. The evaluation is a <b>library</b>: the sampling, the routed references,
 * the error metrics, the exposure diagnostic - the pieces an analysis reaches for before trusting a
 * matrix it is about to use. An experiment is a <b>run of the study</b>: it fixes what the paper holds
 * fixed, varies what the paper varies, and writes the table a figure is drawn from. So an experiment
 * reuses the evaluation's pieces and adds what a study needs and a single evaluation does not: a cost
 * model that can refuse a configuration before building it
 * ({@link org.matsim.contrib.msp.traveltimematrix.experiments.MatrixFootprint}) and a benchmark that is
 * honest about things four orders of magnitude apart in speed
 * ({@link org.matsim.contrib.msp.traveltimematrix.experiments.QueryBenchmark}) - neither of which
 * experiment 3 uses, because its question is accuracy and the cost of a lookup has been answered by
 * experiments 1 and 2.
 * <p>
 * Dependencies run experiments &rarr; evaluation &rarr; matrix, and never back.
 *
 * @see org.matsim.contrib.msp.traveltimematrix.experiments.RunMatrixCostExperiment experiments 1 and 2:
 * study area size, and the resolution of the two halves of the matrix, against accuracy and cost
 * @see org.matsim.contrib.msp.traveltimematrix.experiments.RunCongestionIndexExperiment experiment 3:
 * one exact matrix plus a congestion index, against routing and against the trips of a run
 */
package org.matsim.contrib.msp.traveltimematrix.experiments;

import org.matsim.contrib.msp.traveltimematrix.run.RunCongestionIndexExperiment;
import org.matsim.contrib.msp.traveltimematrix.run.RunMatrixCostExperiment;
