package playground.dziemke.other;

import org.apache.log4j.Logger;

public class TestCharacterStuff {
	private final static Logger log = Logger.getLogger(TestCharacterStuff.class);

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String name = "OTIS GmbH & Co. OHG";
		
		log.info("Building has the name " + name + ".");
		
//		String newName = null;
		if (name.contains("&")) {
			name = name.replaceAll("&", "u");
			System.out.println("lalala");
		}
		
		log.info("Building has the name " + name + ".");
//		log.info("Building has the name " + newName + ".");
	}

}
