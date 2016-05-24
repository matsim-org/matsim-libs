package org.matsim.core.config.groups;


import org.matsim.core.config.ReflectiveConfigGroup;

@Deprecated
public class ChangeLegModeConfigGroup extends ReflectiveConfigGroup {

	public final static String CONFIG_MODULE = "changeLegMode";
	public final static String CONFIG_PARAM_MODES = "modes";
	public final static String CONFIG_PARAM_IGNORECARAVAILABILITY = "ignoreCarAvailability";


	private static final String message = "changeLegMode config group does no longer exist; use changeMode instead";

	@Deprecated
	public ChangeLegModeConfigGroup() {
		super(CONFIG_MODULE);
	}

	@StringGetter( CONFIG_PARAM_MODES )
	@Deprecated
	private String getModesString() {
		throw new RuntimeException(message) ;
	}

	@StringSetter( CONFIG_PARAM_MODES )
	@Deprecated
	private void setModes( final String value ) {
		throw new RuntimeException(message) ;
	}

	@Deprecated
	public void setModes( final String[] modes ) {
		throw new RuntimeException(message) ;
	}

	@StringSetter( CONFIG_PARAM_IGNORECARAVAILABILITY )
	@Deprecated
	public void setIgnoreCarAvailability(final boolean value) {
		throw new RuntimeException(message) ;
	}

	@StringGetter( CONFIG_PARAM_IGNORECARAVAILABILITY )
	@Deprecated
	public boolean getIgnoreCarAvailability() {
		throw new RuntimeException(message) ;
	}

	@Deprecated
	private static String toString( final String[] modes ) {
		// (not same as toString() because of argument!)
		throw new RuntimeException(message) ;
	}

	@Deprecated
	private static String[] toArray( final String modes ) {
		throw new RuntimeException(message) ;
	}


}
