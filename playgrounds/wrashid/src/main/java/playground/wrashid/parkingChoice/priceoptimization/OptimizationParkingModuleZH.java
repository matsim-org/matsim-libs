package playground.wrashid.parkingChoice.priceoptimization;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.parkingchoice.PC2.infrastructure.PC2Parking;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.utils.io.IOUtils;

import playground.wrashid.parkingChoice.freeFloatingCarSharing.analysis.AverageWalkDistanceStatsZH;
import playground.wrashid.parkingChoice.freeFloatingCarSharing.analysis.ParkingGroupOccupanciesZH;
import playground.wrashid.parkingChoice.priceoptimization.analysis.ArrivalDepartureParkingHandler;
import playground.wrashid.parkingChoice.priceoptimization.infrastracture.OptimizableParking;
import playground.wrashid.parkingChoice.priceoptimization.scoring.ParkingScoreManager;
import playground.wrashid.parkingChoice.priceoptimization.simulation.ParkingChoiceSimulation;
import playground.wrashid.parkingChoice.priceoptimization.simulation.ParkingInfrastructureManager;

public class OptimizationParkingModuleZH implements IterationStartsListener, 
	IterationEndsListener, StartupListener, BeforeMobsimListener {
	
	private AverageWalkDistanceStatsZH averageWalkDistanceStatsZH;
	private ParkingGroupOccupanciesZH parkingGroupOccupanciesZH;
	private ArrivalDepartureParkingHandler arrivalDepartureParkingHandler;
	private EventsManager eventsManager;
	private EventWriterXML eventsWriter;
	private Controler controler;	
	
	
	private ParkingScoreManager parkingScoreManager;
	public final ParkingScoreManager getParkingScoreManager() {
		return parkingScoreManager;
	}

	public final void setParkingScoreManager(ParkingScoreManager parkingScoreManager) {
		this.parkingScoreManager = parkingScoreManager;
	}

	private ParkingInfrastructureManager parkingInfrastructureManager;
	private ParkingChoiceSimulation parkingSimulation;

	public OptimizationParkingModuleZH(Controler controler) {
		this.controler = controler;
		controler.addControlerListener(this);

		try {
			SetupParkingForOptimizationScenario.prepare(this,controler);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		parkingSimulation = new ParkingChoiceSimulation(controler.getScenario(), parkingInfrastructureManager);
		controler.getEvents().addHandler(parkingSimulation);
//		controler.addControlerListener(parkingSimulation);
		// was not doing anything there. kai, jul'15
	}

	public final ParkingInfrastructureManager getParkingInfrastructure() {
		return parkingInfrastructureManager;
	}
	
	public final void setParkingInfrastructurManager(ParkingInfrastructureManager parkingInfrastructureManager) {
		this.parkingInfrastructureManager = parkingInfrastructureManager;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		parkingScoreManager.prepareForNewIteration();
		parkingInfrastructureManager.reset();
		parkingSimulation.prepareForNewIteration();
	}

	protected final ParkingInfrastructureManager getParkingInfrastructureManager() {
		return parkingInfrastructureManager;
	}
	
	protected final ParkingChoiceSimulation getParkingSimulation() {
		return parkingSimulation;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
		
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		
		
		/*for (ParkingArrivalEvent rA: rentableArrival) {
			for (ParkingDepartureEvent rD: rentableDeparture) {
				double rentedTime = 0;
				if (rA.getParkingId(rA.getAttributes()).equals(rD.getParkingId(rD.getAttributes())) && rD.getTime() > rA.getTime()){
					rentedTime = rD.getTime()-rA.getTime();
					double tempTime =0;
					if(rentableDur.containsKey(rA.getParkingId(rA.getAttributes()))){
						tempTime = rentableDur.get(rA.getParkingId(rA.getAttributes()));
						rentableDur.put(rA.getParkingId(rA.getAttributes()), rentedTime+tempTime);
					} else {
						rentableDur.put(rA.getParkingId(rA.getAttributes()), rentedTime);
					}
					break;
				}
			}
		}*/
		//Save rentable parking information
		/*HashMap<String, ArrayList<String>> rentableInfo = new HashMap<String, ArrayList<String>>();
		for (PC2Parking parking : getParkingInfrastructure().getAllParkings().values()) {
			if (parking instanceof OptimizableParking){
				OptimizableParking par = (OptimizableParking) parking;
				ArrayList<String> info = new ArrayList<String>();
				
				double occup = Double.NaN;
				double rentedTime = Double.NaN;
				double pricePerHour=par.getCostPerHour();
				if (!(rentableDur.get(par.getId()) == null)){
					rentedTime = rentableDur.get(par.getId());
					occup = rentableDur.get(par.getId())/(24 * 60 * 60);
				}
				info.add(parking.getId().toString());
				info.add(Double.toString(rentedTime));
				info.add(Double.toString(occup));
				info.add(Double.toString(pricePerHour));
				info.add(Double.toString(parking.getCoordinate().getX()));
				info.add(Double.toString(parking.getCoordinate().getY()));
				rentableInfo.put(parking.getId().toString(), info);
			}
		}*/
		
		
		
		//String outputFolder = event.getControler().getControlerIO().getIterationPath(iter);
		//CsvFileWriter.writeCsvFile(outputFolder+"/rentableParkingInfo.csv", rentableInfo);
		
		
		
		//====================
	
		
		
		
		eventsManager = EventsUtils.createEventsManager();
		eventsWriter = new EventWriterXML(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "parkingEvents.xml.gz"));
		eventsManager.addHandler(eventsWriter);
		parkingGroupOccupanciesZH = new ParkingGroupOccupanciesZH(this.controler);
		eventsManager.addHandler(parkingGroupOccupanciesZH);
		averageWalkDistanceStatsZH = new AverageWalkDistanceStatsZH(getParkingInfrastructureManager().getAllParkings());
		eventsManager.addHandler(averageWalkDistanceStatsZH);
		
		arrivalDepartureParkingHandler = new ArrivalDepartureParkingHandler();
		eventsManager.addHandler(arrivalDepartureParkingHandler);

		eventsManager.resetHandlers(0);
		//eventsWriter.init(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "parkingEvents.xml.gz"));
		
		getParkingInfrastructure().setEventsManager(eventsManager);
	}
	
	
	/*@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		//TODO send output to file
		int iteration = event.getIteration();
		final BufferedWriter outLink = IOUtils.getBufferedWriter(this.controler.getControlerIO().getIterationFilename(iteration, "prices.txt"));
		HashMap<Id<PC2Parking>, double[]> rentableDurW = this.arrivalDepartureParkingHandler.getRentableDurPerHour();

		try {
			for (PC2Parking parking : getParkingInfrastructure().getAllParkings().values()) {
				if (parking instanceof OptimizableParking){
					OptimizableParking par = (OptimizableParking) parking;

					double oldPrice = par.getCostPerHour();
					
					outLink.write(par.getId() + ";" + Double.toString(oldPrice) + ";" + par.getCoordinate().getX() + ";" +
					par.getCoordinate().getY() + ";" + par.getMaximumParkingCapacity());
					if (!(rentableDurW.get(par.getId()) == null)) {
						
						double[] durations = rentableDurW.get(par.getId());
						for (int i = 0; i < 24; i++) {
							outLink.write(";" + durations[i]/(60 * 60 * par.getMaximumParkingCapacity()));
						}						
						
					}
					else {
						for (int i = 0; i < 24; i++) 
							outLink.write(";0.0");
						
					}
					
					if (par.getGroupName().equals("streetParking")) {
						boolean hight = par.isHighTariff();
						for (int i = 0; i < 24; i++) {
							if (hight)
								outLink.write(";" + par.getCostPerHour(i) * 2);

							else
								outLink.write(";" + par.getCostPerHour(i));
						}
					
					}
					else {
						for (int i = 0; i < 24; i++) 
							outLink.write(";" + par.getCostPerHour(i));
						
					}
				
					outLink.newLine();
			}
								
			}
			outLink.flush();
			outLink.close();
		}
		 catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		HashMap<Id<PC2Parking>, Double> rentableDur = this.arrivalDepartureParkingHandler.getRentableDur();

				//==================== Setting new prices for rentable parking dependig on the usage
				for (PC2Parking parking : getParkingInfrastructure().getAllParkings().values()) {
					double occup = 0;
					if (parking instanceof OptimizableParking){
						OptimizableParking par = (OptimizableParking) parking;

						double oldPrice = par.getCostPerHour();
						if (!(rentableDur.get(par.getId()) == null)){
							//Check if rentable parking was used (is in list)
							occup = rentableDur.get(par.getId())/(24 * 60 * 60 * par.getMaximumParkingCapacity());
							if (occup > 1.0)
								System.out.println();
							if (occup > 0.85){
							//If occupancy was > 0.85, increase price by 0.25, else decrease price by 0.25
								if (oldPrice < 200){
									par.setCostPerHour(oldPrice + 0.25);
								}
							} else {
								if (oldPrice >= 0.25) {
									par.setCostPerHour(oldPrice - 0.25);	
								}
								else
									par.setCostPerHour(0.0);
							}
						}
						//rentable space was not used at all (is not in list) => also decrease price by 0.25
						else {
							if (oldPrice >= 0.25) {
								par.setCostPerHour(oldPrice - 0.25);	
							}
							else
								par.setCostPerHour(0.0);
						}
						
										
					}
				}
		
		
		
		parkingGroupOccupanciesZH.savePlot(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "parkingGroupOccupancy.png"));
		averageWalkDistanceStatsZH.printStatistics();
		
		eventsManager.finishProcessing();
		eventsWriter.reset(0);		
	}*/


	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		//TODO send output to file
		int iteration = event.getIteration();
		final BufferedWriter outLink = IOUtils.getBufferedWriter(this.controler.getControlerIO().getIterationFilename(iteration, "prices.txt"));
		HashMap<Id<PC2Parking>, double[]> rentableDur = this.arrivalDepartureParkingHandler.getRentableDurPerHour();

		try {
			for (PC2Parking parking : getParkingInfrastructure().getAllParkings().values()) {
				if (parking instanceof OptimizableParking){
					OptimizableParking par = (OptimizableParking) parking;

					double oldPrice = par.getCostPerHour();
					
					outLink.write(par.getId() + ";" + Double.toString(oldPrice) + ";" + par.getCoordinate().getX() + ";" +
					par.getCoordinate().getY() + ";" + par.getMaximumParkingCapacity());
					if (!(rentableDur.get(par.getId()) == null)) {
						
						double[] durations = rentableDur.get(par.getId());
						for (int i = 0; i < 24; i++) {
							outLink.write(";" + durations[i]/(60 * 60 * par.getMaximumParkingCapacity()));
						}						
						
					}
					else {
						for (int i = 0; i < 24; i++) 
							outLink.write(";0.0");
						
					}
					
					if (par.getGroupName().equals("streetParking")) {
						boolean hight = par.isHighTariff();
						for (int i = 0; i < 24; i++) {
							if (hight)
								outLink.write(";" + par.getCostPerHour(i) * 2);

							else
								outLink.write(";" + par.getCostPerHour(i));
						}
					
					}
					else {
						for (int i = 0; i < 24; i++) 
							outLink.write(";" + par.getCostPerHour(i));
						
					}
				
					outLink.newLine();
			}
								
			}
			outLink.flush();
			outLink.close();
		}
		 catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
				//==================== Setting new prices for rentable parking dependig on the usage
				for (PC2Parking parking : getParkingInfrastructure().getAllParkings().values()) {
					double occup = 0;
					if (parking instanceof OptimizableParking){
						OptimizableParking par = (OptimizableParking) parking;

						if (!(rentableDur.get(par.getId()) == null)){

							double[] durations = rentableDur.get(par.getId());
							for (int i = 0; i < 24; i++) {
							
								occup = durations[i]/(60 * 60 * par.getMaximumParkingCapacity());
								if (occup > 1.0)
									System.out.println();
								
								double oldPrice = par.getCostPerHour(i);
								if (occup > 0.85){
								//If occupancy was > 0.85, increase price by 0.25, else decrease price by 0.25
									if (oldPrice < 200){
										par.setCostPerHour(oldPrice + 0.25,i);
									}
								} else {
									if (oldPrice >= 0.25) {
										par.setCostPerHour(oldPrice - 0.25,i);	
									}
									else
										par.setCostPerHour(0.0,i);
								}
							}
						}
						//rentable space was not used at all (is not in list) => also decrease price by 0.25
						else {
							for (int i = 0; i < 24; i++) {
								double oldPrice = par.getCostPerHour(i);

								if (oldPrice >= 0.25) {
									par.setCostPerHour(oldPrice - 0.25, i);	
								}
								else
									par.setCostPerHour(0.0, i);
							}
						}
						
										
					}
				}
		
		
		
		parkingGroupOccupanciesZH.savePlot(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "parkingGroupOccupancy.png"));
		averageWalkDistanceStatsZH.printStatistics();
		
		eventsManager.finishProcessing();
		eventsWriter.reset(0);		
	}
	

}
