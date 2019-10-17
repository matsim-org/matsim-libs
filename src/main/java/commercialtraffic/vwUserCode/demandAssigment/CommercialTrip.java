package commercialtraffic.vwUserCode.demandAssigment;

public class CommercialTrip {

	int id;
	int unternehmensID;
	int fahrtID;
	String unternehmenszelle;
	String wirtschaftszweig;
	int fahrzeugtyp;
	int zweck;
	String quellzelle;
	String zielzelle;
	int art_ziel;
	double fahrzeit;
	String customerRelation;

	CommercialTrip(int id, int unternehmensID, int fahrtID, String unternehmenszelle, String wirtschaftszweig,
			int fahrzeugtyp, int zweck, String quellzelle, String zielzelle, int art_ziel, double fahrzeit,String customerRelation) {
		this.id = id;
		this.unternehmensID = unternehmensID;
		this.fahrtID = fahrtID;
		this.unternehmenszelle = unternehmenszelle;
		this.wirtschaftszweig = wirtschaftszweig;
		this.fahrzeugtyp = fahrzeugtyp;
		this.zweck = zweck;
		this.quellzelle = quellzelle;
		this.zielzelle = zielzelle;
		this.art_ziel = art_ziel;
		this.fahrzeit = fahrzeit;
		this.customerRelation = customerRelation;
	}
	public int getvehId() {
		return id;
		
	}

}
