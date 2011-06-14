package playground.fhuelsmann.emission.objects;

public class FuelSizeHroad {
	
	
	private String trafficSit;
	private String Technology;
	private String SizeClasse;
	private String EmConcept;
	public String getTrafficSit() {
		return trafficSit;
	}
	public void setTrafficSit(String trafficSit) {
		this.trafficSit = trafficSit;
	}
	public String getTechnology() {
		return Technology;
	}
	public void setTechnology(String technology) {
		Technology = technology;
	}
	public String getSizeClasse() {
		return SizeClasse;
	}
	public void setSizeClasse(String sizeClasse) {
		SizeClasse = sizeClasse;
	}
	public String getEmConcept() {
		return EmConcept;
	}
	public void setEmConcept(String emConcept) {
		EmConcept = emConcept;
	}
	public FuelSizeHroad(String trafficSit, String technology,
			String sizeClasse, String emConcept) {
		super();
		this.trafficSit = trafficSit;
		Technology = technology;
		SizeClasse = sizeClasse;
		EmConcept = emConcept;
	}

	public FuelSizeHroad(String traficSituation) {
		setTrafficSit(traficSituation);
		}
	
	
}
