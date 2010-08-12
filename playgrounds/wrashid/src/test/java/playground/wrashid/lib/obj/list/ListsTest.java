package playground.wrashid.lib.obj.list;

import java.util.ArrayList;

import junit.framework.TestCase;

public class ListsTest extends TestCase {

	public void testBasic(){
		ArrayList<String> list=new ArrayList<String>();
		list.add("Hello");
		list.add("World");
		
		char[] charArray=Lists.getCharsOfAllArrayItemsWithNewLineCharacterInbetween(list);
		
		assertEquals(12, charArray.length);
		assertEquals('H', charArray[0]);
		assertEquals('\n', charArray[5]);
		assertEquals('W', charArray[6]);
		assertEquals('\n', charArray[11]);
	}
	
}
