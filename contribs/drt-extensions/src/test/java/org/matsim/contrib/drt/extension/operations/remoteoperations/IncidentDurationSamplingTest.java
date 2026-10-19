/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.drt.extension.operations.remoteoperations.config.IncidentSeverityParams.DurationDistribution;

/**
 * Unit tests for the pure incident-duration sampling math ({@link IncidentDispatcher#sampleDuration}). The whole point of
 * the three-family switch is a <em>controlled experiment on service-time variability</em>: LOGNORMAL, EXPONENTIAL and
 * DETERMINISTIC must all share the SAME mean {@code exp(mu + sigma^2/2)} (hence the same service rate for the operator
 * M/G/m queue) and differ ONLY in the squared coefficient of variation (SCV = Var/mean^2): LOGNORMAL has
 * {@code exp(sigma^2)-1}, EXPONENTIAL has 1 (memoryless / exact M/M/m), DETERMINISTIC has 0 (M/D/m). If that invariant
 * ever breaks, any comparison built on it is measuring a sampling bug rather than the distribution
 * effect — so it is locked in here with a large seeded Monte-Carlo sample.
 *
 * @author nkuehnel / MOIA
 */
public class IncidentDurationSamplingTest {

	private static final int N = 2_000_000;
	// log-space params of a right-skewed duration: median exp(mu)=120s, sigma=0.8 -> SCV(lognormal)=exp(0.64)-1≈0.896
	private static final double MU = Math.log(120.0);
	private static final double SIGMA = 0.8;
	private static final double EXPECTED_MEAN = Math.exp(MU + 0.5 * SIGMA * SIGMA);

	@Test
	void allThreeFamiliesShareTheSameMean() {
		// the invariant that makes the switch a controlled experiment: same mean, so same service rate mu across families
		double logNormalMean = sampleMean(DurationDistribution.LOGNORMAL);
		double exponentialMean = sampleMean(DurationDistribution.EXPONENTIAL);
		double deterministicMean = sampleMean(DurationDistribution.DETERMINISTIC);

		// 1% tolerance on the two random families (Monte-Carlo error at N=2e6); deterministic is exact up to the
		// floating-point drift of summing the same constant N times and dividing (hence a tiny tolerance, not isEqualTo)
		assertThat(logNormalMean).isCloseTo(EXPECTED_MEAN, withinPercent(1.0));
		assertThat(exponentialMean).isCloseTo(EXPECTED_MEAN, withinPercent(1.0));
		assertThat(deterministicMean).isCloseTo(EXPECTED_MEAN, withinPercent(1e-6));
	}

	@Test
	void scvMatchesEachDistributionsClosedForm() {
		// SCV is what actually differs between the families - the queueing signal (Allen-Cunneen (1+SCV)/2)
		assertThat(sampleScv(DurationDistribution.DETERMINISTIC)).isZero();
		assertThat(sampleScv(DurationDistribution.EXPONENTIAL)).isCloseTo(1.0, withinPercent(2.0));
		double lognormalScv = Math.exp(SIGMA * SIGMA) - 1.0;
		assertThat(sampleScv(DurationDistribution.LOGNORMAL)).isCloseTo(lognormalScv, withinPercent(3.0));
	}

	@Test
	void deterministicIsConstantAndPositive() {
		Random random = new Random(1L);
		for (int i = 0; i < 1000; i++) {
			double d = IncidentDispatcher.sampleDuration(DurationDistribution.DETERMINISTIC, MU, SIGMA, random);
			assertThat(d).isEqualTo(EXPECTED_MEAN);
		}
	}

	@Test
	void samplesAreAlwaysPositive() {
		// durations feed schedule task lengths; a zero/negative would corrupt the vehicle hold. Exponential inverse-CDF
		// uses 1-U with U in [0,1) so the argument of log stays in (0,1], never log(0).
		Random random = new Random(7L);
		for (DurationDistribution distribution : DurationDistribution.values()) {
			for (int i = 0; i < 100_000; i++) {
				assertThat(IncidentDispatcher.sampleDuration(distribution, MU, SIGMA, random)).isPositive();
			}
		}
	}

	@Test
	void zeroSigmaCollapsesTheMeanBasedFamiliesToExpMu() {
		// with sigma=0 the shared mean is exp(mu+0)=exp(mu). LOGNORMAL degenerates to that constant (exp(mu + 0*gaussian))
		// and DETERMINISTIC returns it exactly - both regardless of the RNG draw. EXPONENTIAL, by contrast, stays
		// memoryless: its SCV is 1 independent of sigma, so individual draws remain random around the same mean exp(mu).
		double expMu = Math.exp(MU);
		Random random = new Random(3L);
		for (int i = 0; i < 1000; i++) {
			assertThat(IncidentDispatcher.sampleDuration(DurationDistribution.LOGNORMAL, MU, 0.0, random)).isEqualTo(expMu);
			assertThat(IncidentDispatcher.sampleDuration(DurationDistribution.DETERMINISTIC, MU, 0.0, random)).isEqualTo(expMu);
		}
		// exponential with sigma=0 still averages to exp(mu) but is not constant
		Random expRandom = new Random(3L);
		double sum = 0.0;
		for (int i = 0; i < N; i++) {
			sum += IncidentDispatcher.sampleDuration(DurationDistribution.EXPONENTIAL, MU, 0.0, expRandom);
		}
		assertThat(sum / N).isCloseTo(expMu, withinPercent(1.0));
	}

	private static double sampleMean(DurationDistribution distribution) {
		Random random = new Random(42L);
		double sum = 0.0;
		for (int i = 0; i < N; i++) {
			sum += IncidentDispatcher.sampleDuration(distribution, MU, SIGMA, random);
		}
		return sum / N;
	}

	private static double sampleScv(DurationDistribution distribution) {
		Random random = new Random(42L);
		// Welford one-pass mean/variance to stay numerically stable over 2e6 samples
		double mean = 0.0;
		double m2 = 0.0;
		for (int i = 1; i <= N; i++) {
			double x = IncidentDispatcher.sampleDuration(distribution, MU, SIGMA, random);
			double delta = x - mean;
			mean += delta / i;
			m2 += delta * (x - mean);
		}
		double variance = m2 / N;
		return variance / (mean * mean);
	}

	private static org.assertj.core.data.Percentage withinPercent(double pct) {
		return org.assertj.core.data.Percentage.withPercentage(pct);
	}
}
