package playground.gthunig.generalJavaBehavior;

import org.junit.Assert;
import org.junit.Test;

public class PassByValuePrincipleTest {

	@Test
	public void testPassByValue() {
		boolean bool = true;
//		bool = getChangedValue(bool);
		changeValue(bool);
		Assert.assertTrue(bool);
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
