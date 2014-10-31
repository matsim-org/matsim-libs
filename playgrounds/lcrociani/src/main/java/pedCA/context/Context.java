package pedCA.context;

import java.io.IOException;

import pedCA.agents.Population;
import pedCA.environment.grid.EnvironmentGrid;
import pedCA.environment.grid.FloorFieldsGrid;
import pedCA.environment.grid.PedestrianGrid;
import pedCA.environment.markers.MarkerConfiguration;
import pedCA.environment.network.CANetwork;

public class Context {
	private PedestrianGrid pedestrianGrid;
	private EnvironmentGrid environmentGrid;
	private FloorFieldsGrid floorFieldsGrid;
	private MarkerConfiguration markerConfiguration;
	private Population population;
	private CANetwork network;

	public Context(EnvironmentGrid environmentGrid, MarkerConfiguration markerConfiguration){
		initializeGrids(environmentGrid, markerConfiguration);
		population = new Population();
		network = new CANetwork(markerConfiguration);
	}
	
	public Context(String path){
		this(new EnvironmentGrid(path),new MarkerConfiguration(path));
	}

	private void initializeGrids(EnvironmentGrid environmentGrid, MarkerConfiguration markerConfiguration) {
		this.environmentGrid = environmentGrid;
		this.markerConfiguration = markerConfiguration;
		floorFieldsGrid = new FloorFieldsGrid(environmentGrid, markerConfiguration);
		pedestrianGrid = new PedestrianGrid(environmentGrid.getRows(), environmentGrid.getColumns());
	}
	
	public void saveConfiguration(String path) throws IOException{
		markerConfiguration.saveConfiguration(path);
		environmentGrid.saveCSV(path);
		floorFieldsGrid.saveCSV(path);
    } 
	
	public EnvironmentGrid getEnvironmentGrid() {
		return environmentGrid;
	}

	public FloorFieldsGrid getFloorFieldsGrid() {
		return floorFieldsGrid;
	}

	public PedestrianGrid getPedestrianGrid(){
		return pedestrianGrid;
	}
	
	public Population getPopulation(){
		return population;
	}
	
	public MarkerConfiguration getMarkerConfiguration(){
		return markerConfiguration;
	}
	
	public CANetwork getNetwork(){
		return network;
	}
	
}
