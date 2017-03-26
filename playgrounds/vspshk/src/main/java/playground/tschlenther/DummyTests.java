package playground.tschlenther;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import playground.tschlenther.parkingSearch.utils.ParkingTuple;
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
		
		
		String pathToZoneFile = "blablabla/basjfkj/runs/meineZone.txt";
		String zone = pathToZoneFile.substring(pathToZoneFile.lastIndexOf("/")+1, pathToZoneFile.lastIndexOf("."));
		System.out.println(zone);
		
		System.out.println("--");
	
		System.out.println("Viertelstunde:" + (int) (39551/900) );
		System.out.println("halbe stunde:" + (int) (39551/1800) );
		System.out.println("Stunde:" + (int) (39551/3600) );
		
	}
	
	

	private static void testArray(){
		double[] arr1 = new double[3];
		
		arr1[0] = 1;
		arr1[1] = 2;
		arr1[2] = 3;
		System.out.println("\n ARRAY \n" + printarray(arr1));
		
		System.out.println("länge des arrays= " + arr1.length);
		System.out.println("letztes element= " + arr1[arr1.length-1]);

		double[] arr2 = new double[1];
		System.out.println("\n ARRAY \n" + printarray(arr2));
		System.out.println("länge des arrays= " + arr2.length);
		System.out.println("letztes element= " + arr2[arr2.length-1]);

		arr2[0] = 1;
		
		System.out.println("\n ARRAY \n" + printarray(arr2));
		
		
	}
	
	private static void testTreeSet(){
		System.out.println("--------------------TREEEEESEEEEEET------------------");
		
		TreeSet treeSet = new TreeSet();
		treeSet.add(1.0d);
		treeSet.add(4.3d);
		treeSet.add(4.1d);
		treeSet.add(5.0d);
		
		System.out.println("Treeset:\n" + treeSet.toString());
		TreeSet pSet = new TreeSet<ParkingTuple>();
		pSet.add(new ParkingTuple(1000,0));
		pSet.add(new ParkingTuple(500,2));
		pSet.add(new ParkingTuple(10000,1));
		pSet.add(new ParkingTuple(3000,0.6));
		
		System.out.println("\n\n pSet :\n" + pSet.toString());

	}
	
	private static String printarray(double[] arr){
		String str = "";
		for(int i = 0; i< arr.length; i++){
			str += "" + i + ":" + arr[i] + "\n";
		}
		return str;
	}

}
