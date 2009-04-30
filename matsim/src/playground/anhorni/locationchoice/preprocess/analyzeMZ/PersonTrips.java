package playground.anhorni.locationchoice.preprocess.analyzeMZ;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.world.WorldUtils;

import playground.anhorni.locationchoice.cs.helper.MZTrip;

public class PersonTrips {
	
	
	private Id personId;
	private ArrayList<MZTrip> mzTrips = new ArrayList<MZTrip>();
	private ArrayList<MZTrip> intermediateShoppingTrips = new ArrayList<MZTrip>();
	private ArrayList<MZTrip> roundTripShoppingTrips = new ArrayList<MZTrip>();
	private ArrayList<MZTrip> leisureShoppingTrips = new ArrayList<MZTrip>();
	
	
	public PersonTrips(Id personId, ArrayList<MZTrip> mzTrips) {
		super();
		this.personId = personId;
		this.mzTrips = mzTrips;
	}

	public void addMZTrip(MZTrip trip) {
		this.mzTrips.add(trip);
	}
	
	public Id getPersonId() {
		return personId;
	}
	public void setPersonId(Id personId) {
		this.personId = personId;
	}
	public void setMzTrips(ArrayList<MZTrip> mzTrips) {
		this.mzTrips = mzTrips;
	}
	
	public boolean containsTrips(String type) {
		Iterator<MZTrip> mzTrips_it = mzTrips.iterator();
		while (mzTrips_it.hasNext()) {
			MZTrip mzTrip = mzTrips_it.next();
			if (mzTrip.getShopOrLeisure().equals(type)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean containsImplausibleCoordinates() {
		Iterator<MZTrip> mzTrips_it = mzTrips.iterator();
		while (mzTrips_it.hasNext()) {
			MZTrip mzTrip = mzTrips_it.next();
			if (mzTrip.getCoordEnd().getX() < 1000 || mzTrip.getCoordEnd().getY() < 1000) {
				return true;
			}
		}
		return false;
	}
	
	public boolean containsImplausibleDistances() {
		Iterator<MZTrip> mzTrips_it = mzTrips.iterator();
		while (mzTrips_it.hasNext()) {
			MZTrip mzTrip = mzTrips_it.next();
			if (mzTrip.getCoordStart().calcDistance(mzTrip.getCoordEnd()) < 1.0) {
				return true;
			}
		}
		return false;
	}
	
	
		
	public boolean intersectZH() {	
		
		final double radius = 30000.0;
		final CoordImpl center = new CoordImpl(683518.0,246836.0);
		
		Iterator<MZTrip> mzTrips_it = mzTrips.iterator();
		while (mzTrips_it.hasNext()) {
			MZTrip mzTrip = mzTrips_it.next();
		
			double distance = WorldUtils.distancePointLinesegment(
					mzTrip.getCoordStart(), mzTrip.getCoordEnd(), center);
			
			if (distance <= radius) return true;
		}
		return false;
	}
	
	/*
	 *  9: Auto
	 * 10: Lastwagen
	 * 11: Taxi
	 * 12: Motorrad, Kleinmotorrad
	 * 13: Mofa
	 */
	private void mivShoppingType() {
		
		for (int i = 0; i < this.mzTrips.size(); i++) {
			if (this.mzTrips.get(i).getShopOrLeisure().equals("shop")) {
				if (i < this.mzTrips.size()-1) {
					
					// if MIV as described above ...
					if (Integer.parseInt(this.mzTrips.get(i).getWmittel()) >= 9 &&
							Integer.parseInt(this.mzTrips.get(i).getWmittel()) <= 13) {
						if (checkCoordinates(
								new CoordImpl(this.mzTrips.get(i).getCoordStart().getX(), this.mzTrips.get(i).getCoordStart().getY()),
								new CoordImpl(this.mzTrips.get(i+1).getCoordEnd().getX(), this.mzTrips.get(i+1).getCoordEnd().getY()))) {
							this.intermediateShoppingTrips.add(this.mzTrips.get(i));
							this.intermediateShoppingTrips.add(this.mzTrips.get(i+1));
						}
						else {
							this.roundTripShoppingTrips.add(this.mzTrips.get(i));
							this.roundTripShoppingTrips.add(this.mzTrips.get(i+1));
						}
					}
				}
			}
			else if (this.mzTrips.get(i).getShopOrLeisure().equals("leisure")) {
				if (i < this.mzTrips.size()-1) {
					this.leisureShoppingTrips.add(this.mzTrips.get(i));
					this.leisureShoppingTrips.add(this.mzTrips.get(i+1));
				}
			}
		}		
	}
	
	private boolean checkCoordinates(CoordImpl coordPre, CoordImpl coordPost) {
		if (coordPre.calcDistance(coordPost) <= 0.01) {
			return true;
		}
		return false;
	}
	public List<MZTrip> getIntermediateShoppingTrips() {
		return intermediateShoppingTrips;
	}
	public List<MZTrip> getRoundTripShoppingTrips() {
		return roundTripShoppingTrips;
	}

	public ArrayList<MZTrip> getLeisureShoppingTrips() {
		return leisureShoppingTrips;
	}
	
	public void finish() {
		this.mivShoppingType();
	}
}
