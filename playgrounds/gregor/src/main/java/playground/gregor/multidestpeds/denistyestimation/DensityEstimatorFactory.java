package playground.gregor.multidestpeds.denistyestimation;

public class DensityEstimatorFactory {

	public NNGaussianKernelEstimator createDensityEstimator() {
		NNGaussianKernelEstimator ret = new NNGaussianKernelEstimator();
		ret.addGroupId("r");
		ret.addGroupId("g");
		return ret;
	}
}
