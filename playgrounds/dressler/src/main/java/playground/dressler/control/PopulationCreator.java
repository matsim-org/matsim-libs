package playground.dressler.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import playground.dressler.ea_flow.PathStep;
import playground.dressler.ea_flow.StepEdge;
import playground.dressler.ea_flow.TimeExpandedPath;
import playground.dressler.network.IndexedLinkI;
import playground.dressler.network.IndexedNetworkI;
import playground.dressler.network.IndexedNodeI;

public class PopulationCreator {
//////////////////////////////////////////////////////////////////////////////////////
	//---------------------------Plans Converter----------------------------------------//
	//////////////////////////////////////////////////////////////////////////////////////

		// maps a NODE id to a LINK id, telling which way to the true and final sink the path should take. 
		public HashMap<Id, Id> pathSuffix;
	
		private FlowCalculationSettings _settings;
		private IndexedNetworkI _network;
		
		public PopulationCreator(FlowCalculationSettings settings) {
			this._settings = settings;
			this._network = settings.getNetwork();
			this.pathSuffix = new HashMap<Id, Id>();
		}

		/**
		 * Backtracks from what should be a sink to the real sinks, and adjusts this.pathSuffix
		 * Careful, this cannot deal with cycles!
		 * @param sink the real final supersink
		 */
		public void autoFixSink(IndexedNodeI sink) {
			System.out.println("Starting Autofix sink " + sink.getId());
			autoFixSink(sink, null);						
		}
		
		/**
		 * Backtracks from what should be a sink to the real sinks, and adjusts this.pathSuffix
		 * Careful, this cannot deal with cycles!
		 * @param sinkid the Id of the real final supersink
		 */
		public void autoFixSink(Id sinkid) {		
			IndexedNodeI sink = this._network.getIndexedNode(sinkid);
			if (sink == null) {
				System.out.println("Warning: autoFixSink could not find sink: '" + sinkid + "'. Skipping.");
			} else {
				autoFixSink(sink, null);	
			}									
		}
		
		private void autoFixSink(IndexedNodeI sink, IndexedLinkI nextLink) {						
			// this vertex already has a direction ... we don't want to change it.
			// this also prevents infinite loops in the algorithm, but doesn't really solve the associated problem 
			if (this.pathSuffix.containsKey(sink.getId())) return;
			
			if (nextLink != null) {
				this.pathSuffix.put(sink.getId(), nextLink.getId());
			}
						
			if (this._settings.isSink(sink)) return;
			
			// we are not at a sink
			for (IndexedLinkI link : sink.getInLinks()) {
				// recurse ...
				autoFixSink(link.getFromNode(), link);				
			}			
		}
		
		public Population createPopulation(List<TimeExpandedPath> paths) {
			return createPopulation(paths, null);
		}
			
