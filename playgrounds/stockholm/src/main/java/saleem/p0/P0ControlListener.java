package saleem.p0;

import java.util.ArrayList;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;

public class P0ControlListener implements StartupListener, IterationStartsListener,IterationEndsListener, ShutdownListener {
	public NetworkImpl network;
	P0ControlHandler handler;
	public ArrayList<Double> avgabsolutepressuredifference = new ArrayList<Double>();//To check the convergence quality
	public ArrayList<Double> initialabsolutepressuredifference = new ArrayList<Double>();//To check the convergence quality
	public ArrayList<Double> iters = new ArrayList<Double>();//To check the convergence quality
	public ArrayList<Double> itersscaled = new ArrayList<Double>();
	public P0ControlListener(NetworkImpl network){
		this.network = network;
	}
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		//handler = new P0QueueDelayControl(network, event.getIteration());
		handler.initialise(event.getIteration());//To avoid creating objects every time, to save memory
	    network.setNetworkChangeEvents(P0ControlHandler.events);
//	    P0ControlHandler.events.removeAll(P0ControlHandler.events);
	    //event.getServices().getEvents().addHandler(handler);
		
	}
	public void populateInitialAbsolutePressureDifference(){
		initialabsolutepressuredifference = new ArrayList<Double>();
		TextReaderWriter rw = new TextReaderWriter();
		initialabsolutepressuredifference = rw.readFromTextFile("H:\\Mike Work\\initabsdiff.txt");
	}
	@Override
	public void notifyShutdown(ShutdownEvent event) {
//		writeInitialAbsolutePressureDifference();
		handler.plotStats();
		handler.plotAbsoultePressureDifference(iters, initialabsolutepressuredifference, avgabsolutepressuredifference);
		handler.AverageDelayOverLast20Iters();
		// TODO Auto-generated method stub
		
	}
	public void writeInitialAbsolutePressureDifference(){
		TextReaderWriter rw = new TextReaderWriter();
		rw.writeToTextFile(avgabsolutepressuredifference, "H:\\Mike Work\\initabsdiff.txt");
	}
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		//handler.printEvents();
		//network.setNetworkChangeEvents(P0QueueDelayControl.events);
		iters.add(Double.parseDouble(Integer.toString(event.getIteration())));
		avgabsolutepressuredifference.add(handler.getAvgPressDiffOverIter());
		handler.populatelastCapacities();
		handler.printDelayStats();
		handler.printCapacityStats();
		handler.plotStats();
		handler.plotAbsolutePressures();
		handler.plotAbsoultePressureDifference(iters, initialabsolutepressuredifference, avgabsolutepressuredifference);
//		handler.printDelayStats();
		// TODO Auto-generated method stub
		
	}
	@Override
	public void notifyStartup(StartupEvent event) {
		handler = new P0ControlHandler(network);
	    event.getServices().getEvents().addHandler(handler);
		// TODO Auto-generated method stub
		populateInitialAbsolutePressureDifference();
	}
}
