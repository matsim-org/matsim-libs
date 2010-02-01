package playground.mmoyo.analysis.counts.chen;

/** A class to store data from a data error file**/ 
public class ErrorData {
	String titel;
	double [] meanRelError = new double[24];
	double [] meanAbsBias = new double[24];
		
	public ErrorData (final String titel){
		this.titel = titel;
	}

	public double [] getMeanRelError() {
		return meanRelError;
	}

	public double [] getMeanAbsBias() {
		return meanAbsBias;
	}
	
}
