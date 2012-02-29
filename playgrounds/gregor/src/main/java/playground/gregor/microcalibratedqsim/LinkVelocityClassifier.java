package playground.gregor.microcalibratedqsim;

import org.matsim.api.core.v01.Id;

public interface LinkVelocityClassifier {

	public double getEstimatedVelocity(double[] onLink);
	
	public void addInstance(double[] input, double output);
	
	public boolean isCalibrationMode();
	
	public void build();

	public void setIds(Id[] ids);
	
	public Id[] getIds();

}
