package playground.mzilske.ulm;


public class Main {

	public static void main(String[] args) {
		UlmResource res = new UlmResource("/Users/michaelzilske/gtfs-ulm");
		res.convert();
		res.population();
		res.run();
		res.otfvis();
	}




}
