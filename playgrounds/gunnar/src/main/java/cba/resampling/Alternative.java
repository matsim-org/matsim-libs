package cba.resampling;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public interface Alternative {

	public double getSampersOnlyScore();

	public double getSampersTimeScore();

	public double getMATSimTimeScore();

	public void setMATSimTimeScore(double score);
	
	public double getSampersChoiceProbability();

	public EpsilonDistribution getEpsilonDistribution();

	public double getSampersEpsilonRealization();
	
	public void setSampersEpsilonRealization(double eps);
	
}
