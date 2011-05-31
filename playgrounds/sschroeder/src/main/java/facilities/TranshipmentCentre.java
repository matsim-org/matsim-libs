package facilities;

import org.matsim.api.core.v01.Id;

public class TranshipmentCentre extends Facility {

	private static String TYPE = "transhipment centre";
	
	public TranshipmentCentre(Id id) {
		super(id);
		
	}

	@Override
	public String getType() {
		return TYPE;
	}

}
