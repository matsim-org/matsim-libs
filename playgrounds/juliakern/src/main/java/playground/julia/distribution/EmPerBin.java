package playground.julia.distribution;

import org.matsim.api.core.v01.Id;

public class EmPerBin {
	
	Integer xBin;
	Integer yBin;
	Id personId;
	Double value;
	
	public EmPerBin(Integer xBin, Integer yBin, Id personId, Double value) {
		this.xBin = xBin;
		this.yBin = yBin;
		this.personId = personId;
		this.value = value;
	}

	public Integer getXbin() {
		// TODO Auto-generated method stub
		return this.xBin;
	}
	
	public Integer getYbin() {
		// TODO Auto-generated method stub
		return this.yBin;
	}

	public Id getPersonId() {
		// TODO Auto-generated method stub
		return this.personId;
	}

	public Double getConcentration() {
		// TODO Auto-generated method stub
		return this.value;
	}

}
