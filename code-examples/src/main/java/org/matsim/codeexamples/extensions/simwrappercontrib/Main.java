package org.matsim.codeexamples.extensions.simwrappercontrib;

import org.matsim.simwrapper.*;
import org.matsim.simwrapper.viz.PieChart;

import java.nio.file.Path;

class Main{

	public static void main( String[] args ){

		SimWrapper sw = SimWrapper.create();

		sw.addDashboard( new MyDashboard() );

		sw.run( Path.of("./output" ) );


	}

	private static class MyDashboard implements Dashboard{

		@Override public void configure( Header header, Layout layout ){
			header.title="mytitle";


			VizElement<PieChart> pie = new VizElement<PieChart>(){
				@Override public void configure( PieChart viz, Data data ){

					viz.title = "visTitle";
					viz.dataset = "file.csv";

				}
			};
			layout.row( "myrow" ).el( PieChart.class, pie );
		}
	}


}
