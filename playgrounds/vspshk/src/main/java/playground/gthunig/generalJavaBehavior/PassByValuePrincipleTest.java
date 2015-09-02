package playground.gthunig.generalJavaBehavior;

import org.junit.Test;

import junit.framework.Assert;

public class PassByValuePrincipleTest {

	@Test
	public void testPassByValue() {
		boolean bool = true;
//		bool = getChangedValue(bool);
		changeValue(bool);
		Assert.assertEquals(true, bool);
//		TODO mit nichtprimitivem Datentyp testen
	}
	
	private boolean getChangedValue(boolean bool) {
		bool = false;
		return bool;
	}

	private void changeValue(boolean bool) {
		bool = false;
	}
	
}
