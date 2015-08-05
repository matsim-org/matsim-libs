package playground.gthunig.generalJavaBehavior;

import org.junit.Test;

import junit.framework.Assert;

public class PassByValuePrincipleTest {

	@Test
	public void testPassByValue() {
		boolean bool = true;
		changeValue(bool);
		Assert.assertEquals(true, bool);
	}
	
	private void changeValue(boolean bool) {
		bool = !bool;
	}
	
}
