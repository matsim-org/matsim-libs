package playground.dziemke.potsdam.population;

public class Commuter {

	private String residence;
	private int id;
	private int beschaeftigteSozialversichert;
	private int beschaeftigteGesamt;
	private int autofahrer;
	private int oev;
	
	public Commuter(String residence, int id, int beschaeftigteSozialversichert, int beschaeftigteGesamt, int autofahrer, int oev){
		
		this.residence = residence;
		this.id = id;
		this.beschaeftigteSozialversichert = beschaeftigteSozialversichert;
		this.beschaeftigteGesamt = beschaeftigteGesamt;
		this.autofahrer = autofahrer;
		this.oev = oev;
		
	}

	public String getResidence() {
		return residence;
	}

	public void setResidence(String residence) {
		this.residence = residence;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getBeschaeftigteSozialversichert() {
		return beschaeftigteSozialversichert;
	}

	public void setBeschaeftigteSozialversichert(int beschaeftigteSozialversichert) {
		this.beschaeftigteSozialversichert = beschaeftigteSozialversichert;
	}

	public int getBeschaeftigteGesamt() {
		return beschaeftigteGesamt;
	}

	public void setBeschaeftigteGesamt(int beschaeftigteGesamt) {
		this.beschaeftigteGesamt = beschaeftigteGesamt;
	}

	public int getAutofahrer() {
		return autofahrer;
	}

	public void setAutofahrer(int autofahrer) {
		this.autofahrer = autofahrer;
	}

	public int getOev() {
		return oev;
	}

	public void setOev(int oev) {
		this.oev = oev;
	}

	@Override
	public String toString() {
		return "Commuter [residence=" + residence + ", id=" + id
				+ ", beschaeftigteSozialversichert="
				+ beschaeftigteSozialversichert + ", beschaeftigteGesamt="
				+ beschaeftigteGesamt + ", autofahrer=" + autofahrer + ", oev="
				+ oev + "]";
	}
}