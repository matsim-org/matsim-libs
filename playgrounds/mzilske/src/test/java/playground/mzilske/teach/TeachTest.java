package playground.mzilske.teach;

import org.junit.Test;
import static org.junit.Assert.*;

public class TeachTest {
	
	@Test
	public void testStringReverser() {
	
		StringReverser reverser = new StringReverser();
		String testString = "Wurst";
		assertEquals(reverser.reverse(), "tsruW");
		
	}
	

}
