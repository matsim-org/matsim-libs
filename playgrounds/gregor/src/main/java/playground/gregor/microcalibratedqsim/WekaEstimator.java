package playground.gregor.microcalibratedqsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import weka.classifiers.Classifier;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class WekaEstimator implements LinkVelocityClassifier {

	private static final Logger log = Logger.getLogger(WekaEstimator.class);
	
	Classifier est = new MultilayerPerceptron();
	Instances is = null;

	private Id[] ids;
	
	double min = Double.POSITIVE_INFINITY;;
	double max = 0;
	@Override
	public double getEstimatedVelocity(double[] onLink) {
		Instance i = new Instance(1,onLink);
		i.setDataset(this.is);
		
		Exception e;
		try {
			double ret = Math.max(this.min, Math.min(this.est.classifyInstance(i),this.max));
			return ret;
		} catch (Exception ex) {
			e = ex;
		}
		throw new RuntimeException(e);
	}

	@Override
	public void addInstance(double[] input, double output) {
		Instance i = new Instance(1, input);
		if (this.is == null) {
			FastVector fv = new FastVector();
			for (Id id : this.ids) {
				Attribute a = new Attribute(id.toString());
				fv.addElement(a);
			}
			this.is = new Instances("test",fv, 0);
			this.is.setClassIndex(0);
		}
		i.setDataset(this.is);
		
		i.setClassValue(output);
		this.is.add(i);
		
		if (output > this.max) {
			this.max = output;
		} else if (output < this.min) {
			this.min = output;
		}
		
	}

	@Override
	public boolean isCalibrationMode() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void validateAndBuild() {
		// TODO Auto-generated method stub

	}

	@Override
	public void build() {
		try {
			this.est.buildClassifier(this.is);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("est build. min val=" + this.min + " max val=" + this.max);
	}

	@Override
	public void setIds(Id[] ids) {
		this.ids = ids;

	}

	@Override
	public Id[] getIds() {
		return this.ids;
	}

}
