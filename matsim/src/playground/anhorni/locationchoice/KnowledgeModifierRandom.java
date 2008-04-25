package playground.anhorni.locationchoice;

import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Knowledge;
import org.matsim.world.Location;


import java.util.ArrayList;
import java.util.TreeMap;

public class KnowledgeModifierRandom extends KnowledgeModifier {
	
	private final String[] types2change={"leisure"};

	// explore and modify	
	public KnowledgeModifierRandom(TreeMap<Id, ? extends Location> facilities) {
		super(facilities);		
	}

	// random or next best -----------------------------------------------------------------
	public void modify(Knowledge knowledge){	
		Object [] f_array=this.facilities.values().toArray();
		ArrayList<Facility> facilitiesPool=new ArrayList<Facility>();
		
		for (int i=0; i<f_array.length; i++) {
			Facility f = (Facility)f_array[i];
			if (f.getActivity(types2change[0])!=null) {
				facilitiesPool.add(f);
			}
		}
		Facility facility2add = facilitiesPool.get(Gbl.random.nextInt(facilitiesPool.size()));	
		
		Object [] fl_array = knowledge.getFacilities(types2change[0]).values().toArray();
		Facility facility2remove=(Facility)fl_array[Gbl.random.nextInt(fl_array.length)];

		// add a new facility
		knowledge.getFacilities().put(facility2add.getId(), facility2add);

		// remove a facility
		if (fl_array.length>3) {
			knowledge.removeFacility(facility2remove);
		}		
	}	
}
