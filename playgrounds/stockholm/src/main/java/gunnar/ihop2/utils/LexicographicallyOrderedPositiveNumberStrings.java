package gunnar.ihop2.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LexicographicallyOrderedPositiveNumberStrings {

	private int maxValue;

	private final String zeros;

	public LexicographicallyOrderedPositiveNumberStrings(final int maxValue) {
		this.maxValue = maxValue;
		final int digits;
		if (maxValue == 0) {
			digits = 1;
		} else {
			digits = ((int) Math.log10(maxValue)) + 1;
		}
		final StringBuffer zeroBuffer = new StringBuffer("0");
		while (zeroBuffer.length() < digits) {
			zeroBuffer.append("0");
		}
		this.zeros = zeroBuffer.toString();
	}

	public String toString(final int number) {
		if ((number < 0) || (number > this.maxValue)) {
			throw new IllegalArgumentException(number + " is not in {0,...,"
					+ maxValue + "}");
		}
		final String secondPart = Integer.toString(number);
		return this.zeros.substring(0,
				this.zeros.length() - secondPart.length())
				+ secondPart;
	}
	
	public static final void main(String[] args) {
		
		LexicographicallyOrderedPositiveNumberStrings test = new LexicographicallyOrderedPositiveNumberStrings(100);
		final List<String> list = new ArrayList<>();
		for (int i = 0; i <= 100; i++) {
			list.add(test.toString(i));
		}
		Collections.shuffle(list);
		Collections.sort(list);
		for (String entry : list) {
			System.out.println(entry);
		}
	}

}
