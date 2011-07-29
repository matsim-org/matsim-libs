package playground.fhuelsmann.emission.objects;

public enum ColdPollutant {
	
	FC("FC"), HC("HC"), CO("CO"), NOx("NOx"), NO2("NO2"), PM("PM");
	
	private String key;

	ColdPollutant(String key) {
		this.key = key;
	}

	public String getText() {
		return key;
	}
			
}
