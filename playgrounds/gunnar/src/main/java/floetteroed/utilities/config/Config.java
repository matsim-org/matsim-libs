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
package floetteroed.utilities.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class Config {

	// -------------------- MEMBERS --------------------

	private final String element;

	private final LinkedList<String> values = new LinkedList<String>();

	private Map<String, Config> element2child = new LinkedHashMap<String, Config>();

	private final ConfigPaths configPaths;

	// -------------------- CONSTRUCTION --------------------

	public Config(final String element, final String configPath) {
		if (element == null) {
			throw new IllegalArgumentException("element is null");
		} else if ("".equals(element)) {
			throw new IllegalArgumentException("element is an empty string");
		}
		this.element = element;
		if (configPath == null) {
			this.configPaths = null;
		} else {
			this.configPaths = new ConfigPaths(configPath);
		}
	}

	// -------------------- INTERNALS --------------------

	private List<String> key(final String... keysAndValue) {
		final List<String> result = new ArrayList<String>(
				keysAndValue.length - 1);
		for (int i = 0; i < keysAndValue.length - 1; i++) {
			result.add(keysAndValue[i]);
		}
		return result;
	}

	private String value(final String... keysAndValue) {
		return keysAndValue[keysAndValue.length - 1];
	}

	// -------------------- SETTERS --------------------

	public void clear() {
		this.element2child.clear();
		this.values.clear();
	}

	public boolean add(final List<String> key, final String value) {
		if (key.size() > 0 && this.element.equals(key.get(0))) {
			if (key.size() == 1) {
				this.values.addLast(value);
				return true;
			} else {
				final List<String> subKey = key.subList(1, key.size());
				Config child = this.element2child.get(subKey.get(0));
				if (child == null) {
					child = new Config(subKey.get(0), this.getConfigPath());
					this.element2child.put(subKey.get(0), child);
				}
				return child.add(subKey, value);
			}
		} else {
			return false;
		}
	}

	public void add(final String... keysAndValue) {
		this.add(this.key(keysAndValue), this.value(keysAndValue));
	}

	// -------------------- GETTERS --------------------

	public String getConfigPath() {
		if (this.configPaths == null) {
			return null;
		} else {
			return this.configPaths.getAbsolutePathToConfig();
		}
	}

	public String absolutePath(final String fileName) {
		if (this.configPaths == null) {
			return fileName;
		} else {
			return this.configPaths.getAbsoluteFileName(fileName);
		}
	}

	public int size() {
		int result = this.values.size();
		for (Config child : this.element2child.values()) {
			result += child.size();
		}
		return result;
	}

	public List<String> getList(final List<String> keys) {
		if (keys.size() > 0 && this.element.equals(keys.get(0))) {
			if (keys.size() == 1) {
				return this.values;
			} else {
				final Config child = this.element2child.get(keys.get(1));
				if (child != null) {
					return child.getList(keys.subList(1, keys.size()));
				}
			}
		}
		// TODO NEW!
		return new ArrayList<String>(0);
		// return null;
	}

	public List<String> getList(final String... keys) {
		return this.getList(Arrays.asList(keys));
	}

	public boolean containsKeys(final String... keys) {
		return (this.getList(keys) != null);
	}

	public String get(final int index, final List<String> keys) {
		final List<String> list = this.getList(keys);
		if (list != null) {
			return list.get(index);
		} else {
			return null;
		}
	}

	public String get(final int index, final String... keys) {
		return this.get(index, Arrays.asList(keys));
	}

	public String get(final List<String> keys) {
		return this.get(0, keys);
	}

	public String get(final String... keys) {
		return this.get(0, keys);
	}

	public Config newSubConfig(final List<String> keys) {
		if (keys.size() > 0 && this.element.equals(keys.get(0))) {
			if (keys.size() == 1) {
				return this;
			} else {
				final Config child = this.element2child.get(keys.get(1));
				if (child != null) {
					return child.newSubConfig(keys.subList(1, keys.size()));
				}
			}
		}
		return null;
	}

	public Config newSubConfig(final String... parentKeys) {
		return this.newSubConfig(Arrays.asList(parentKeys));
	}

	public List<String> toXML(final String prefix, final String indent) {
		final ArrayList<String> result = new ArrayList<String>();
		if (this.values.size() == 0) {
			result.add(prefix + "<" + this.element + ">");
			for (Config child : this.element2child.values()) {
				result.addAll(child.toXML(prefix + indent, indent));
			}
			result.add(prefix + "</" + this.element + ">");
		} else {
			for (String value : this.values) {
				result.add(prefix + "<" + this.element + " value=\"" + value
						+ "\">");
				for (Config child : this.element2child.values()) {
					result.addAll(child.toXML(prefix + indent, indent));
				}
				result.add(prefix + "</" + this.element + ">");
			}
		}
		return result;
	}

	public List<String> toXML() {
		return this.toXML("", "");
	}
}
