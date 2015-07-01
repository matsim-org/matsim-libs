package tworoutes;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TwoRoutesAnalytical {

	static final double p(final double theta, final double _D, final double _C) {
		return 0.5 * (1.0 - theta / Math.pow(_D / _C, 2.0));
	}

	static final double _Q(final double theta, final double eta,
			final double _D, final double _C) {
		final double q1 = p(theta, _D, _C) * _D;
		final double q2 = _D - q1;
		final double tt1 = Math.pow(q1 / _C, 2.0);
		final double tt2 = Math.pow(q2 / _C, 2.0);
		return q1 * tt1 + q2 * tt2 + eta * q1;
	}

	public static void main(String[] args) {

		final double _D = 1000;
		final double _C = 750;
		final double etaMax = 3 * Math.pow(_D / _C, 2.0);

		for (int i = 0; i <= 10; i++) {
			final double eta = etaMax * i / 10.0;
			final double theta = eta / 3.0;
			System.out.println("eta = " + eta + ",\ttheta = " + theta
					+ ",\tp = " + p(theta, _D, _C) + ",\tQ(0.95 * theta) = "
					+ _Q(0.95 * theta, eta, _D, _C) + ",\tQ(theta) = "
					+ _Q(theta, eta, _D, _C) + ",\tQ(1.05 * theta) = "
					+ _Q(1.05 * theta, eta, _D, _C));

		}

	}
}
