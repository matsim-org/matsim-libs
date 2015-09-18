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

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class CommandLineParserElement implements
		Comparable<CommandLineParserElement> {

	// -------------------- CONSTANTS --------------------

	private final String key;

	private final String defaultValue;

	private final String explanation;

	private final boolean required;

	// -------------------- CONSTRUCTION --------------------

	public CommandLineParserElement(final String key, final boolean required,
			final String defaultValue, final String explanation) {
		if (key == null) {
			throw new IllegalArgumentException("key must not be null");
		}
		this.key = key.toUpperCase();
		this.defaultValue = defaultValue;
		this.explanation = explanation;
		this.required = required;
	}

	// -------------------- CONTENT ACCESS --------------------

	public String getKey() {
		return this.key;
	}

	public String getDefaultValue() {
		return this.defaultValue;
	}

	public String getExplanation() {
		return this.explanation;
	}

	public boolean getRequired() {
		return this.required;
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append(this.key);
		if (this.required) {
			result.append(" : required; ");
		} else {
			result.append(" : optional; ");
		}
		result.append(this.explanation);
		result.append("; ");
		if (this.defaultValue == null) {
			result.append("no default value");
		} else {
			result.append("default = ");
			result.append(this.defaultValue);
		}
		return result.toString();
	}

	// -------------------- IMPLEMENTATION OF Comparable --------------------

	@Override
	public int compareTo(CommandLineParserElement arg0) {
		return this.getKey().compareTo(arg0.getKey());
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		try {
			final CommandLineParserElement otherElement = (CommandLineParserElement) o;
			return this.key.equals(otherElement.key);
		} catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.key.hashCode();
	}
}
