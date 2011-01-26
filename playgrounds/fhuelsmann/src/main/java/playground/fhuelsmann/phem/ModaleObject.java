package playground.fhuelsmann.phem;
public class ModaleObject {

	//Sek, V, Pe/Prated, n_norm, FC, NOx, CO, HC, PM
	
	private String sek ;
	public ModaleObject(String sek, String v, String pePrated, String n_norm,
			String fC, String nOx, String cO, String hC, String pM) {
		super();
		this.sek = sek;
		V = v;
		PePrated = pePrated;
		this.n_norm = n_norm;
		FC = fC;
		NOx = nOx;
		CO = cO;
		HC = hC;
		PM = pM;
	}
	private String V;
	private String PePrated;
	private String n_norm;
	private String FC;
	private String NOx;
	private String CO;
	
	public String getSek() {
		return sek;
	}
	public void setSek(String sek) {
		this.sek = sek;
	}
	public String getV() {
		return V;
	}
	public void setV(String v) {
		V = v;
	}
	public String getPePrated() {
		return PePrated;
	}
	public void setPePrated(String pePrated) {
		PePrated = pePrated;
	}
	public String getN_norm() {
		return n_norm;
	}
	public void setN_norm(String n_norm) {
		this.n_norm = n_norm;
	}
	public String getFC() {
		return FC;
	}
	public void setFC(String fC) {
		FC = fC;
	}
	public String getNOx() {
		return NOx;
	}
	public void setNOx(String nOx) {
		NOx = nOx;
	}
	public String getCO() {
		return CO;
	}
	public void setCO(String cO) {
		CO = cO;
	}
	public String getHC() {
		return HC;
	}
	public void setHC(String hC) {
		HC = hC;
	}
	public String getPM() {
		return PM;
	}
	public void setPM(String pM) {
		PM = pM;
	}
	String HC;
	String PM;
	
}
