package facilities;

import org.matsim.api.core.v01.Id;


public class Warehouse extends Facility {

	private static String TYPE = "warehouse";
	
	public Warehouse(Id id) {
		super(id);
	}

	@Override
	public String getType() {
		return TYPE;
	}
}