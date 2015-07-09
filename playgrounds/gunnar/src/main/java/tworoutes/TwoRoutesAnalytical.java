package tworoutes;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TwoRoutesAnalytical {

	// -------------------- MEMBERS --------------------

	// total demand
	final double _D;

	// capacity
	final double _C;

	// -------------------- CONSTRUCTION --------------------

	public TwoRoutesAnalytical(final double _D, final double _C) {
		this._D = _D;
		this._C = _C;
	}

	// -------------------- IMPLEMENTATION --------------------

	public double p1(final double theta) {
		if (theta <= Math.pow(this._D / this._C, 2.0)) {
			return 0.5 * (1.0 - Math.pow(this._C / this._D, 2.0) * theta);
		} else {
			return 0.0;
		}
	}

	public double _Q(final double theta, final double eta) {
		final double q1 = p1(theta) * this._D;
		final double q2 = this._D - q1;
		final double tt1 = Math.pow(q1 / this._C, 2.0);
		final double tt2 = Math.pow(q2 / this._C, 2.0);
		return q1 * (tt1 + eta) + q2 * tt2;
	}

	public double thetaOpt(final double eta) {
		return eta / 3.0;
	}

	// -------------------- MAIN-FUNCTION --------------------

	public static void main(String[] args) {

		final double _D = 1000;
		final double _C = 750;
		final TwoRoutesAnalytical twoRoutes = new TwoRoutesAnalytical(_D, _C);

		final double etaMax = 3 * Math.pow(_D / _C, 2.0);

		for (int i = 0; i <= 10; i++) {
			final double eta = etaMax * i / 10.0;
			final double thetaOpt = twoRoutes.thetaOpt(eta);
			System.out.println("eta = " + eta + ",\ttheta = " + thetaOpt
					+ ",\tp = " + twoRoutes.p1(thetaOpt)
					+ ",\tQ(0.95 * theta) = "
					+ twoRoutes._Q(0.95 * thetaOpt, eta) + ",\tQ(theta) = "
					+ twoRoutes._Q(thetaOpt, eta) + ",\tQ(1.05 * theta) = "
					+ twoRoutes._Q(1.05 * thetaOpt, eta));

		}

	}
}
