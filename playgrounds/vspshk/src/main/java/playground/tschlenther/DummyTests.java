package playground.tschlenther;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import playground.tschlenther.processing.testapplet;

public class DummyTests {
	
	public static void main(String[] argS){
		testListIndex();
	}
	
	private static void testListIndex(){
		List<String> ll = new ArrayList<String>();
		ll.add("A");
		ll.add("B");
		ll.add("C");
		ll.add("D");
		ll.add("E");
		
		System.out.println("size of list: " +  ll.size() + "\n last index of B: " + ll.lastIndexOf("B") );
		System.out.println("last index of E: " + ll.lastIndexOf("E") + "\n \n" + ll.toString());
		
		
		HashSet set = new HashSet();
		
		System.out.println("---- now testing HashSet----");
		System.out.println("set size should be 0 and is : " + set.size());
		set.add("a");
		System.out.println("set size should be 1 and is : " + set.size());
		set.add("b");
		System.out.println("set size should be 2 and is : " + set.size());
	}
}
