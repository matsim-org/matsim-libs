package lcrociani.TestPedCA;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;
import org.matsim.testcases.MatsimTestCase;

import pedCA.agents.Agent;
import pedCA.context.Context;
import pedCA.environment.grid.EnvironmentGrid;
import pedCA.environment.grid.FloorFieldsGrid;
import pedCA.environment.grid.GridCell;
import pedCA.environment.grid.GridPoint;
import pedCA.environment.grid.PedestrianGrid;
import pedCA.environment.markers.Destination;
import pedCA.environment.markers.MarkerConfiguration;
import pedCA.output.Log;
import scenarios.EnvironmentGenerator;

public class EnvironmentTests extends MatsimTestCase {
	
	@Test
	public void testGridCell(){
		GridCell<String> gc = new GridCell<String>();
		gc.add("Hello");
		gc.add(" ");
		gc.add(" ");
		gc.add("World");
		
		gc.remove(" ");
		 
		assertEquals("Hello World",gc.toString());
	}
	
	@Test
	public void testPedestrianGrid(){
		PedestrianGrid grid = new PedestrianGrid(4,5, null);
		PedestrianGrid result = new PedestrianGrid(4,5, null);

		Agent ped1 = new Agent(0, new GridPoint(1,0), null, null);
		Agent ped2 = new Agent(0, new GridPoint(1,2), null, null);
			
		result.addPedestrian(new GridPoint(1, 0), ped1);
		result.addPedestrian(new GridPoint(1, 2), ped2);
		result.moveTo(ped1, new GridPoint(2,0));
		
		grid.addPedestrian(new GridPoint(1, 0), ped1);
		grid.addPedestrian(new GridPoint(1, 2), ped2);
		
		grid.moveTo(ped1, new GridPoint(2,0));
		//grid.moveTo(ped2, new GridPoint(2,0));
		
		assertEquals(grid.toString(),result.toString());
	}
	
	@Test
	public void testFloorFieldsGrid(){
		String path = "c:/tmp/pedCATest/basic";
		ArrayList <GridPoint> destinationCells = new ArrayList <GridPoint>();
		int shift = 10;
		
		for(int i=0;i<1;i++){
			destinationCells.add(new GridPoint(i+shift,i+shift));
		}
		
		Destination destination = new Destination(destinationCells);
		
		FloorFieldsGrid ff = new FloorFieldsGrid(20, 20);
		ff.generateField(destination);
		
		destinationCells.add(new GridPoint(0,0));
		ff.generateField(destination);
				
		try {
			ff.saveCSV(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	@Test
	public void testCorridor(){
		int rows = 20;
		int cols = 10;
		String path = "c:/tmp/pedCATest/corridor";
		EnvironmentGrid environment = new EnvironmentGrid(rows, cols);
		EnvironmentGenerator.initCorridorWithWalls(environment);
		Destination eastDestination = EnvironmentGenerator.getCorridorEastDestination(environment);
		Destination westDestination = EnvironmentGenerator.getCorridorWestDestination(environment);
		FloorFieldsGrid ff = new FloorFieldsGrid(environment);
		ff.generateField(eastDestination);
		ff.generateField(westDestination);
		try {
			environment.saveCSV(path);
			ff.saveCSV(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testCorridorWithContext(){
		int rows = 8;
		int cols = 10;
		String path = "c:/tmp/pedCATest/corridor";
		EnvironmentGrid environment = new EnvironmentGrid(rows, cols);
		EnvironmentGenerator.initCorridorWithWalls(environment);
		MarkerConfiguration markerConf = new MarkerConfiguration();
		markerConf.addDestination(EnvironmentGenerator.getCorridorEastDestination(environment));
		markerConf.addDestination(EnvironmentGenerator.getCorridorWestDestination(environment));
		//markerConf.addDestination(new TacticalDestination(new Coordinates(0,2),markerConf.getDestinations().get(0).getCells()));
		markerConf.addStart(EnvironmentGenerator.getCorridorEastStart(environment));
		markerConf.addStart(EnvironmentGenerator.getCorridorWestStart(environment));
		Context context = new Context(environment,markerConf);
		try {
			context.saveConfiguration(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Context context2 = null;
		try {
			context2 = new Context(path);
			context2.saveConfiguration("c:/tmp/pedCATest2/corridor");
		} catch (IOException e1) {
			Log.error("Path not found!");
			return;
		}
		
		//CHECK MARKER CONFIGURATION
		for(int i=0;i<markerConf.getDestinations().size();i++)
			for(int j=0;j<markerConf.getDestinations().get(i).getCells().size();j++)
				assertEquals(context.getMarkerConfiguration().getDestinations().get(i).get(j).toString(),context2.getMarkerConfiguration().getDestinations().get(i).get(j).toString());
		for(int i=0;i<markerConf.getStarts().size();i++)
			for(int j=0;j<markerConf.getStarts().get(i).getCells().size();j++)
				assertEquals(context.getMarkerConfiguration().getStarts().get(i).get(j).toString(),context2.getMarkerConfiguration().getStarts().get(i).get(j).toString());

		//CHECK ENVIRONMENT GRID
		for(int i=0;i<context.getEnvironmentGrid().getRows();i++)
			for(int j=0;j<context.getEnvironmentGrid().getColumns();j++)
				assertEquals(context.getEnvironmentGrid().getCellValue(i, j),context2.getEnvironmentGrid().getCellValue(i, j));

		//CHECK PEDESTRIAN GRID
		for(int i=0;i<context.getEnvironmentGrid().getRows();i++)
			for(int j=0;j<context.getEnvironmentGrid().getColumns();j++)
				assertEquals(context.getPedestrianGrid().get(i, j).toString(),context2.getPedestrianGrid().get(i, j).toString());

		//CHECK FLOOR FIELDS
		for(int i=0;i<context.getEnvironmentGrid().getRows();i++)
			for(int j=0;j<context.getEnvironmentGrid().getColumns();j++)
				for (int k=0; k<markerConf.getDestinations().size();k++)
					assertEquals(context.getFloorFieldsGrid().getCellValue(k, new GridPoint(j, i)),context2.getFloorFieldsGrid().getCellValue(k, new GridPoint(j, i)));
	}
	
	@Test
	public void testNetwork(){
		String path = "c:/tmp/pedCATest/corridor";
		Context context= null;
		try {
			context = new Context(path);
			System.out.println(context.getNetwork().toString());
		} catch (IOException e) {
			Log.error("Path not found!");
			return;
		}
	}
		
}
