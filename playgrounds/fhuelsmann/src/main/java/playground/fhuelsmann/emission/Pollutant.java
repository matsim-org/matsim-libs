package playground.fhuelsmann.emission;

public enum Pollutant {
	
	FC("FC"), NOX("NOx"), CO2_TOTAL("CO2(total)"), NO2("NO"), PM("PM");
	
	private String key;

	Pollutant(String key) {
		this.key = key;
	}

	public String getText() {
		return key;
	}
			
}
