/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
package org.matsim.codeexamples.extensions.minibus;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.minibus.RunMinibus;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.testcases.MatsimTestUtils;

public class RunMinibusTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    /**
     * Test method for {@link RunMinibus#main(java.lang.String[])}.
     */
    @SuppressWarnings("static-method")
    @Test
    public final void testMain() {
        RunMinibus runner = new RunMinibus( new String[]{"https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/atlantis/minibus/config.xml"} );

        Config config = runner.getConfig();

        config.controler().setOutputDirectory( utils.getOutputDirectory() );
        config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

        config.controler().setLastIteration( 1 );

        runner.run();
    }
}
