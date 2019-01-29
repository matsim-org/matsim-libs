/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.parking.parkingchoice.lib.utils.expr;

/**
 *  A lexical token from an input string.
 *
 */

class Token {
	public static final int TT_ERROR = -1;
	public static final int TT_EOF = -2;
	public static final int TT_NUMBER = -3;
	public static final int TT_WORD = -4;
	public static final int TT_LE = -5;
	public static final int TT_NE = -6;
	public static final int TT_GE = -7;

	public Token(int ttype, double nval, String input, int start, int end) {
		this.ttype = ttype;
		this.sval = input.substring(start, end);
		this.nval = nval;
		this.location = start;

		int count = 0;
		for (int i = start - 1; 0 <= i; --i) {
			if (!Character.isWhitespace(input.charAt(i)))
				break;
			++count;
		}
		this.leadingWhitespace = count;

		count = 0;
		for (int i = end; i < input.length(); ++i) {
			if (!Character.isWhitespace(input.charAt(i)))
				break;
			++count;
		}
		this.trailingWhitespace = count;
	}

	Token(int ttype, double nval, String sval, Token token) {
		this.ttype = ttype;
		this.sval = sval;
		this.nval = nval;
		this.location = token.location;
		this.leadingWhitespace = token.leadingWhitespace;
		this.trailingWhitespace = token.trailingWhitespace;
	}

	public final int ttype;
	public final String sval;
	public final double nval;

	public final int location;

	public final int leadingWhitespace, trailingWhitespace;
}
