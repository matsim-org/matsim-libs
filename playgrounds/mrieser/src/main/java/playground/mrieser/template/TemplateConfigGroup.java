/* *********************************************************************** *
 * project: org.matsim.*
 * PluginConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.mrieser.template;

import java.util.LinkedHashMap;
import java.util.Map;

public class TemplateConfigGroup extends org.matsim.core.config.Module {

	private static final long serialVersionUID = 1L;

	/*package*/ static final String GROUP_NAME = "myPlugin";
	private static final String SOMEVALUE_NAME = "someValue";

	private int someValue = 0;

	public TemplateConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public void addParam(final String paramName, final String value) {
		if (SOMEVALUE_NAME.equals(paramName)) {
			setSomeValue(Integer.parseInt(value));
		}
		throw new IllegalArgumentException(paramName);
	}

	@Override
	public String getValue(final String paramName) {
		if (SOMEVALUE_NAME.equals(paramName)) {
			return Integer.toString(getSomeValue());
		}
		throw new IllegalArgumentException(paramName);
	}

	@Override
	public Map<String, String> getParams() {
		Map<String, String> map = new LinkedHashMap<String, String>(5);
		map.put(SOMEVALUE_NAME, getValue(SOMEVALUE_NAME));
		return map;
	}

	public void setSomeValue(final int someValue) {
		this.someValue = someValue;
	}

	public int getSomeValue() {
		return this.someValue;
	}

}
