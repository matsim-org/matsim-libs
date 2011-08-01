package playground.fhuelsmann.emission.objects;

public enum WarmPollutant {
	
	FC("FC"), NOX("NOx"), CO2_TOTAL("CO2total"), NO2("NO2"), PM("PM");
	
	private String key;

	WarmPollutant(String key) {
		this.key = key;
	}

	public String getText() {
		return key;
	}
			
}
