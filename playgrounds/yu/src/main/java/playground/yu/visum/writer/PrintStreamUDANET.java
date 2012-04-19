/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.yu.visum.writer;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import playground.yu.visum.filter.finalFilters.FinalEventFilterA;

/**
 * test of PrintStream of the attributs definited by user of Visum. This class
 * offers the function to print the important parameters of every attribut
 * defined by VISUM-user in a .net-file of VISUM.
 * 
 * @author ychen
 * 
 */
public class PrintStreamUDANET extends PrintStreamVisum9_3A {
	/*---------------------------CONSTRUCTOR-----------------*/
	public PrintStreamUDANET(final String fileName) {
		if (fileName.endsWith(".net")) {
			try {
				out = new DataOutputStream(new BufferedOutputStream(
						new FileOutputStream(new File(fileName))));

				out.writeBytes("$VISION\n* Technische Universitaet Berlin\n* "
						+ new SimpleDateFormat("dd.MM.yy", Locale.GERMANY)
								.format(new Date())
						+ "\n"
						+ "*\n"
						+ "*\n"
						+ "* Tabelle: Versionsblock\n"
						+ "$VERSION:VERSNR;FILETYPE;LANGUAGE;UNIT\n"
						+ "3.000;Net;DEU;KM\n"
						+ "\n"
						+ "*\n"
						+ "*\n"
						+ "* Tabelle: Benutzerdefinierte Attribute\n"
						+ "$USERATTDEF:OBJID;ATTID;CODE;NAME;DATENTYP;NACHKOMMASTELLEN;"
						+ "DUENN" + "\n");

			} catch (IOException e) {
				System.err.println(e);
			}
		} else {
			System.err.println("please write the fileName with \".net\"");
			throw new IllegalArgumentException(
					"please write the fileName with \".net\"");
		}
	}

	/**
	 * Prints the part of the table "* Tabelle: Benutzerdefinierte Attribute"
	 * under the tablehead in a .net-file of VISUM. e.g.
	 * "LINK;ATTTEST;AttTest;AttTest;Timepoint;0;0;1;0.000;86399.000;0.000;;;0;0;0;4;1"
	 * 
	 * @param uda
	 *            - an attribut defined by VISUM-user, which implements
	 *            interface: org.matsim.playground.filters.writer.UserDefAttI
	 *            and whose fundamental contents will be printed
	 */
	public void printUserDefAtt(final UserDefAtt uda) {
		try {
			out.writeBytes(uda.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*-----------------------------IMPLEMENTING METHOD------------------------------*/
	@Override
	public void output(final FinalEventFilterA fef) {
		for (UserDefAtt uda : fef.UDAexport()) {
			printUserDefAtt(uda);
		}
	}
}