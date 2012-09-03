package playground.mkillat.ba;

public class Angebot {

	String datum;
	String zeit;
	String preis;
	String plaetze;
	String id;
	String nummer;
	
	public Angebot (String datum, String zeit, String preis, String plaetze, String id, String nummer){
		this.datum =datum;
		this.zeit=zeit;
		this.preis=preis;
		this.plaetze=plaetze;
		this.id=id;
		this.nummer=nummer;
	}

	@Override
	public String toString() {
		return "Angebot [datum=" + datum + ", zeit=" + zeit + ", preis="
				+ preis + ", plaetze=" + plaetze + ", id=" + id + ", nummer="
				+ nummer + "]";
	}

	
	
	
}