		public Population createPopulation(List<TimeExpandedPath> thePaths, final Scenario scenario) {
			
			
			boolean org = scenario != null && scenario.getPopulation() != null;
			
			HashMap<Node, LinkedList<Person>> orgpersons = null;
			
			if (org) {
				orgpersons = new HashMap<Node,LinkedList<Person>>();
				for (Person person : scenario.getPopulation().getPersons().values()) {
					Plan plan = person.getPlans().get(0);
					if(((PlanImpl) plan).getFirstActivity().getLinkId()==null){
						continue;
					}

					Link link = scenario.getNetwork().getLinks().get(((PlanImpl) plan).getFirstActivity().getLinkId());
					if (link == null) {					
						continue;
					}

					Node node = link.getToNode();
					IndexedNodeI inode = this._network.getIndexedNode(node.getId());
					if (!this._network.getNodes().contains(inode)) {
						continue;
					}
					
					if (orgpersons.get(node) == null) {
						LinkedList<Person> list = new LinkedList<Person>();
						list.add(person);
						orgpersons.put(node, list);
					} else {
						LinkedList<Person> list = orgpersons.get(node);
						list.add(person);
					}
				}
			}
			  
			//construct Population
			Population result = new ScenarioImpl().getPopulation();
			int id = 1;
			for (TimeExpandedPath path : thePaths){
				if (path.isforward()) {
					//units of flow on the Path
					int nofpersons = path.getFlow();
					// list of links in order of the path
					
					LinkedList<Id> ids = new LinkedList<Id>();
					for (PathStep step : path.getPathSteps()){
						if (step instanceof StepEdge) {
							ids.add(((StepEdge) step).getEdge().getId());
						}
					}
					
					// should we add another step into the true sink?
					// e.g. from "1234->en1" to the true "en1->en2" link
					
					if (pathSuffix != null) {
						Id currentLinkId = ids.getLast();
												
						do {
						  Id currentNodeId = this._network.getIndexedLink(currentLinkId).getToNode().getId();						  
						  currentLinkId = pathSuffix.get(currentNodeId);
						  
						  if (currentLinkId != null) {
							    ids.add(currentLinkId);							    
						  }						  
						  
						} while (currentLinkId != null);
						
					}
									
					Node firstnode  = _network.getIndexedLink(ids.get(0)).getFromNode().getMatsimNode();

					// for each unit of flow, construct a person and plan
					for (int i = 1 ; i <= nofpersons; i++){
						
						LinkNetworkRouteImpl route;
						
						Id pid = null;
						Person person = null;
						
						Id startLinkId = null;
						int startindex = 0;
											
						boolean orgokay = false;

						// find a suitable original person or create a new one 
						if (org && orgpersons.get(firstnode) != null) {					
							LinkedList<Person> list = orgpersons.get(firstnode);
							person = list.poll();
							
							if(list.isEmpty()){
								orgpersons.remove(firstnode);
							}
							
							pid = person.getId();
							
							orgokay = true;
							//System.out.println("found person #" + i + "/" + nofpersons + " id " + pid + " at " + firstnode.getId());
							
							startLinkId = ((PlanImpl) person.getPlans().get(0)).getFirstActivity().getLinkId();
							startindex = 0;
													
							// now delete all plans so that only our new plan will be in there
							// FIXME
							// Is this the MATSim way to do it? 
							
							person.getPlans().clear();
						} 

						if (!orgokay) {
							pid = new IdImpl("new" + String.valueOf(id));
							person = new PersonImpl(pid);
							id++;
							//System.out.println("created person #" + i + "/" + nofpersons + " id " + pid + " at " + firstnode.getId());

							startLinkId = ids.get(0);
							startindex = 1;
						}
						
						
						//System.out.println("starts on link " + startLinkId + " from " + this._network.getLinks().get(startLinkId).getFromNode().getId() + " --> " + this._network.getLinks().get(startLinkId).getToNode().getId());
						
						// add "sink flow"
						Id endLinkId = ids.getLast();
						int endindex = ids.size() - 1;
						
					
										
						route = new LinkNetworkRouteImpl(startLinkId, endLinkId);

						List<Id> routeLinkIds = new ArrayList<Id>();					
						for (int j = startindex; j < endindex ; j++) {
								routeLinkIds.add(ids.get(j));
						}
						route.setLinkIds(startLinkId, routeLinkIds, endLinkId);


						LegImpl leg = new LegImpl(TransportMode.car);					
						leg.setRoute(route);
						
						ActivityImpl home = new ActivityImpl("h", startLinkId);
						
						ActivityImpl work = new ActivityImpl("w", endLinkId);

						home.setEndTime(0);
						work.setEndTime(0);
						
						Plan plan = new PlanImpl(person);
						plan.addActivity(home);
						plan.addLeg(leg);
						plan.addActivity(work);
						person.addPlan(plan);
						result.addPerson(person);
					}
				} else { // residual edges
					// this should not happen!
					System.out.println("createPopulation encountered a residual step in");
					System.out.println(path);
					System.out.println("This should not happen!");
				}


			}

			return result;
		}


}
