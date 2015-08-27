/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.networks.construction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class AttributeContainer {

	// -------------------- MEMBERS --------------------

	private final String id;

	private final int hashCode;

	// makes one value available through several keys
	// private Map<String, String> key2key = new LinkedHashMap<String,
	// String>();

	private Map<String, String> key2attr = new LinkedHashMap<String, String>();

	// TODO NEW
	protected void setAttributes(final Map<String, String> attributes) {
		// this.key2key.clear();
		this.key2attr = attributes;
	}

	// -------------------- CONSTRUCTION --------------------

	public AttributeContainer(final String id) {
		if (id == null) {
			throw new IllegalArgumentException("id is null");
		}
		this.id = id;
		this.hashCode = id.hashCode();
	}

	// AttributeContainer(final AttributeContainer parent) {
	// this.id = parent.id;
	// this.hashCode = parent.hashCode;
	// this.key2key.putAll(parent.key2key);
	// this.key2attr.putAll(parent.key2attr);
	// }

	// -------------------- INTERNALS --------------------

	private String finalKey(String key) {
		// while (this.key2key.containsKey(key)) {
		// key = this.key2key.get(key);
		// }
		return key;
	}

	// -------------------- CONTENT WRITING --------------------

	// TODO NEW >>>>>
	public void setAttrs(final Attributes attrs, final String... exceptionKeys) {
		final List<String> exceptionKeyList;
		if (exceptionKeys == null) {
			exceptionKeyList = new ArrayList<String>(0);
		} else {
			exceptionKeyList = Arrays.asList(exceptionKeys);
		}
		for (int i = 0; i < attrs.getLength(); i++) {
			final String key = attrs.getQName(i);
			if (!exceptionKeyList.contains(key)) {
				this.setAttr(key, attrs.getValue(i));
				// System.out.println("SETTING " + "key" + " -> " +
				// attrs.getValue(i));
			}
		}
		// System.out.println();
	}

	// TODO NEW <<<<<

	public void setAttr(final String key, final String value) {
		this.key2attr.put(finalKey(key), value);
	}

	// public void redirectKey(final String newKey, final String existingKey) {
	// this.key2key.put(newKey, existingKey);
	// }

	public Map<String, String> getKey2AttributeView() {
		return Collections.unmodifiableMap(this.key2attr);
	}

	// -------------------- CONTENT READING --------------------

	public String getId() {
		return this.id;
	}

	public String getAttr(final String key) {
		return this.key2attr.get(finalKey(key));
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public boolean equals(final Object o) {
		try {
			// TODO 2nd condition is new
			if (o == null || !o.getClass().equals(this.getClass())) {
				return false;
			}
			final AttributeContainer other = (AttributeContainer) o;
			return this.id.equals(other.id);
		} catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append(this.getClass().getSimpleName());
		result.append("(id = ");
		result.append(this.id);
		result.append(", attributes = ");
		result.append(this.key2attr.toString());
		result.append(")");
		return result.toString();
	}
}
