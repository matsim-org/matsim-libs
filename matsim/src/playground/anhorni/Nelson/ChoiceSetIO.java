package playground.anhorni.Nelson;

import java.util.TreeMap;

public class ChoiceSetIO {

	public static void main(String[] args) {
		ChoiceSetIO io = new ChoiceSetIO();
		io.run();
	}
	
	private void run() {		
		ChoiceSetReader reader = new ChoiceSetReader();
		TreeMap<String, Person> persons = reader.read("input/Nelson/ChoiceSet.dat");
		
		ChoiceSetWriter writer = new ChoiceSetWriter();
		writer.write("output/Nelson/ChoiceSetMod.txt", persons);
		System.out.println("Job finished.");
	}
}
