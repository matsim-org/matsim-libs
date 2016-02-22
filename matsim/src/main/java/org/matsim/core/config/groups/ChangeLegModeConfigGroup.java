package org.matsim.core.config.groups;


import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.misc.StringUtils;

public class ChangeLegModeConfigGroup extends ReflectiveConfigGroup {

	public final static String CONFIG_MODULE = "changeLegMode";
	public final static String CONFIG_PARAM_MODES = "modes";
	public final static String CONFIG_PARAM_IGNORECARAVAILABILITY = "ignoreCarAvailability";

	private String[] modes = new String[] { TransportMode.car, TransportMode.pt };
	private boolean ignoreCarAvailability = true;

	public ChangeLegModeConfigGroup() {
		super(CONFIG_MODULE);
	}

	public String[] getModes() {
		return modes;
	}

	@StringGetter( CONFIG_PARAM_MODES )
	private String getModesString() {
		return toString( modes );
	}

	@StringSetter( CONFIG_PARAM_MODES )
	private void setModes( final String value ) {
		setModes( toArray( value ) );
	}

	public void setModes( final String[] modes ) {
		this.modes = modes;
	}

	@StringSetter( CONFIG_PARAM_IGNORECARAVAILABILITY )
	public void setIgnoreCarAvailability(final boolean value) {
		this.ignoreCarAvailability = value;
	}

	@StringGetter( CONFIG_PARAM_IGNORECARAVAILABILITY )
	public boolean getIgnoreCarAvailability() {
		return ignoreCarAvailability;
	}

	private static String toString( final String[] modes ) {
		// (not same as toString() because of argument!)

		StringBuilder b = new StringBuilder();

		if (modes.length > 0) b.append( modes[ 0 ] );
		for (int i=1; i < modes.length; i++) {
			b.append( ',' );
			b.append( modes[ i ] );
		}

		return b.toString();
	}

	private static String[] toArray( final String modes ) {
		String[] parts = StringUtils.explode(modes, ',');

		for (int i = 0, n = parts.length; i < n; i++) {
			parts[i] = parts[i].trim().intern();
		}

		return parts;
	}


}
