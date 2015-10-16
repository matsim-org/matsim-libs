package gunnar.ihop2.transmodeler.networktransformation;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
abstract class TransmodelerElement {

	private final String id;

	TransmodelerElement(final String id) {
		this.id = id;
	}

	String getId() {
		return this.id;
	}
}
