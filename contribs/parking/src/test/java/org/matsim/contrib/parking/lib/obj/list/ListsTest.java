package org.matsim.contrib.parking.lib.obj.list;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;
import org.matsim.contrib.parking.parkingchoice.lib.obj.list.Lists;

public class ListsTest {

	@Test public void testBasic(){
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
