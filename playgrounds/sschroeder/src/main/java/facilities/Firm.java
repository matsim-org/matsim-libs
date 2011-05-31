package facilities;

import org.matsim.api.core.v01.Id;

public class Firm extends Facility {

	private static String TYPE = "firm";
	
	public Firm(Id id) {
		super(id);
	}

	@Override
	public String getType() {
		return TYPE;
	}

}
