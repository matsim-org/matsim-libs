package playground.wrashid.lib.obj;

/**
 * Sometimes one needs to pass more than one result and creating a new class to
 * fit the objects seems artificial. A quick solution would be to return an
 * array of Objects, but for this one must create them manually. This class
 * performs this task, but keeps the code short.
 * 
 * @author wrashid
 * 
 */
public class GenericResult {

	private Object[] result;

	public GenericResult(Object... objects) {
		result = objects;
	}

	public Object[] getResult() {
		return result;
	}

}
