package saleem.p0;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.network.NetworkImpl;

public class P0ControlListener implements IterationStartsListener,IterationEndsListener, ShutdownListener {
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
		handler = new P0ControlHandler(network, event.getIteration());
	    event.getControler().getEvents().addHandler(handler);
	    network.setNetworkChangeEvents(P0ControlHandler.events);
	    P0ControlHandler.events.removeAll(P0ControlHandler.events);
	    //event.getControler().getEvents().addHandler(handler);
		
	}
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		handler.plotStats();
		handler.plotAbsoultePressureDifference(iters, itersscaled, initialabsolutepressuredifference, avgabsolutepressuredifference);
		handler.printDelayStats();
		// TODO Auto-generated method stub
		
	}
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		//handler.printEvents();
		//network.setNetworkChangeEvents(P0QueueDelayControl.events);
		if(event.getIteration()==0){
			itersscaled.add(Double.parseDouble(Integer.toString(event.getIteration())));
			initialabsolutepressuredifference.add(handler.getAvgPressDiffOverIter());
		}else if(event.getIteration()%10==0){//To ensure that the dashed line doesnt become solid line
			itersscaled.add(Double.parseDouble(Integer.toString(event.getIteration())));
			initialabsolutepressuredifference.add(new Double(initialabsolutepressuredifference.get(0)));
		}
		iters.add(Double.parseDouble(Integer.toString(event.getIteration())));
		avgabsolutepressuredifference.add(handler.getAvgPressDiffOverIter());
		handler.populatelastCapacities();
		handler.printDelayStats();
		handler.printCapacityStats();
		handler.plotStats();
		handler.plotAbsolutePressures();
		handler.plotAbsoultePressureDifference(iters, itersscaled, initialabsolutepressuredifference, avgabsolutepressuredifference);
		handler.printDelayStats();
		// TODO Auto-generated method stub
		
	}
}
