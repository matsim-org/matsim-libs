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
package floetteroed.utilities.commandlineparser;

import static java.util.Collections.unmodifiableSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class CommandLineParser {

	// -------------------- CONSTANTS --------------------

	public static final String KEY_PREFIX = "-";

	// -------------------- MEMBERS --------------------

	private final Map<String, String> parameters = new HashMap<String, String>();

	private final Map<String, CommandLineParserElement> elements = new TreeMap<String, CommandLineParserElement>();

	private final Set<CommandLineParserElement> missingElements = new TreeSet<CommandLineParserElement>();

	private boolean isDone = false;

	// -------------------- CONSTRUCTION & SETUP --------------------

	public CommandLineParser() {
	}

	public boolean defineParameter(final String key, final boolean required,
			final String defaultValue, final String explanation) {
		if (this.isDone) {
			return false;
		} else {
			this.elements.put(key.toUpperCase(), new CommandLineParserElement(
					key, required, defaultValue, explanation));
			return true;
		}
	}

	public boolean parse(final String[] args) {
		if (this.isDone) {
			return false;
		} else {
			if (args != null) {
				for (int i = 0; i < args.length; i += 2) {
					this.put(args[i], args[i + 1]);
				}
			}
			final Map<String, String> defaults = new HashMap<String, String>();
			for (CommandLineParserElement element : this.elements.values()) {
				if (!this.containsKey(element.getKey())) {
					if (element.getRequired()) {
						this.missingElements.add(element);
					}
					if (element.getDefaultValue() != null) {
						defaults.put(element.getKey(), element
								.getDefaultValue());
					}
				}
			}
			this.parameters.putAll(defaults);
			this.isDone = true;
			return true;
		}
	}

	// -------------------- BASIC INTERNAL OPERATIONS --------------------

	// private boolean isKey(final String s) {
	// return (s != null && KEY_PREFIX.equals(s.substring(0, 1)));
	// }

	private void put(final String key, final String value) {
		this.parameters.put(key.toUpperCase(), value);
	}

	private String get(final String key) {
		return this.parameters.get(key.toUpperCase());
	}

	public boolean containsKey(final String key) {
		return this.parameters.containsKey(key.toUpperCase());
	}

	// -------------------- CONTENT ACCESS --------------------

	public int size() {
		return this.parameters.size();
	}

	public boolean isComplete() {
		if (this.isDone) {
			return (this.missingElements.size() == 0);
		} else {
			return false;
		}
	}

	public Set<CommandLineParserElement> getMissingElements() {
		return unmodifiableSet(this.missingElements);
	}

	public Set<CommandLineParserElement> getElements() {
		return unmodifiableSet(new TreeSet<CommandLineParserElement>(
				this.elements.values()));
	}

	public boolean containsKeys(final String... keys) {
		if (keys != null) {
			for (String key : keys) {
				if (!containsKey(key)) {
					return false;
				}
			}
		}
		return true;
	}

	public String getString(final String key) {
		return this.get(key);
	}

	public int getInteger(final String key) {
		return Integer.parseInt(this.get(key));
	}

	public long getLong(final String key) {
		return Long.parseLong(this.get(key));
	}

	public double getDouble(final String key) {
		return Double.parseDouble(this.get(key));
	}

	public boolean getBoolean(final String key) {
		return Boolean.parseBoolean(this.get(key));
	}

	@Override
	public String toString() {
		return this.parameters.toString();
	}
}
