package playground.gregor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ArrayListVsArray {
	private static final Integer INT_VALUE = 1000;

	public static Integer[] INT_ARRAY = new Integer[500000];
	public static List<Integer> ARRAY_LIST;

	public static void main(String... args) {

		// as the JVM warms up the times will improved

		for(;;) {
			ARRAY_LIST = new ArrayList(Collections.nCopies(INT_ARRAY.length, INT_VALUE));
			Arrays.fill(INT_ARRAY, INT_VALUE);

			readFromArray();
			readFromArrayList();
		}

	}

	private static long readFromArray() {

		Integer j;
		long start = System.nanoTime();

		for (Integer element : INT_ARRAY) {
			j = element;  //
			int i = j+1;
			i++;
		}
		long timeTaken = (System.nanoTime() - start);
		System.out.println("To read " + INT_ARRAY.getClass().getSimpleName() + " takes " + timeTaken + " nano seconds");
		return timeTaken;
	}

	private static long readFromArrayList() {

		Integer j;
		long start = System.nanoTime();
		for (int i = 0; i < INT_ARRAY.length; i++) {
			j = ARRAY_LIST.get(i);
			int ii = j+1;
			ii++;
		}
		long timeTaken = (System.nanoTime() - start);

		System.out.println("To read " + ARRAY_LIST.getClass().getSimpleName() + " takes " + timeTaken + " nano seconds");
		return timeTaken;
	}
}
