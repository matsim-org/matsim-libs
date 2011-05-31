package facilities;

import org.matsim.api.core.v01.Id;


public class Shop extends Facility {

	private static String TYPE = "shop";
	
	public Shop(Id id) {
		super(id);
	}

	@Override
	public String getType() {
		return TYPE;
	}
	
}