/**
 * 
 */
package kid.filter;

import kid.KiDSchema;
import kid.Vehicle;

/**
 * @author stefan
 *
 */
public class WeekFilter implements VehicleFilter{

	public boolean judge(Vehicle vehicle) {
		String wochenTagsTyp = vehicle.getAttributes().get(KiDSchema.VEHICLE_WOCHENTAGTYP);
		if(wochenTagsTyp.equals("1") || wochenTagsTyp.equals("2") || wochenTagsTyp.equals("3")){
			return true;
		}
		return false;
	}

}
