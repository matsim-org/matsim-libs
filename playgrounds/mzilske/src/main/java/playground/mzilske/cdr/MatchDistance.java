package playground.mzilske.cdr;

class MatchDistance {
	
	public static double[] cumul(double[] h) {
		double[] result = new double[h.length];
		double a = 0;
		for (int i=0; i<h.length; i++) {
			a += h[i];
			result[i] = a;
		}
		return result;
	}
	
	public static double[] norm(double[] h) {
		double[] result = new double[h.length];
		double weight = 0.0;
		for (int i=0; i<h.length; i++) {
			weight += h[i];
		}
		for (int i=0; i<h.length; i++) {
			result[i] = h[i] / weight;
		}
		return result;
	}

	public static double emd(double[] u, double[] v) {
		double result = 0.0;
		double[] cu = cumul(norm(u));
		double[] cv = cumul(norm(v));
		for (int i=0; i<u.length; i++) {
			result += Math.abs(cu[i] - cv[i]);
		}
		return result;
	}
	
	public static double[] int2double(int[] h) {
		double[] result = new double[h.length];
		for (int i=0; i<h.length; i++) {
			result[i] = h[i];
		}
		return result;
	}
	
	public static void main(String[] args) {
		System.out.println(emd(new double[]{0, 0, 1, 0, 0, 0, 0}, new double[]{0, 0, 0, 0, 0, 1, 0}));
		System.out.println(emd(new double[]{0, 0, 7, 0, 0, 0, 0}, new double[]{0, 0, 0, 0, 0, 7, 0}));
		System.out.println(emd(new double[]{0, 0, 1, 0, 0, 0, 0}, new double[]{0, 0, 0, 0, 0, 7, 0}));
		System.out.println(emd(new double[]{0, 0, 1, 0, 0, 0, 0}, new double[]{0, 0, 1, 0, 0, 7, 0}));
	}
	
}
