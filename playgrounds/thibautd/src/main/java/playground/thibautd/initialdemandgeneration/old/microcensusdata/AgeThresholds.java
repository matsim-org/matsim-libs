package playground.thibautd.initialdemandgeneration.old.microcensusdata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class AgeThresholds {
	private final List<Integer> thresholds = new ArrayList<Integer>();
	private boolean isDefault = true;
	private boolean isLocked = false;

	public AgeThresholds() {
		//thresholds.add( 0 );
		thresholds.add( 7 );
		thresholds.add( 15 );
		thresholds.add( 18 );
		thresholds.add( 66 );
	}

	public void clear() {
		if (isLocked) throw new IllegalStateException( "age thresholds are not modifiable anymore" );
		thresholds.clear();
		// thresholds.add( 0 );
	}

	public void addThreshold( final String value ) {
		if (isLocked) throw new IllegalStateException( "age thresholds are not modifiable anymore" );
		if (isDefault) clear();

		isDefault = false;
		thresholds.add( Integer.parseInt( value ) );
		Collections.sort( thresholds );
	}

	public int getCategoryIndex( final int age ) {
		// starting to acces values locks the possibility to modify them
		isLocked = true;
		int i = 0;

		for (int threshold : thresholds) {
			if (age < threshold) {
				return i;
			}
			i++;
		}

		return i;
	}

	public String printCategory(final int index ) {
		if (index < 0 || index > thresholds.size()) {
			throw new IllegalArgumentException( "trying to print unexisting category "+index );
		}

		String low = ""+((index == 0) ? 0 : thresholds.get( index - 1 ));
		String up = ""+((index == thresholds.size()) ? "+Infinity" : thresholds.get( index ));

		return "["+low+" , "+up+"[";
	}

	@Override
	public String toString() {
		return "[ageThresholds: "+thresholds.toString()+"]";
	}
}