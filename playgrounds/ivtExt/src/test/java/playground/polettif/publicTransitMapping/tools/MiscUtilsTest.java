package playground.polettif.publicTransitMapping.tools;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MiscUtilsTest {
	
	@Test
	public void testListIsSubset() throws Exception {
		List<String> subList = new ArrayList<>();
		subList.add("def");
		subList.add("ghi");

		List<String> reverseSublist = new ArrayList<>();
		reverseSublist.add("ghi");
		reverseSublist.add("def");

		List<String> notSubList = new ArrayList<>();
		notSubList.add("cde");
		notSubList.add("kln");

		List<String> longList = new ArrayList<>();
		longList.add("abc");
		longList.add("def");
		longList.add("ghi");
		longList.add("jkl");
		longList.add("mno");

		List<String> oneEntryList = new ArrayList<>();
		oneEntryList.add("mno");

		assertEquals("subList is not a subset of longList!", true, MiscUtils.listIsSubset(subList, longList));
		assertEquals("reverseSublistist is not a subset of longList!", true, MiscUtils.listIsSubset(reverseSublist, longList));
		assertEquals("notSubList is a subset of longList!", false, MiscUtils.listIsSubset(notSubList, longList));
		assertEquals("oneEntryList is not a subset of longList!", true, MiscUtils.listIsSubset(oneEntryList, longList));
	}
}