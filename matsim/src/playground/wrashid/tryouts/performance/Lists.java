package playground.wrashid.tryouts.performance;

import java.util.ArrayList;
import java.util.LinkedList;


public class Lists {
	public static void main(String[] args) {
		

		// new ArrayList<Integer>(0): 8250 ms
		// new ArrayList<Integer>(20000000): 4422 ms
		//testArrayListAdd();
		
		// 17814 ms
		//testLinkedListAdd();
		
		// new FastQueue<Integer>(): 6969ms
		// new FastQueue<Integer>(20000000): 4469ms
		//testFastQueueAdd();
		
		// -----------------------------------------------
		
		// 204 ms
		//testArrayListGet (testArrayListAdd());
		
		// infinity...
		//testLinkedListGet (testLinkedListAdd());
		
		// 313 ms
		//testFastQueueGet(testFastQueueAdd());

		//-----------------------------------------------
		
		// new ArrayList<Integer>(0): 2734 ms
		// new ArrayList<Integer>(20000000): 2703 ms
		//testArrayListAddAll(testArrayListAdd());

		// infinity... / OutOfMemory
		//testLinkedListAddAll (testLinkedListAdd());
		
		// new FastQueue<Integer>(): 3672 ms
		// new FastQueue<Integer>(20000000): 3204 ms
		testFastQueueAddAll(testFastQueueAdd());
		
		// -----------------------------------------------
		
		// infinity
		//testArrayListPoll(testArrayListAdd());
		
		// list.size()>0: 1266 ms
		// !list.isEmpty(): 1438 ms
		//testLinkedListPoll(testLinkedListAdd());
		
		// 250 ms
		//testFastQueuePoll(testFastQueueAdd());
		
		// -----------------------------------------------
		// see results in code
		//testMixedAddAll();
		
		
	}

	public static ArrayList<Integer> testArrayListAdd() {
		long time = System.currentTimeMillis();
		ArrayList<Integer> list = new ArrayList<Integer>(20000000);

		for (int i = 0; i < 20000000; i++) {
			list.add(i);
		}
		System.out.println("time [ms]: " + (System.currentTimeMillis() - time));
		return list;
	}
	
	public static LinkedList<Integer> testLinkedListAdd() {
		long time = System.currentTimeMillis();
		LinkedList<Integer> list = new LinkedList<Integer>();

		for (int i = 0; i < 20000000; i++) {
			list.add(i);
		}
		System.out.println("time [ms]: " + (System.currentTimeMillis() - time));
		return list;
	}
	
	public static FastQueue<Integer> testFastQueueAdd() {
		long time = System.currentTimeMillis();
		FastQueue<Integer> list = new FastQueue<Integer>(20000000);

		for (int i = 0; i < 20000000; i++) {
			list.add(i);
		}
		System.out.println("time [ms]: " + (System.currentTimeMillis() - time));
		return list;
	}
	
	// ----------------------------------
	
	public static void testArrayListGet(ArrayList<Integer> list){
		long time = System.currentTimeMillis();
		for (int i=0;i<list.size();i++){
			list.get(i);
		}
		System.out.println("time [ms]: " + (System.currentTimeMillis() - time));
	}
	
	public static void testLinkedListGet(LinkedList<Integer> list){
		long time = System.currentTimeMillis();
		for (int i=0;i<list.size();i++){
			list.get(i);
		}
		System.out.println("time [ms]: " + (System.currentTimeMillis() - time));
	}
	
	public static void testFastQueueGet(FastQueue<Integer> list){
		long time = System.currentTimeMillis();
		for (int i=0;i<list.size();i++){
			list.get(i);
		}
		System.out.println("time [ms]: " + (System.currentTimeMillis() - time));
	}
	
	// ----------------------------------
	
	public static void testArrayListAddAll(ArrayList<Integer> list){
		long time = System.currentTimeMillis();
		ArrayList<Integer> list1 = new ArrayList<Integer>(20000000);
		list1.addAll(list);
		System.out.println("time [ms]: " + (System.currentTimeMillis() - time));
	}
	
	public static void testLinkedListAddAll(LinkedList<Integer> list){
		long time = System.currentTimeMillis();
		LinkedList<Integer> list1 = new LinkedList<Integer>();
		list1.addAll(list);
		System.out.println("time [ms]: " + (System.currentTimeMillis() - time));
	}
	
	public static void testFastQueueAddAll(FastQueue<Integer> list){
		long time = System.currentTimeMillis();
		FastQueue<Integer> list1 = new FastQueue<Integer>(20000000);
		list1.addAll(list);
		System.out.println("time [ms]: " + (System.currentTimeMillis() - time));
	}
	
	// ----------------------------------
	
	public static void testArrayListPoll(ArrayList<Integer> list){
		long time = System.currentTimeMillis();
		while (list.size()>0){
			list.remove(0);
		}
		System.out.println("time [ms]: " + (System.currentTimeMillis() - time));
	}
	
	public static void testLinkedListPoll(LinkedList<Integer> list){
		long time = System.currentTimeMillis();
		while (list.size()>0){
			list.poll();
		}
		System.out.println("time [ms]: " + (System.currentTimeMillis() - time));
	}
	
	public static void testFastQueuePoll(FastQueue<Integer> list){
		long time = System.currentTimeMillis();
		while (list.size()>0){
			list.poll();
		}
		System.out.println("time [ms]: " + (System.currentTimeMillis() - time));
	}
	
	
	
	public static void testMixedAddAll(){
		
		ArrayList<Integer> list = new ArrayList<Integer>(10000000);
		LinkedList<Integer> list2 = new LinkedList<Integer>();

		for (int i = 0; i < 10000000; i++) {
			list.add(i);
			list2.add(i);
		}
		
		long time = System.currentTimeMillis();
		
		// 3704 ms
		//list1.addAll(list);
		
		// 5407 ms
		//for (int i=0;i<list.size();i++){
		//	list1.add(list.get(i));
		//}
		
		// infinity, as before
		//for (int i=0;i<list.size();i++){
		//	list1.addAll(list2);
		//}
		
		
		System.out.println("time [ms]: " + (System.currentTimeMillis() - time));
	}

}
