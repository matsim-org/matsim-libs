package floetteroed.utilities.searchrepeater;

/**
 * 
 * @author Gunnar Flötteröd
 *
 * @param <D>
 *            the decision variable type
 */
public interface SearchAlgorithm<D> {

	public void run();

	public Double getObjectiveFunctionValue();

	public D getDecisionVariable();

}
