/* *********************************************************************** *
 * project: org.matsim.*
 * RunIt.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.experimental;

import java.io.File;

public class RunIt {

	public static void main(String [] args) {
		//		playground.gregor.boundarycondition.BiPedExperiments.main(new String []{".8"});
		//		playground.gregor.sim2d_v4.run.Sim2DRunner.main(new String [] {"/Users/laemmel/devel/bipedexp/input/s2d_config.xml","/Users/laemmel/devel/bipedexp/input/config.xml","false"});
		//		File f = new File("/Users/laemmel/devel/bipedexp/fnd/fnd");
		//		f.renameTo(new File("/Users/laemmel/devel/bipedexp/fnd/fnd_80_20"));
		//
		//		playground.gregor.boundarycondition.BiPedExperiments.main(new String []{".2"});
		//		playground.gregor.sim2d_v4.run.Sim2DRunner.main(new String [] {"/Users/laemmel/devel/bipedexp/input/s2d_config.xml","/Users/laemmel/devel/bipedexp/input/config.xml","false"});
		//		f = new File("/Users/laemmel/devel/bipedexp/fnd/fnd");
		//		f.renameTo(new File("/Users/laemmel/devel/bipedexp/fnd/fnd_20_80"));
		//		
		//		playground.gregor.boundarycondition.BiPedExperiments.main(new String []{".1"});
		//		playground.gregor.sim2d_v4.run.Sim2DRunner.main(new String [] {"/Users/laemmel/devel/bipedexp/input/s2d_config.xml","/Users/laemmel/devel/bipedexp/input/config.xml","false"});
		//		f = new File("/Users/laemmel/devel/bipedexp/fnd/fnd");
		//		f.renameTo(new File("/Users/laemmel/devel/bipedexp/fnd/fnd_10_90"));
		//		
		//		playground.gregor.boundarycondition.BiPedExperiments.main(new String []{".9"});
		//		playground.gregor.sim2d_v4.run.Sim2DRunner.main(new String [] {"/Users/laemmel/devel/bipedexp/input/s2d_config.xml","/Users/laemmel/devel/bipedexp/input/config.xml","false"});
		//		f = new File("/Users/laemmel/devel/bipedexp/fnd/fnd");
		//		f.renameTo(new File("/Users/laemmel/devel/bipedexp/fnd/fnd_90_10"));
		//		
		//		playground.gregor.boundarycondition.BiPedExperiments.main(new String []{".3"});
		//		playground.gregor.sim2d_v4.run.Sim2DRunner.main(new String [] {"/Users/laemmel/devel/bipedexp/input/s2d_config.xml","/Users/laemmel/devel/bipedexp/input/config.xml","false"});
		//		f = new File("/Users/laemmel/devel/bipedexp/fnd/fnd");
		//		f.renameTo(new File("/Users/laemmel/devel/bipedexp/fnd/fnd_30_70"));
		//		
		//		playground.gregor.boundarycondition.BiPedExperiments.main(new String []{".7"});
		//		playground.gregor.sim2d_v4.run.Sim2DRunner.main(new String [] {"/Users/laemmel/devel/bipedexp/input/s2d_config.xml","/Users/laemmel/devel/bipedexp/input/config.xml","false"});
		//		 f = new File("/Users/laemmel/devel/bipedexp/fnd/fnd");
		//		f.renameTo(new File("/Users/laemmel/devel/bipedexp/fnd/fnd_70_30"));
		//		
		//		playground.gregor.boundarycondition.BiPedExperiments.main(new String []{".5"});
		//		playground.gregor.sim2d_v4.run.Sim2DRunner.main(new String [] {"/Users/laemmel/devel/bipedexp/input/s2d_config.xml","/Users/laemmel/devel/bipedexp/input/config.xml","false"});
		//		f = new File("/Users/laemmel/devel/bipedexp/fnd/fnd");
		//		f.renameTo(new File("/Users/laemmel/devel/bipedexp/fnd/fnd_50_50"));
		//		
		//		playground.gregor.boundarycondition.BiPedExperiments.main(new String []{".6"});
		//		playground.gregor.sim2d_v4.run.Sim2DRunner.main(new String [] {"/Users/laemmel/devel/bipedexp/input/s2d_config.xml","/Users/laemmel/devel/bipedexp/input/config.xml","false"});
		//		f = new File("/Users/laemmel/devel/bipedexp/fnd/fnd");
		//		f.renameTo(new File("/Users/laemmel/devel/bipedexp/fnd/fnd_60_40"));
		//		
		//		playground.gregor.boundarycondition.BiPedExperiments.main(new String []{".4"});
		//		playground.gregor.sim2d_v4.run.Sim2DRunner.main(new String [] {"/Users/laemmel/devel/bipedexp/input/s2d_config.xml","/Users/laemmel/devel/bipedexp/input/config.xml","false"});
		//		f = new File("/Users/laemmel/devel/bipedexp/fnd/fnd");
		//		f.renameTo(new File("/Users/laemmel/devel/bipedexp/fnd/fnd_40_60"));

		double [] splits  = new double[]{0.25,0.3,0.35, 0.65,0.7,0.75};
		for (double split : splits) {
			for (int i = 4; i < 5; i++) {
				int s = (int) (split*100);
				playground.gregor.boundarycondition.BiPedExperiments.main(new String []{split+""});
				playground.gregor.sim2d_v4.run.Sim2DRunner.main(new String [] {"/Users/laemmel/devel/bipedexp/input/s2d_config.xml","/Users/laemmel/devel/bipedexp/input/config.xml","false"});
				File f = new File("/Users/laemmel/devel/bipedexp/fnd/fnd");
				f.renameTo(new File("/Users/laemmel/devel/bipedexp/fnd/fnd-"+s+"-"+(100-s)+"-"+i));
			}
		}
	}
}
