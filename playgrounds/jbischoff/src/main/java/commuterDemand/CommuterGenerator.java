package commuterDemand;

public class CommuterGenerator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		CommuterDataReader cdr = new CommuterDataReader();
		cdr.fillFilter(12071000);
		cdr.addFilter("12052000");
		cdr.readFile("/Users/JB/Documents/Work/brb/pendlerdaten_brb.csv");
		cdr.printMunicipalities();
		
	}

}
