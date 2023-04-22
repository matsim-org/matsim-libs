package org.matsim.core.router;

public class RouterUtils{
	private RouterUtils(){} // do not instantiate

	public static MultimodalLinkChooser getMultimodalLinkChooserDefault() {
		return new MultimodalLinkChooserDefaultImpl();
	}
}
