package playground.toronto.gtfsutils;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import GTFS2PTSchedule.GTFS2MATSimTransitSchedule;

public class GTFSController {

	public static void main(final String[] args) throws FileNotFoundException, IOException{
			
		//////////////////
		//OPEN CONFIG FILE
		//////////////////
		
		JFileChooser fc;
		int state;
		String GTFSCONFIGNAME = null;
		if (args.length == 1){
			GTFSCONFIGNAME = args[0];
		}else{
			fc = new JFileChooser();
			fc.setDialogTitle("Please select GTFS config file");
			fc.setFileFilter(new FileFilter() {
				@Override
				public String getDescription() {return "GTFS config file *.xml";}
				@Override
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".xml" );
				}
			});		
			state = fc.showOpenDialog(null);
			if (state == JFileChooser.APPROVE_OPTION){
				GTFSCONFIGNAME = fc.getSelectedFile().getAbsolutePath();
			}else if (state == JFileChooser.CANCEL_OPTION) return;
		}

		if (GTFSCONFIGNAME == null) return;
		
		Config config = ConfigUtils.loadConfig(GTFSCONFIGNAME);
		
		String[] rts = config.getParam("gtfs", "roots").split(",");
		File[] roots = new File[rts.length];
		for(int i = 0; i < roots.length; i++) roots[i] = new File(rts[i]);
		
		//(File[] roots, String[] modes, Network network, String[] serviceIds, String outCoordinateSystem)
		GTFS2MATSimTransitSchedule gtfsTTC = new GTFS2MATSimTransitSchedule(roots, 
				config.getParam("gtfs", "modes").split(","), 
				ScenarioUtils.loadScenario(config).getNetwork(), 
				config.getParam("gtfs", "service").split(","), 
				config.getParam("gtfs", "outCoordinateSystem"));
		
		///////////////
		//RUN CONVERTER
		///////////////
		TransitSchedule outputSchedule = gtfsTTC.getTransitSchedule();
		
		/////////////////
		//CREATE VEHICLES
		/////////////////
		
		Vehicles outputVehicles = VehicleUtils.createVehiclesContainer();
		CreateMultipleVehicleTypesForSchedule vehicleConverter = new CreateMultipleVehicleTypesForSchedule(outputSchedule, outputVehicles);
		
		String VEHICLETYPESFILE = null;
		fc = new JFileChooser();
		fc.setDialogTitle("Please select a table of vehicle types");
		fc.setFileFilter(new FileFilter() {
			@Override
			public String getDescription() {return "Table file *.txt";}
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".txt" );
			}
		});		
		state = fc.showOpenDialog(null);
		if (state == JFileChooser.APPROVE_OPTION){
			VEHICLETYPESFILE = fc.getSelectedFile().getAbsolutePath();
		}else if (state == JFileChooser.CANCEL_OPTION) return;
		if (VEHICLETYPESFILE == null) return;
		
		vehicleConverter.ReadVehicleTypes(VEHICLETYPESFILE);
		
		String ROUTEVEHCILEMAPFILE = null;
		fc = new JFileChooser();
		fc.setDialogTitle("Please select a table of route vehicle mapping");
		fc.setFileFilter(new FileFilter() {
			@Override
			public String getDescription() {return "Table file *.txt";}
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".txt" );
			}
		});		
		state = fc.showOpenDialog(null);
		if (state == JFileChooser.APPROVE_OPTION){
			ROUTEVEHCILEMAPFILE = fc.getSelectedFile().getAbsolutePath();
		}else if (state == JFileChooser.CANCEL_OPTION) return;
		if (ROUTEVEHCILEMAPFILE == null) return;
		
		vehicleConverter.ReadRouteVehicleMapping(ROUTEVEHCILEMAPFILE);
		
		vehicleConverter.run();
		
		String VEHICLEOUTPUTFILE = null;
		fc = new JFileChooser();
		fc.setDialogTitle("Save vehicles file");
		state = fc.showSaveDialog(null);
		if (state == JFileChooser.APPROVE_OPTION){
			VEHICLEOUTPUTFILE = fc.getSelectedFile().getAbsolutePath();
		}else if (state == JFileChooser.CANCEL_OPTION) return;
		if (VEHICLEOUTPUTFILE == null) return;
		
		vehicleConverter.exportVehicles(VEHICLEOUTPUTFILE);
		
		/////////////////////////
		//EXPORT TRANSIT SCHEDULE
		/////////////////////////
			
		final String SCHEDULEOUTPUTNAME = config.getParam("gtfs", "outputFile");
		
		TransitScheduleWriterV1 writer = new TransitScheduleWriterV1(outputSchedule);
		writer.write(SCHEDULEOUTPUTNAME);
		
		
	}
	
	
}
