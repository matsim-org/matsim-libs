package playground.ciarif.models.subtours;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.world.Zone;


public class Municipality {


	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	protected final Zone zone;
	protected int k_id;
	protected double income; // average monthly income
	protected int reg_type; // degree of urbanization
	protected double fuelcost; // per liter
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Municipality(Zone zone) {
		this.zone = zone;
	}
	
	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////

	public int compareTo(Municipality other) {
		return ((IdImpl)this.zone.getId()).compareTo((IdImpl)other.zone.getId());
	}

	//////////////////////////////////////////////////////////////////////

	public final Id getId() {
		return this.zone.getId();
	}
	
	public final double getIncome() {
		return this.income;
	}

	public final int getRegType() {
		return this.reg_type;
	}
	
	public final int getCantonId() {
		return this.k_id;
	}
	
	public final double getFuelCost() {
		return this.fuelcost;
	}
	
	public final Zone getZone() {
		return this.zone;
	}
	
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[m_id=" + this.getId() + "]" +
			"[k_id=" + this.k_id + "]" +
			"[income=" + this.income + "]" +
			"[reg_type=" + this.reg_type + "]" +
			"[fuelcost=" + this.fuelcost + "]";
	}
}
