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

package playground.vsptelematics.bangbang;

import org.matsim.run.gui.Gui;

public class KNBangBangGUI {

	public static void main(String[] args) {
		Gui.show("MATSim: Bang Bang", KNAccidentScenario.class);
	}

	/* To start this class upon double-clicking the jar-file, add the following lines to the pom.xml
	 * and configure the mainClass correctly:
	 * 
	 * 
	 		<build>
				<plugins>
			  	<plugin>
						<groupId>org.apache.maven.plugins</groupId>
						<artifactId>maven-jar-plugin</artifactId>
						<configuration>
							<archive>
								<manifest>
									<mainClass>playground.vsptelematics.bangbang.KNBangBangGUI</mainClass>
								</manifest>
							</archive>
						</configuration>
					</plugin>
				</plugins>
			</build>
	 * 
	 * and then, to create the clickable jar-file:
	 * 
	 * - make sure the dependencies (including MATSim-core) is maven-installed, 
	 *   e.g. do "mvn install -DskipTests=true" for all required dependencies
	 * - change to the directory of this project, e.g. cd /path/to/playground/vsptelematics/
	 * - mvn clean
	 * - mvn -Prelease
	 * 
	 * This will result in a zip file in the target-directory which includes the clickable jar-file.
	 */
}
