/**
 * The experiments of the study: each one sweeps a parameter of the matrix and reports what it buys and
 * what it costs.
 * <p>
 * Separate from {@code traveltimematrix.evaluation} on purpose, and the difference is worth stating
 * because the two do overlap. The evaluation answers <em>how wrong is this matrix</em> for one setting,
 * and it is the tool an analysis reaches for before trusting a matrix it is about to use. An experiment
 * answers <em>which setting should we use</em>, by building many matrices and putting their error next
 * to their memory and their runtime - it is a figure in a paper, not a step in a pipeline. So an
 * experiment reuses the evaluation's pieces (the sampling, the routing, the metrics) and adds the two
 * things a sweep needs and a single evaluation does not: a cost model that can refuse a configuration
 * before building it ({@link org.matsim.contrib.msp.traveltimematrix.experiments.MatrixFootprint}) and a
 * benchmark that is honest about things four orders of magnitude apart in speed
 * ({@link org.matsim.contrib.msp.traveltimematrix.experiments.QueryBenchmark}).
 * <p>
 * Dependencies run experiments &rarr; evaluation &rarr; matrix, and never back.
 *
 * @see org.matsim.contrib.msp.traveltimematrix.experiments.RunMatrixResolutionExperiment experiment 1:
 * the resolution of the two halves of the matrix against accuracy and cost
 */
package org.matsim.contrib.msp.traveltimematrix.experiments;
