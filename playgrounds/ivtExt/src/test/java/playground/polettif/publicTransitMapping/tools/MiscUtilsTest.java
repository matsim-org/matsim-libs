package playground.polettif.publicTransitMapping.tools;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

	@Test
	public void testSetsShareMinOneStringEntry() {
		Set<String> set1 = new HashSet<>();
		set1.add("abc");
		set1.add("def");
		set1.add("ghi");
		set1.add("jkl");
		set1.add("mno");

		Set<String> set2 = new HashSet<>();
		set2.add("jkl");
		set2.add("mno");
		set2.add("pqr");
		set2.add("stu");
		set2.add("vwx");

		Set<String> set3 = new HashSet<>();
		set3.add("pqr");
		set3.add("stu");
		set3.add("vwx");
		set3.add("yza");
		set3.add("bcd");

		assertTrue(MiscUtils.setsShareMinOneStringEntry(set1, set2));
		assertTrue(MiscUtils.setsShareMinOneEntry(set1, set2));
		assertFalse(MiscUtils.setsShareMinOneEntry(set1, set3));

		Set<String> sharedEntries = new HashSet<>();
		sharedEntries.add("pqr");
		sharedEntries.add("stu");
		sharedEntries.add("vwx");
		assertEquals("Wrong shared entries found!", sharedEntries, MiscUtils.getSharedSetEntries(set2, set3));
	}
}