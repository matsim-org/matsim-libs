package org.matsim.codeexamples.extensions.matsimApplication;

import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import picocli.CommandLine;

import java.nio.file.Path;

@CommandLine.Command(name="example")
class RunMATSimAppCommandExample implements MATSimAppCommand {

	@CommandLine.Option( names="--input", required = true )
	private Path input;

	@CommandLine.Option( names="--input2", description = "input2", required=false, defaultValue = "input2default")
	private String input2;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	@Override public Integer call() throws Exception{
		System.out.println( input.toString() );
		System.out.println( input2.toString() );
		return null;
	}

	public static void main( String[] args ){
		new RunMATSimAppCommandExample().execute( args );
	}

}
