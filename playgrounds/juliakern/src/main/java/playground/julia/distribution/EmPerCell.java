package playground.julia.distribution;

import org.matsim.api.core.v01.Id;

public class EmPerCell {
	
	private Integer xBin;
	private Integer yBin;
	private Id responsiblePersonId;
	private Double concentration;
	private Double emissionEventStartTime;
	
	public EmPerCell(Integer xBin, Integer yBin, Id personId, Double concentration, Double emissisionEventStartTime) {
		this.xBin = xBin;
		this.yBin = yBin;
		this.responsiblePersonId = personId;
		this.concentration = concentration;
		this.emissionEventStartTime = emissisionEventStartTime;
	}

	public Integer getXbin() {
		return this.xBin;
	}
	
	public Integer getYbin() {
		return this.yBin;
	}

	public Id getPersonId() {
		return this.responsiblePersonId;
	}

	public Double getConcentration() {
		return this.concentration;
	}

	public Double getEmissionEventStartTime() {
		return emissionEventStartTime;
	}

}
