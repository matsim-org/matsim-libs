package org.matsim.codeexamples.extensions.simwrappercontrib;

import org.matsim.simwrapper.*;
import org.matsim.simwrapper.viz.PieChart;

import java.io.IOException;
import java.nio.file.Path;

/**
 * example class that I started writing during CR's class.  Some of it worked, but I cannot remember if the final version worked.  kai, jan'23
 */
class RunSimwrappercontribOfflineExample{

	public static void main( String[] args ) throws IOException{

		SimWrapper sw = SimWrapper.create();

		sw.addDashboard( new MyDashboard() );

//		sw.run( Path.of("./output" ) );

		sw.generate( Path.of("dashboard") );

		// now: open simwrapper in Chrome, allow the "dashboard" local directory, point simwrapper to it.
		// It fails, possibly because "file.csv" is not there.

	}

	public static class MyDashboard implements Dashboard{

		@Override public void configure( Header header, Layout layout ){
			header.title="mytitle";
			header.description="description";

			layout.row( "myrow" ).el( PieChart.class, ( viz, data ) -> {

				viz.title = "visTitle";
				viz.dataset = "file.csv";

			} );
		}
	}


}
