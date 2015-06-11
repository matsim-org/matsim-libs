/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.gregor.rtcadyts.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.misc.StringUtils;

public class SensorDataReader {
	


	public static SensorDataFrame handle(File f) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(f));
		String l = br.readLine();
		l = br.readLine();//sloppy header skipping
		if (l == null) {
			br.close();
			return null;
		}
		String[] expl = StringUtils.explode(l, ' ');
		double time = Double.parseDouble(expl[1]);
		SensorDataFrame frame = new SensorDataFrame(time);
		while (l != null){
			expl = StringUtils.explode(l, ' ');
			double t = Double.parseDouble(expl[1]);
			double x = Double.parseDouble(expl[2]);
			double y = Double.parseDouble(expl[3]);
			double v = Double.parseDouble(expl[4]);
			double angle = Double.parseDouble(expl[5]);
			frame.addVehicle(t, x, y, v, angle);
			l = br.readLine();
			
		}
		br.close();
		return frame;
		
	}

}
