/* *********************************************************************** *
 * project: org.matsim.*
 * ArgumentParser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.utils.misc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>This class analyses arguments passed on program start-up and provides an
 * iterator for easy access to the single options and arguments.
 * The parser understands normal arguments (like filenames) and options
 * (arguments beginning with - or --) and supports value-assignments of options
 * (e.g. "-rate 10" or "-rate=10").</p>
 *
 * <p>Additionally, the parser can be instructed to specifically expect short
 * options. In that case, options starting with one hyphen can only be one
 * character long. If more than one character follow a single hyphen, each
 * character will be interpreted as an option. The iterator returns every
 * option on its own in that case.<br>
 *
 * Example: <code>tar -xzvf file</code><br />
 * If shortOptions are enabled, the above argument list would be translated into
 * <code>tar -x -z -v -f file</code>, whereas if shortOptions are disabled, a
 * single option named "xzvf" should exist (like in <code>java -jar
 * file.jar</code>).</p>
 *
 * <p>Value assignments to options can be delimitted by a space or the equal-sign.
 * In both cases, option and value are separated into two entities and returned
 * separately by the iterator.<br />
 *
 * Example: <code>myapp -start 5</code> or <code>myapp -start=5</code> both
 * work.</p>
 *
 *
 * @author mrieser
 */
public class ArgumentParser implements Iterable<String> {

	private List<String> args = null;
	private boolean enableShortOptions = true;

	/**
	 * Constructs a new <code>ArgumentParser</code> with support for shortOptions
	 * disabled.
	 *
	 * @param args The arguments to parse.
	 */
	public ArgumentParser(final String[] args) {
		this(args, false);
	}

	/**
	 * Constructs a new <code>ArgumentParser</code> with the specified behaviour
	 * regarding shortOptions.
	 *
	 * @param args The arguments to parse.
	 * @param enableShortOptions whether options with a single hyphen should be
	 * 		treated as short options (only one character long) or not.
	 */
	public ArgumentParser(final String[] args, final boolean enableShortOptions) {
		this.args = new ArrayList<String>();
		this.enableShortOptions = enableShortOptions;
		parse(args);
	}


	/**
	 * Returns an iterator over all single options and arguments.
	 */
	@Override
	public Iterator<String> iterator() {
		return this.args.iterator();
	}


	private void parse(final String[] args) {
		if (this.enableShortOptions) {
			for (String arg : args) {
				if (arg.startsWith("--")) {
					parseLongOption(arg.substring(2));
				} else if (arg.startsWith("-")) {
					parseShortOption(arg.substring(1));
				} else {
					this.args.add(arg);
				}
			}
		} else {
			for (String arg : args) {
				if (arg.startsWith("-")) {
					parseOption(arg);
				} else {
					this.args.add(arg);
				}
			}
		}
	}

	private void parseShortOption(final String arg) {
		if (arg.length() == 0) {
			this.args.add("-");
			return;
		}
		for (int i = 0; i < arg.length(); i++) {
			char ch = arg.charAt(i);
			if (ch == '=') {
				if ((i == 0) && (arg.length() == 1)) {
					// '=' is the only char
					this.args.add("-=");
					return;
				} else if (i == 0) {
					// arg is a string in the form of '-=*', for me this makes only sense as a string-param, so just add it
					this.args.add('-' + arg);
					return;
				} else {
					this.args.add(arg.substring(i+1));
					return;
				}
			}
			// else...
			this.args.add("-" + ch);
		}
	}

	private void parseLongOption(final String arg) {
		StringBuilder argname = new StringBuilder("--");
		for (int i = 0; i < arg.length(); i++) {
			char ch = arg.charAt(i);
			if (ch == '=') {
				if ((i == 0) && (arg.length() == 1)) {
					// '=' is the only char
					this.args.add("--=");
					return;
				} else if (i == 0) {
					// arg is a string in the form of '--=*', interpret it as a string argument
					this.args.add("--" + arg);
					return;
				} else {
					this.args.add(argname.toString());
					this.args.add(arg.substring(i + 1));
					return;
				}
			}
			// else...
			argname.append(ch);
		}
		this.args.add(argname.toString());
	}

	private void parseOption(final String arg) {
		StringBuilder argname = new StringBuilder();
		argname.append('-');  // every option starts with '-'
		for (int i = 1; i < arg.length(); i++) { // options always start with '-', so we can start at char 1
			char ch = arg.charAt(i);
			if (ch == '=') {
				if (argname.toString().equals("-")) {
					// arg is a string in the form of '-=*', interpret it as a strig argument
					this.args.add(arg);
					return;
				}
				if (argname.toString().equals("--")) {
					// arg is a string in the form of '--=*', interpret it as a strig argument
					this.args.add(arg);
					return;
				}
				// it seems there are already some real characters before '=' that make a correct argument
				this.args.add(argname.toString());
				this.args.add(arg.substring(i + 1));
				return;
			}
			// else...
			argname.append(ch);
		}
		this.args.add(argname.toString());
	}

}
