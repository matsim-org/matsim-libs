package playground.mkillat.ba;

public class Angebot {

	String datum;
	String zeit;
	String preis;
	String plaetze;
	String id;
	
	public Angebot (String datum, String zeit, String preis, String plaetze, String id){
		this.datum =datum;
		this.zeit=zeit;
		this.preis=preis;
		this.plaetze=plaetze;
		this.id=id;
	}

	@Override
	public String toString() {
		return "Angebot [datum=" + datum + ", zeit=" + zeit + ", preis="
				+ preis + ", plaetze=" + plaetze + ", id=" + id + "]";
	}
	
	
}
