package gunnar.ihop2.utils;

import java.util.Comparator;

public class StringAsIntegerComparator implements Comparator<String> {

	public StringAsIntegerComparator() {
	}

	@Override
	public int compare(final String arg0, final String arg1) {
		return (new Integer(arg0)).compareTo(new Integer(arg1));
	}

}
