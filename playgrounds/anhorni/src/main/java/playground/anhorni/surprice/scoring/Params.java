package playground.anhorni.surprice.scoring;

import org.matsim.api.core.v01.TransportMode;

import playground.anhorni.surprice.AgentMemory;
import playground.anhorni.surprice.Surprice;

public class Params {

	private double beta_TD = 0.0;
	private double beta_TT = 0.0;
	private double asc = 0.0;
	
	private double lagP = 0.0;
	double lagT = 0.0;
	
	private double constCost = 0.0; // [EUR]
	private double distanceCostFactor = 0.0; // [EUR / m]
				
	public void setParams(String purpose, String mode, AgentMemory memory, String day, double departureTime) {			
		boolean mLag_purpose = false;
		boolean mLag_time = false;		
		// lag effects for tue - sun: 
		if (!day.equals("mon")) {				
			mLag_purpose = memory.getLagPurpose(departureTime, purpose, day, mode);
			mLag_time = memory.getLagTime(departureTime, purpose, day, mode);
		}
		
		distanceCostFactor = Surprice.distanceCost_car;
		constCost = Surprice.constantCost_car;	
		
	// ============= car, other, unkown, pax, mtb ================================	
		// purpose == undef in initialization
		if (purpose.equals("undef") || purpose.equals("work") || purpose.equals("education") || purpose.equals("home")
				|| purpose.equals("business")) {
			beta_TD = Surprice.beta_TD_car_com;
			beta_TT = Surprice.beta_TT_car_com;
		} else if (purpose.equals("shop")) {
			beta_TD = Surprice.beta_TD_car_shp;
			beta_TT = Surprice.beta_TT_car_shp;
		} else if (purpose.equals("leisure")) {
			beta_TD = Surprice.beta_TD_car_lei;
			beta_TT = Surprice.beta_TT_car_lei;
		} 
		lagP = (mLag_purpose? 1 : 0) * Surprice.lag_purpose_car; // if mLag_purpose == true then ( ) = 1 else 0
		lagT = (mLag_time? 1 : 0) * Surprice.lag_time_car;
		asc = Surprice.constant_car;	
							
		// ============= PT =======================================================
		if (TransportMode.pt.equals(mode)) {			
			if (purpose.equals("work") || purpose.equals("education") || purpose.equals("home")
					|| purpose.equals("business")) {
				beta_TD = Surprice.beta_TD_pt_com;
				beta_TT = Surprice.beta_TT_pt_com;
			} else if (purpose.equals("shop")) {
				beta_TD = Surprice.beta_TD_pt_shp;
				beta_TT = Surprice.beta_TT_pt_shp;
			} else if (purpose.equals("leisure")) {
				beta_TD = Surprice.beta_TD_pt_lei;
				beta_TT = Surprice.beta_TT_pt_lei;
			} 
			lagP = (mLag_purpose? 1 : 0) * Surprice.lag_purpose_pt;
			lagT = (mLag_time? 1 : 0) * Surprice.lag_time_pt;
			asc = Surprice.constant_pt;
			
			distanceCostFactor = Surprice.distanceCost_pt;
			constCost = Surprice.constantCost_pt;
		
		// ============= slm =======================================================
		} else if (TransportMode.bike.equals(mode)) {
			if (purpose.equals("work") || purpose.equals("education") || purpose.equals("home") 
					|| purpose.equals("business")) {
				beta_TD = Surprice.beta_TD_bike_com;
				beta_TT = Surprice.beta_TT_bike_com;
			} else if (purpose.equals("shop")) {
				beta_TD = Surprice.beta_TD_bike_shp;
				beta_TT = Surprice.beta_TT_bike_shp;
			} else if (purpose.equals("leisure")) {
				beta_TD = Surprice.beta_TD_bike_lei;
				beta_TT = Surprice.beta_TT_bike_lei;
			} 
			lagP = (mLag_purpose? 1 : 0) * Surprice.lag_purpose_bike;
			lagT = (mLag_time? 1 : 0) * Surprice.lag_time_bike;
			asc = Surprice.constant_bike;
			
			distanceCostFactor = Surprice.distanceCost_bike;
			constCost = Surprice.constantCost_bike; 
		} else if (TransportMode.walk.equals(mode)) {
			if (purpose.equals("work") || purpose.equals("education") || purpose.equals("home") 
					|| purpose.equals("business")) {
				beta_TD = Surprice.beta_TD_walk_com;
				beta_TT = Surprice.beta_TT_walk_com;
			} else if (purpose.equals("shop")) {
				beta_TD = Surprice.beta_TD_walk_shp;
				beta_TT = Surprice.beta_TT_walk_shp;
			} else if (purpose.equals("leisure")) {
				beta_TD = Surprice.beta_TD_walk_lei;
				beta_TT = Surprice.beta_TT_walk_lei;
			} 
			lagP = (mLag_purpose? 1 : 0) * Surprice.lag_purpose_walk;
			lagT = (mLag_time? 1 : 0) * Surprice.lag_time_walk;
			asc = Surprice.constant_walk;
			
			distanceCostFactor = Surprice.distanceCost_walk;
			constCost = Surprice.constantCost_walk;
	}
	}

	public double getBeta_TD() {
		return beta_TD;
	}

	public double getBeta_TT() {
		return beta_TT;
	}

	public double getAsc() {
		return asc;
	}

	public double getLagP() {
		return lagP;
	}

	public double getLagT() {
		return lagT;
	}

	public double getConstCost() {
		return constCost;
	}

	public double getDistanceCostFactor() {
		return distanceCostFactor;
	}
}
