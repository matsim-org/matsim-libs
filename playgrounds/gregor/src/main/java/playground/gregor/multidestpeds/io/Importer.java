/* *********************************************************************** *
 * project: org.matsim.*
 * Importer.java
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
package playground.gregor.multidestpeds.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImplTest;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;


import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLDouble;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author laemmel
 * 
 */
public class Importer {


	private static final Scenario SC = ScenarioUtils.createScenario(ConfigUtils.createConfig());


	private static final File file = new File("/Users/laemmel/devel/dfg/data/gr90.mat");

	private final List<Ped> peds = new ArrayList<Ped>();

	private final int count = 0;

	private final List<Double> timeSteps = new ArrayList<Double>();

	public void read() throws IOException {
		MatFileReader reader = new MatFileReader();
		Map<String, MLArray> map = reader.read(file);
		MLDouble redX = (MLDouble) map.get("xr");
		MLDouble redY = (MLDouble) map.get("yr");
		MLDouble redVX = (MLDouble) map.get("vxr");
		MLDouble redVY = (MLDouble) map.get("vyr");

		MLDouble greenX = (MLDouble) map.get("xg");
		MLDouble greenY = (MLDouble) map.get("yg");
		MLDouble greenVX = (MLDouble) map.get("vxg");
		MLDouble greenVY = (MLDouble) map.get("vyg");

		MLDouble timeStamps = (MLDouble) map.get("time_stamps");
		extractPeds(timeStamps,redX,redY,redVX,redVY,"r");
		extractPeds(timeStamps,greenX,greenY,greenVX,greenVY,"g");

		double[][] times = timeStamps.getArray();
		int numTimeStamps = times[0].length;
		for (int i = 0; i < numTimeStamps; i ++) {
			double time = times[0][i];
			this.timeSteps.add(time);
		}

	}


	private void extractPeds(MLDouble timeStamps, MLDouble aX, MLDouble aY,
			MLDouble aVX, MLDouble aVY, String color) {
		double[][] times = timeStamps.getArray();
		int numTimeStamps = times[0].length;
		double[][] x = aX.getArray();
		int numPeds = x[0].length;
		double[][] y = aY.getArray();
		double[][] vx = aVX.getArray();
		double[][] vy = aVY.getArray();

		for (int i = 0; i < numPeds; i++) {
			Id id = SC.createId(color + i);
			Ped p = new Ped();
			p.id = id;
			this.peds.add(p);
			boolean departed = false;
			for (int j = 0; j < numTimeStamps; j++) {
				double time = times[0][j];
				double xPos = x[j][i];
				double yPos = y[j][i];
				double currVX = vx[j][i];
				double currVY = vy[j][i];
				if (departed && Double.isNaN(xPos)) {
					break;
				}
				if (!departed && !Double.isNaN(xPos)) {
					departed = true;
					p.depart = time;
				}

				if (departed) {
					Coordinate c = new Coordinate(xPos,yPos);
					p.coords.put(time, c);
					Coordinate v = new Coordinate(currVX,currVY);
					p.velocities.put(time, v);
					p.arrived = time;
				}

			}
		}
	}


	/**
	 * @return
	 */
	public List<Ped> getPeds() {

		return this.peds;
	}

	/**
	 * @return
	 */
	public List<Double> getTimeSteps() {
		return this.timeSteps;
	}

}
