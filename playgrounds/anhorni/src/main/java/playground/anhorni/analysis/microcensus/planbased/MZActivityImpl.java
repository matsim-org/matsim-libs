package playground.anhorni.analysis.microcensus.planbased;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.ActivityImpl;

public class MZActivityImpl extends ActivityImpl {

	private int plz;
	
	public MZActivityImpl(Activity act) {
		super(act);
	}

	public MZActivityImpl(String type, Coord from) {
		super(type, from);
	}

	public int getPlz() {
		return plz;
	}

	public void setPlz(int plz) {
		this.plz = plz;
	}	
}
