package playground.artemc.heterogeneity;

import org.matsim.core.config.experimental.ReflectiveConfigGroup;

import java.util.Map;

/**
 * Created by artemc on 28/1/15.
 */
public class HeterogeneityConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "heterogeneity";

	private static final String INCOME_FILE = "incomeFile";
	private String incomeFile = null;

	private static final String INCOME_ON_TRAVELCOST_LAMBDA = "incomeOnTravelCostLambda";
	private String IncomeOnTravelCostLambda = null;

	private static final String INCOME_ON_TRAVELCOST_TYPE = "incomeOnTravelCostType";
	private String IncomeOnTravelCostType = null;

	private static final String DISTANCE_ON_TRAVELTIME_LAMBDA = "distanceOnTravelTimeLambda";
	private String distanceOnTravelTimeLambda = null;

	public HeterogeneityConfigGroup() {
		super(GROUP_NAME);
	}
	
	@StringGetter(INCOME_FILE)
	public String getIncomeFile() {
		return this.incomeFile;
	}
	@StringSetter(INCOME_FILE)
	public void setIncomeFile(final String incomeFile) {
		this.incomeFile = incomeFile;
	}

	@StringGetter(INCOME_ON_TRAVELCOST_LAMBDA)
	public String getLambdaIncomeTravelcost() { return this.IncomeOnTravelCostLambda;	}
	@StringSetter(INCOME_ON_TRAVELCOST_LAMBDA)
	public void setLambdaIncomeTravelcost(final String incomeOnTravelCostLambda) {
		this.IncomeOnTravelCostLambda = incomeOnTravelCostLambda;
	}

	@StringGetter(INCOME_ON_TRAVELCOST_TYPE)
	public String getIncomeOnTravelCostType() {
		return this.IncomeOnTravelCostType;
	}
	@StringSetter(INCOME_ON_TRAVELCOST_TYPE)
	public void setIncomeOnTravelCostType(final String incomeOnTravelCostType) {
		this.IncomeOnTravelCostType = incomeOnTravelCostType;
	}


	@StringGetter(DISTANCE_ON_TRAVELTIME_LAMBDA)
	public String getDistanceOnTravelTimeLambda() { return this.distanceOnTravelTimeLambda;	}
	@StringSetter(DISTANCE_ON_TRAVELTIME_LAMBDA)
	public void setDistanceOnTravelTimeLambda(final String distanceOnTravelTimeLambda) {
		this.distanceOnTravelTimeLambda = distanceOnTravelTimeLambda;
	}


	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(INCOME_FILE, " file containing incomes of individual agents.");
		map.put(INCOME_ON_TRAVELCOST_LAMBDA," cost-sensitivity parameter with respect to changes in income, as used for continuous interaction in Axhausen et al. (2008).");
		return map;
	}

}
