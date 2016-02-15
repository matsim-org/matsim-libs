package playground.gthunig.vw_rufbus.demandGeneration;

public class VWRConfig {

	private String basedir;
	
	private String network;
	private String counties;

	private String commercial;
	private String industrial;
	private String residential;
	private String retail;
	private String schools;
	private String universities;
	
	
	
	private String plansOutputComlpete;
	
	private double scalefactor = 1.0;

	private String transitSchedule;

	private String objectAttributes;

	private String bs;

	private String bsb;

	private String wb;
	
	public VWRConfig(String basedir, String network, String counties, String commercial, String industrial, String residential, 
						String retail, String schools, String universities, String plansOutputComlpete ,double scalefactor, String transitSchedule, String objectAttributes, String bs, String bsb, String wb) {
		this.basedir = basedir;
		
		this.network = network;
		this.counties = counties;
		
		this.commercial = commercial;
		this.industrial = industrial;
		this.residential = residential;
		this.retail = retail;
		this.schools = schools;
		this.universities = universities;
		this.objectAttributes = objectAttributes;
		this.plansOutputComlpete = plansOutputComlpete;
		
		this.scalefactor = scalefactor;
		this.transitSchedule = transitSchedule;
		this.bs= bs;
		this.wb = wb;
		this.bsb = bsb;
	}

	public String getNetworkFileString() {
		return this.basedir + this.network;
	}
	public String getObjectAttributes() {
		return objectAttributes;
	}
	public String getBs() {
		return this.basedir + bs;
	}
	public String getBsb() {
		return this.basedir + bsb;
	}
	
	public String getWb() {
		return this.basedir + wb;
	}

	public String getTransitSchedule() {
		return transitSchedule;
	}
	public String getCountiesFileString() {
		return this.basedir + this.counties;
	}

	public String getCommercialFileString() {
		return this.basedir + this.commercial;
	}

	public String getIndustrialFileString() {
		return this.basedir + this.industrial;
	}
	
	public String getResidentialFileString() {
		return this.basedir + this.residential;
	}
	
	public String getRetailFileString() {
		return this.basedir + this.retail;
	}
	
	public String getSchoolsFileString() {
		return this.basedir + this.schools;
	}
	
	public String getUniversitiesFileString() {
		return this.basedir + this.universities;
	}
	
	public String getPlansOutputString() {
		return this.plansOutputComlpete;
	}
	
	public double getScalefactor() {
		return this.scalefactor;
	}
}
