package playground.artemc.heterogeneity;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.util.HashMap;


/**
 * Created by artemc on 29/1/15.
 */
public interface IncomeHeterogeneity {
	/** the name to which heterogeneity should be associated in Scenario */
	public final static String ELEMENT_NAME = "HeterogeneityDimension";

	public String getName();

	public String getType();
	public HashMap<Id<Person>, Double> getIncomeFactors();
	public HashMap<Id<Person>, Double> getBetaFactors();

	public static final String TYPE_PROPORTIONAL = "proportional";
	public static final String TYPE_ALPHA = "alpha";
	public static final String TYPE_GAMMA = "gamma";
}
