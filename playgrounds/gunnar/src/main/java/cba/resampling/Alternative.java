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

	public double getSampersChoiceProbability();

	public EpsilonDistribution getEpsilonDistribution();

}
