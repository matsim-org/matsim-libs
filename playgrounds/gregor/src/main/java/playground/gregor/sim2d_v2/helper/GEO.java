package playground.gregor.sim2d_v2.helper;


public class GEO {


	private static final double PI_HALF = Math.PI / 2;
	private static final double TWO_PI = 2 * Math.PI;
	/**
	 * @param newPos
	 * @param oldPos
	 * @return
	 */
	public static double getAzimuth(double dX, double dY) {
		double alpha = 0.0;
		if (dX > 0) {
			alpha = Math.atan(dY / dX);
		} else if (dX < 0) {
			alpha = Math.PI + Math.atan(dY / dX);
		} else { // i.e. DX==0
			if (dY > 0) {
				alpha = PI_HALF;
			} else {
				alpha = -PI_HALF;
			}
		}
		if (alpha < 0.0)
			alpha += TWO_PI;
		return alpha;
	}
}
