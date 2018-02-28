package demand.demandAgent;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;

import demand.demandObject.DemandObject;
import demand.utilityFunctions.UtilityFunction;

public class DemandAgentImpl implements DemandAgent {

	private Id<DemandAgent> id;
	private ArrayList<DemandObject> demandObjects;
	private ArrayList <UtilityFunction> utilityFunctions;
	
	public static class Builder{
		private Id<DemandAgent> id;
		private ArrayList <UtilityFunction> utilityFunctions;
		
		public Builder newInstance() {
			return new Builder();
		}
		
		private Builder() {
			this.utilityFunctions = new ArrayList<UtilityFunction>();
		}
		
		public Builder setId(Id<DemandAgent> id) {
			this.id = id;
			return this;
		}
	
		public Builder addUtilityFunction(UtilityFunction utilityFunction) {
			this.utilityFunctions.add(utilityFunction);
			return this;
		}
	
		public DemandAgentImpl build() {
			return new DemandAgentImpl(this);
		}
	}
	
	private DemandAgentImpl(Builder builder) {
		this.demandObjects = new ArrayList<DemandObject>();
		this.utilityFunctions = new ArrayList<UtilityFunction>();
		this.utilityFunctions = builder.utilityFunctions;
		this.id = builder.id;
	}
	
	
	@Override
	public Id<DemandAgent> getId() {
		return id;
	}

	@Override
	public Collection<DemandObject> getDemandObjects() {
		return demandObjects;
	}

	@Override
	public Collection<UtilityFunction> getUtilityFunctions() {
		return utilityFunctions;
	}

}
