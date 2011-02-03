package playground.fhuelsmann.emission;

import org.matsim.api.core.v01.Id;


public interface analyseModul {
	
	public void calculateEmissionsPerLink(final double travelTime, final Id linkId, final double averageSpeed, final int roadType, final int freeVelocity, final double distance,HbefaObject[][] hbefaTable);
	

}
