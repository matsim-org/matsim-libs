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

	}

	public static class MyDashboard implements Dashboard{

		@Override public void configure( Header header, Layout layout ){
			header.title="mytitle";
			header.description="description";

			layout.row( "myrow" ).el( PieChart.class, ( viz, data ) -> {

				viz.title = "visTitle";
				viz.dataset = "file.csv";

			} );
			VizElement<PieChart> abc = new VizElement<PieChart>(){
				@Override public void configure( PieChart viz, Data data ){
					throw new RuntimeException( "not implemented" );
				}
			};
			final Layout.Row myrow2 = layout.row( "myrow2" ).el( PieChart.class, abc );
		}
	}


}
