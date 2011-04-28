package playground.fhuelsmann.emission.objects;

/** 
 * 
 * @author mobile.TUM
 * Hot value class has the array value, which contains the KM, Velocity , EFA etc..
 * in order to read the data, you should use getValue()[index] the index is the column index.
 * for example .getValue()[3] returns the value of the velocity.
 * Hotvalue is used in the Hashmap in the class HefaHot
 *
 */
public class HotValue {

	private String[] value;
	
	public HotValue(String[] vlaue){
		super();
		setValue(value);
	}

	public String[] getValue() {
		return value;
	}

	public void setValue(String[] value) {
		this.value = value;
	}
	
	
}
