package org.processmining.plugins.bpmn;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.SwingConstants;

import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.AttributeMap.SerializablePoint2D;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;

import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.AttributeMap;

import org.processmining.models.graphbased.directed.AbstractDirectedGraphEdge;
import org.processmining.models.graphbased.directed.AbstractDirectedGraphNode;
import org.processmining.models.graphbased.directed.ContainableDirectedGraphElement;
import org.processmining.models.graphbased.directed.ContainingDirectedGraphNode;
import org.processmining.models.graphbased.directed.DirectedGraph;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;

import org.processmining.models.graphbased.directed.bpmn.BPMNEdge;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.bpmn.elements.Activity;
import org.processmining.models.graphbased.directed.bpmn.elements.Event;
import org.processmining.models.graphbased.directed.bpmn.elements.Event.EventType;
import org.processmining.models.graphbased.directed.bpmn.elements.Event.EventTrigger;
import org.processmining.models.graphbased.directed.bpmn.elements.Gateway.GatewayType;
import org.processmining.models.graphbased.directed.bpmn.elements.Flow;
import org.processmining.models.graphbased.directed.bpmn.elements.Gateway;
import org.processmining.models.graphbased.directed.bpmn.elements.SubProcess;
import org.processmining.models.graphbased.directed.bpmn.elements.Swimlane;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;

import org.processmining.models.graphbased.directed.petrinet.elements.ExpandableSubNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.jgraph.ProMJGraph;
import org.processmining.models.jgraph.ProMJGraphVisualizer;
import org.processmining.models.jgraph.elements.ProMGraphPort;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.ArrayPetriNet;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.hierarchical.JGraphHierarchicalLayout;

public class BPMNtoPN {

	private ExpandableSubNet subNet = null;

	@Plugin(name = "BPMN to PetriNet with sub-process", parameterLabels = { "BPMNDiagram" }, returnLabels = {
	"Array Petri Net"}, returnTypes = { ArrayPetriNet.class}, userAccessible = true)
@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "BPMNMeasures")
@PluginVariant(requiredParameterLabels = { 0 }, variantLabel = "Trasform BPMN to PN")
public ArrayPetriNet BPMN2PN_WithSub(PluginContext c, BPMNDiagram bpmn) {
	
		Collection<String> error = this.isWellFormed(bpmn);

		// final LinkedHashMap<Place, Flow> placeMap = new LinkedHashMap<Place,
		// Flow>();
		LinkedHashMap<Flow, Place> flowMap = new LinkedHashMap<Flow, Place>();
		String nameBPMNDiagram =  bpmn.getLabel();
		if(nameBPMNDiagram.length()<=0){
			nameBPMNDiagram="Unnamed";
		
		}
		PetrinetGraph net = PetrinetFactory.newPetrinet(nameBPMNDiagram);
		Marking marking = new Marking();
		Collection<Swimlane> collezioneswimlane = bpmn.getSwimlanes();
		ContainingDirectedGraphNode parent = null;

		traslateFlow(bpmn, flowMap, net, parent);

		translateTask(bpmn, flowMap, net, parent);

		translateGateway(bpmn, flowMap, net, parent);

		translateEvent(bpmn, flowMap, net, marking, parent);

		translateSubProcess(bpmn, flowMap, net, marking, parent);
		
		
		

		if (c instanceof UIPluginContext) {
			layoutcreate(c, net);
		}
		String errorLog = error.toString();
		int numsub = bpmn.getSubProcesses().size();
		if(numsub<=0){
			numsub=0;
		}
		
		ArrayPetriNet apn = new ArrayPetriNet();
		
		
		
		apn.setElementListpn(net,marking);
		c.addConnection(new BPMNtoPNConnection(bpmn, net, errorLog, flowMap
				.values()));

		c.addConnection(new InitialMarkingConnection(net, marking));
		
		int i=1;
		
		for (SubProcess sub : bpmn.getSubProcesses()) {
			
			Marking markingsub = new Marking();
			LinkedHashMap<Flow, Place> flowMapsub = new LinkedHashMap<Flow, Place>();
			PetrinetGraph netsub = transSubProcess(sub,markingsub,flowMapsub);
			apn.setElementListpn(netsub,markingsub);
			
			c.addConnection(new BPMNtoPNConnection(bpmn, netsub, errorLog, flowMapsub
					.values()));

			c.addConnection(new InitialMarkingConnection(netsub, markingsub));
			i++;
		}
		
		
		
		

		

		return apn;
	}
	
	
	@Plugin(name = "BPMN to PetriNet", parameterLabels = { "BPMNDiagram" }, returnLabels = {
			"Petri Net", "Marking"}, returnTypes = { Petrinet.class, Marking.class}, userAccessible = true)
	@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "BPMNMeasures")
	@PluginVariant(requiredParameterLabels = { 0 }, variantLabel = "Trasform BPMN to PN")
	public Object[] BPMN2PN(PluginContext c, BPMNDiagram bpmn) {
		Collection<String> error = this.isWellFormed(bpmn);

		// final LinkedHashMap<Place, Flow> placeMap = new LinkedHashMap<Place,
		// Flow>();
		LinkedHashMap<Flow, Place> flowMap = new LinkedHashMap<Flow, Place>();
		String nameBPMNDiagram =  bpmn.getLabel();
		if(nameBPMNDiagram.length()<=0){
			nameBPMNDiagram="Unnamed";
		
		}
		PetrinetGraph net = PetrinetFactory.newPetrinet(nameBPMNDiagram);
		Marking marking = new Marking();
		Collection<Swimlane> collezioneswimlane = bpmn.getSwimlanes();
		ContainingDirectedGraphNode parent = null;

		traslateFlow(bpmn, flowMap, net, parent);

		translateTask(bpmn, flowMap, net, parent);

		translateGateway(bpmn, flowMap, net, parent);

		translateEvent(bpmn, flowMap, net, marking, parent);

		translateSubProcess(bpmn, flowMap, net, marking, parent);
		
		
		

		if (c instanceof UIPluginContext) {
			layoutcreate(c, net);
		}
		String errorLog = error.toString();
		int numsub = bpmn.getSubProcesses().size();
		if(numsub<=0){
			numsub=0;
		}
		
		
		c.addConnection(new BPMNtoPNConnection(bpmn, net, errorLog, flowMap
				.values()));

		c.addConnection(new InitialMarkingConnection(net, marking));
		
		
		
		
		

		

		return new Object[] { net, marking };
	}

	private void traslateFlow(BPMNDiagram bpmn,
			LinkedHashMap<Flow, Place> flowMap, PetrinetGraph net,
			ContainingDirectedGraphNode parent) {
		// gli archi del diagramma BPMN diventano piazze della rete Petri
		for (Flow g : bpmn.getFlows()) {
			String f = g.getSource().getLabel() + "#";
			String z = g.getTarget().getLabel();
			ContainingDirectedGraphNode parentFlow = g.getParentSubProcess();
			// System.out.println("parent: " + parent);
			if (parentFlow == parent) {
				Place p = net.addPlace(f + z, this.subNet);

				flowMap.put(g, p);
			}

		}

	}

	private void transFlows(List<Flow> listFlow,
			LinkedHashMap<Flow, Place> flowMap, PetrinetGraph net) {
		for (Flow flow : listFlow) {
			String f = flow.getSource().getLabel() + "#";
			String z = flow.getTarget().getLabel();
			// ContainingDirectedGraphNode parentFlow =
			// flow.getParentSubProcess();
			// System.out.println("parent: " + parent);
			// if (true) {
			Place p = net.addPlace(f + z, this.subNet);

			flowMap.put(flow, p);
			// }
		}
	}

	private void transNodes(List<BPMNNode> listnode,
			LinkedHashMap<Flow, Place> flowMap, PetrinetGraph net, Marking marking) {
		for (BPMNNode node : listnode) {
if(node instanceof Event ){
	Event e = (Event) node;
	
	if (e.getEventType().equals(EventType.START)
			&& e.getEventTrigger().equals(EventTrigger.NONE)) {

		// Place p = new Place(e.getLabel(), net);
		String name = e.getLabel().toUpperCase();
		Place p = net.addPlace("p" + e.getLabel(), this.subNet);

		Transition t = net.addTransition("t_" + name, this.subNet);
		t.setInvisible(true);
		net.addArc(p, t, 1, this.subNet);
		
		marking.add(p, 1);
		
		for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : e
				.getGraph().getOutEdges(e)) {

			Place pst = flowMap.get(s);

			net.addArc(t, pst, 1, this.subNet);

		}

	}
	if (e.getEventType().equals(EventType.END)
			&& e.getEventTrigger().equals(EventTrigger.NONE)) {

		Place p = net.addPlace("p" + e.getLabel(), this.subNet);

		Transition t = net.addTransition("t_" + e.getLabel(),
				this.subNet);

		t.setInvisible(true);
		net.addArc(t, p, 1, this.subNet);

		for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : e
				.getGraph().getInEdges(e)) {

			Place pst = flowMap.get(s);

			net.addArc(pst, t, 1, this.subNet);

		}

	}
	if (e.getEventType().equals(EventType.INTERMEDIATE)
			&& !e.getEventTrigger().equals(EventTrigger.NONE)) {

		Transition t = net.addTransition(e.getLabel(), this.subNet);

		if (e.getBoundingNode() == null) {
			BPMNEdge<? extends BPMNNode, ? extends BPMNNode> g = e
					.getGraph().getInEdges(e).iterator().next();
			if (g instanceof Flow && g != null) {
				Place ps_pre = flowMap.get(g);

				g = e.getGraph().getOutEdges(e).iterator().next();
				;
				if (g instanceof Flow && g != null) {
					Place ps_post = flowMap.get(g);

					net.addArc(ps_pre, t, 1, this.subNet);
					net.addArc(t, ps_post, 1, this.subNet);
				}

			}
		} else {
			//  un evento di confine

		}

	}
}else{
			if (node instanceof Activity) {
				Activity a = (Activity) node;
				String id = a.getLabel();

				Transition t = net.addTransition(id + "+start", this.subNet);
				Place p = net.addPlace(id, this.subNet);
				net.addArc(t, p, 1, this.subNet);
				Transition t1 = net
						.addTransition(id + "+complete", this.subNet);
				net.addArc(p, t1, 1, this.subNet);

				for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : a
						.getGraph().getInEdges(a)) {
					if (s instanceof Flow) {

						Place pst = flowMap.get(s);

						net.addArc(pst, t, 1, this.subNet);
					}

				}
				for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : a
						.getGraph().getOutEdges(a)) {
					if (s instanceof Flow) {

						Place pst = flowMap.get(s);

						net.addArc(t1, pst, 1, this.subNet);
					}
				}

			} else {
				if (node instanceof Gateway) {
					Gateway g = (Gateway) node;
					// gateway data-based
					if (g.getGatewayType().equals(GatewayType.DATABASED)) {
						int i = 0;
						Map<String, Transition> tranMap = new HashMap<String, Transition>();

						// gateway data-based if branch
						if (g.getGraph().getOutEdges(g).size() > 1
								&& g.getGraph().getInEdges(g).size() == 1) {
							for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g
									.getGraph().getOutEdges(g)) {
								String source = s.getSource().getLabel();
								String target = s.getTarget().getLabel();

								Transition t = net.addTransition(g.getLabel()
										+ "_" + i++, this.subNet);
								t.setInvisible(true);
								tranMap.put(target + source, t);

								Place pst = flowMap.get(s);

								net.addArc(t, pst, 1, this.subNet);

							}
							for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g
									.getGraph().getInEdges(g)) {
								String source = s.getSource().getLabel();
								String target = s.getTarget().getLabel();

								Place pst = flowMap.get(s);

								for (Transition t : tranMap.values()) {

									net.addArc(pst, t, 1, this.subNet);

								}
							}
						} else {
							// gateway merge
							if (g.getGraph().getOutEdges(g).size() == 1
									&& g.getGraph().getInEdges(g).size() > 1) {

								Place ps = null;
								for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> out : g
										.getGraph().getOutEdges(g)) {
									ps = flowMap.get(out);
								}
								i = 0;
								for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g
										.getGraph().getInEdges(g)) {

									Place pst = flowMap.get(s);

									Transition t = net.addTransition(
											g.getLabel() + "_" + i++,
											this.subNet);
									t.setInvisible(true);
									net.addArc(pst, t, 1, this.subNet);

									net.addArc(t, ps, this.subNet);

								}
							} else {
								if (g.getGraph().getOutEdges(g).size() > 1
										&& g.getGraph().getInEdges(g).size() > 1) {
									i = 0;
									Place placecentral = net.addPlace(
											g.getLabel(), this.subNet);
									for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g
											.getGraph().getOutEdges(g)) {
										String source = s.getSource()
												.getLabel();
										String target = s.getTarget()
												.getLabel();

										Transition t = net.addTransition(
												g.getLabel() + "_" + i++,
												this.subNet);
										t.setInvisible(true);
										tranMap.put(target + source, t);

										Place pst = flowMap.get(s);

										net.addArc(t, pst, 1, this.subNet);

										net.addArc(placecentral, t, this.subNet);
									}
									i = 0;
									for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g
											.getGraph().getInEdges(g)) {
										String source = s.getSource()
												.getLabel();
										String target = s.getTarget()
												.getLabel();

										Place pst = flowMap.get(s);

										Transition t = net.addTransition(
												g.getLabel() + "_" + i++,
												this.subNet);
										t.setInvisible(true);
										net.addArc(pst, t, 1, this.subNet);

										net.addArc(t, placecentral, this.subNet);
										/*
										 * Place pst = flowMap.get(s);
										 * 
										 * for (Transition t : tranMap.values())
										 * {
										 * 
										 * net.addArc(pst, t, 1, this.subNet);
										 * 
										 * }
										 */
									}
								}

							}
						}

					} else {
						if (g.getGatewayType().equals(GatewayType.PARALLEL)) {
							// gateway parallel fork
							if (g.getGraph().getOutEdges(g).size() > 1
									&& g.getGraph().getInEdges(g).size() == 1) {
								BPMNEdge<? extends BPMNNode, ? extends BPMNNode> so = g
										.getGraph().getInEdges(g).iterator()
										.next();

								Place ps = flowMap.get(so);
								Transition t = net.addTransition(g.getLabel()
										+ "_fork", this.subNet);
								t.setInvisible(true);
								net.addArc(ps, t, 1, this.subNet);
								for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g
										.getGraph().getOutEdges(g)) {

									Place pst = flowMap.get(s);
									net.addArc(t, pst, 1, this.subNet);

								}

							} else {
								// gateway parallel Join
								if (g.getGraph().getOutEdges(g).size() == 1
										&& g.getGraph().getInEdges(g).size() > 1) {
									BPMNEdge<? extends BPMNNode, ? extends BPMNNode> so = g
											.getGraph().getOutEdges(g)
											.iterator().next();

									Place ps = flowMap.get(so);
									Transition t = net
											.addTransition(g.getLabel()
													+ "_join", this.subNet);
									t.setInvisible(true);
									net.addArc(t, ps, 1, this.subNet);
									for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g
											.getGraph().getInEdges(g)) {

										Place pst = flowMap.get(s);
										net.addArc(pst, t, 1, this.subNet);

									}

								}
							}
						} else {
							// gateway event-based
							if (g.getGatewayType().equals(
									GatewayType.EVENTBASED)) {
								// Exclusive event gateway
								if (g.getGraph().getOutEdges(g).size() > 1
										&& g.getGraph().getInEdges(g).size() == 1) {
									BPMNEdge<? extends BPMNNode, ? extends BPMNNode> so = g
											.getGraph().getInEdges(g)
											.iterator().next();

									Place ps = flowMap.get(so);
									int i = 0;
									for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g
											.getGraph().getOutEdges(g)) {

										Place pst = flowMap.get(s);

										Transition t = net.addTransition(
												g.getLabel() + "_" + i++,
												this.subNet);
										t.setInvisible(true);
										net.addArc(t, pst, 1, this.subNet);

										net.addArc(ps, t, this.subNet);

									}

								}

							}
						}
					}
				}
			}
}
		}

	}
	
	private PetrinetGraph transSubProcess(SubProcess sub,Marking marking, LinkedHashMap<Flow, Place> flowMapsub){
		PetrinetGraph netsub = PetrinetFactory.newPetrinet(sub.getLabel());
		HashSet<ContainableDirectedGraphElement> children = (HashSet<ContainableDirectedGraphElement>) sub
				.getChildren();
		
		

		
	
		List<BPMNNode> listnode = new ArrayList<BPMNNode>();
		List<Flow> listFlow = new ArrayList<Flow>();
		for (ContainableDirectedGraphElement containableDirectedGraphElement : children) {
			if (containableDirectedGraphElement instanceof BPMNNode) {

				listnode.add((BPMNNode) containableDirectedGraphElement);
				System.out.println(""
						+ ((BPMNNode) containableDirectedGraphElement)
								.getLabel());
			} else {
				if (containableDirectedGraphElement instanceof Flow) {

					listFlow.add((Flow) containableDirectedGraphElement);
					System.out.println(""
							+ ((Flow) containableDirectedGraphElement)
									.getEdgeID());
				}
			}
		}

		transFlows(listFlow, flowMapsub, netsub);

		transNodes(listnode, flowMapsub, netsub,marking);
		 
		return netsub;

		
	}

	private PetrinetGraph transSubProcess(SubProcess sub,
			LinkedHashMap<Flow, Place> flowMap) {

		PetrinetGraph netsub = PetrinetFactory.newPetrinet(sub.getLabel());
		HashSet<ContainableDirectedGraphElement> children = (HashSet<ContainableDirectedGraphElement>) sub
				.getChildren();
		List<BPMNNode> listnode = new ArrayList<BPMNNode>();
		List<Flow> listFlow = new ArrayList<Flow>();
		for (ContainableDirectedGraphElement containableDirectedGraphElement : children) {
			if (containableDirectedGraphElement instanceof BPMNNode) {

				listnode.add((BPMNNode) containableDirectedGraphElement);
				System.out.println(""
						+ ((BPMNNode) containableDirectedGraphElement)
								.getLabel());
			} else {
				if (containableDirectedGraphElement instanceof Flow) {

					listFlow.add((Flow) containableDirectedGraphElement);
					System.out.println(""
							+ ((Flow) containableDirectedGraphElement)
									.getEdgeID());
				}
			}
		}

		transFlows(listFlow, flowMap, netsub);
		Marking marking = new Marking();
		transNodes(listnode, flowMap, netsub,marking);
		return netsub;
	}

	private void translateSubProcess(BPMNDiagram bpmn,
			LinkedHashMap<Flow, Place> flowMap, PetrinetGraph net,
			Marking marking, ContainingDirectedGraphNode parent) {
		for (SubProcess sub : bpmn.getSubProcesses()) {
			HashSet<ContainableDirectedGraphElement> childre = (HashSet<ContainableDirectedGraphElement>) sub
					.getChildren();
			List<BPMNNode> listnode = new ArrayList<BPMNNode>();
			List<Flow> listFlow = new ArrayList<Flow>();
			for (ContainableDirectedGraphElement containableDirectedGraphElement : childre) {
				if (containableDirectedGraphElement instanceof BPMNNode) {

					listnode.add((BPMNNode) containableDirectedGraphElement);
					System.out.println(""
							+ ((BPMNNode) containableDirectedGraphElement)
									.getLabel());
				} else {
					if (containableDirectedGraphElement instanceof Flow) {

						listFlow.add((Flow) containableDirectedGraphElement);
						System.out.println(""
								+ ((Flow) containableDirectedGraphElement)
										.getEdgeID());
					}
				}
			}

			//transFlows(listFlow, flowMap, net);

			//transNodes(listnode, flowMap, net);

			sub.isBMultiinstance();


			Transition t = net.addTransition(sub.getLabel() + "start",
					this.subNet);
			t.setInvisible(true);

			//Transition tend = net.addTransition(sub.getLabel() + "end", this.subNet);
			//tend.setInvisible(true);

			//Place pst_central = net.addPlace(sub.getLabel(), this.subNet);

			//net.addArc(pst_central, tend, 1, this.subNet);
			//net.addArc(t, pst_central, 1, this.subNet);
			
			for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : sub
					.getGraph().getInEdges(sub)) {
				if (s instanceof Flow) {

					Place pst = flowMap.get(s);

					net.addArc(pst, t, 1, this.subNet);
				}

			}

			for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : sub
					.getGraph().getOutEdges(sub)) {
				if (s instanceof Flow) {

					Place pst = flowMap.get(s);

					net.addArc(t, pst, 1, this.subNet);
				}

			}

			/*traslateFlow(bpmn, flowMap, net, sub);

			translateTask(bpmn, flowMap, net, sub);

			translateGateway(bpmn, flowMap, net, sub);

			translateEvent(bpmn, flowMap, net, marking, sub);

			Transition t = net.addTransition(sub.getLabel() + "start",
					this.subNet);
			t.setInvisible(true);
			Transition tend = net.addTransition(sub.getLabel() + "end",
					this.subNet);
			tend.setInvisible(true);

			for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : sub
					.getGraph().getInEdges(sub)) {
				if (s instanceof Flow) {

					Place pst = flowMap.get(s);

					net.addArc(pst, t, 1, this.subNet);
				}

			}

			for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : sub
					.getGraph().getOutEdges(sub)) {
				if (s instanceof Flow) {

					Place pst = flowMap.get(s);

					net.addArc(tend, pst, 1, this.subNet);
				}

			}

			Set<ContainableDirectedGraphElement> children = sub.getChildren();
			for (ContainableDirectedGraphElement containableDirectedGraphElement : children) {
				if (containableDirectedGraphElement instanceof Event) {
					Event evnt = (Event) containableDirectedGraphElement;
					if (evnt.getEventType() == EventType.START
							& evnt.getEventTrigger() == EventTrigger.NONE) {
						for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : evnt
								.getGraph().getOutEdges(evnt)) {
							if (s instanceof Flow) {

								Place pst = flowMap.get(s);

								Transition t2 = (Transition) pst.getGraph()
										.getInEdges(pst).iterator().next()
										.getSource();
								Place pt2 = (Place) t2.getGraph()
										.getInEdges(t2).iterator().next()
										.getSource();
								net.addArc(t, pt2, 1, this.subNet);
							}

						}
					} else {
						if (evnt.getEventType() == EventType.END
								& evnt.getEventTrigger() == EventTrigger.NONE) {
							for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : evnt
									.getGraph().getInEdges(evnt)) {
								if (s instanceof Flow) {

									Place pst = flowMap.get(s);

									Transition t2 = (Transition) pst.getGraph()
											.getOutEdges(pst).iterator().next()
											.getTarget();
									Place pt2 = (Place) t2.getGraph()
											.getOutEdges(t2).iterator().next()
											.getTarget();
									net.addArc(pt2, tend, 1, this.subNet);
								}

							}
						}
					}
				} else {
					if (containableDirectedGraphElement instanceof Activity) {

					}
				}
			}
			*/
		}
	}

	private void translateTask(BPMNDiagram bpmn,
			LinkedHashMap<Flow, Place> flowMap, PetrinetGraph net,
			ContainingDirectedGraphNode parent) {

		for (Activity c : bpmn.getActivities()) {
			if (parent == c.getParentSubProcess()) {
				String id = c.getLabel();

				Transition t = net.addTransition(id + "+start", this.subNet);
				Place p = net.addPlace(id, this.subNet);
				net.addArc(t, p, 1, this.subNet);
				Transition t1 = net
						.addTransition(id + "+complete", this.subNet);
				net.addArc(p, t1, 1, this.subNet);

				for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : c
						.getGraph().getInEdges(c)) {
					if (s instanceof Flow) {

						Place pst = flowMap.get(s);

						net.addArc(pst, t, 1, this.subNet);
					}

				}
				for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : c
						.getGraph().getOutEdges(c)) {
					if (s instanceof Flow) {

						Place pst = flowMap.get(s);

						net.addArc(t1, pst, 1, this.subNet);
					}
				}

			}
		}

	}

	private void translateGateway(BPMNDiagram bpmn,
			LinkedHashMap<Flow, Place> flowMap, PetrinetGraph net,
			ContainingDirectedGraphNode parent) {
		for (Gateway g : bpmn.getGateways()) {
			if (parent == g.getParentSubProcess()) {
				// gateway data-based
				if (g.getGatewayType().equals(GatewayType.DATABASED)) {
					int i = 0;
					Map<String, Transition> tranMap = new HashMap<String, Transition>();

					// gateway data-based if branch
					if (g.getGraph().getOutEdges(g).size() > 1
							&& g.getGraph().getInEdges(g).size() == 1) {
						for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g
								.getGraph().getOutEdges(g)) {
							String source = s.getSource().getLabel();
							String target = s.getTarget().getLabel();

							Transition t = net.addTransition(g.getLabel() + "_"
									+ i++, this.subNet);
							t.setInvisible(true);
							tranMap.put(target + source, t);

							Place pst = flowMap.get(s);

							net.addArc(t, pst, 1, this.subNet);

						}
						for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g
								.getGraph().getInEdges(g)) {
							String source = s.getSource().getLabel();
							String target = s.getTarget().getLabel();

							Place pst = flowMap.get(s);

							for (Transition t : tranMap.values()) {

								net.addArc(pst, t, 1, this.subNet);

							}
						}
					} else {
						// gateway merge
						if (g.getGraph().getOutEdges(g).size() == 1
								&& g.getGraph().getInEdges(g).size() > 1) {

							Place ps = null;
							for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> out : g
									.getGraph().getOutEdges(g)) {
								ps = flowMap.get(out);
							}
							i = 0;
							for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g
									.getGraph().getInEdges(g)) {

								Place pst = flowMap.get(s);

								Transition t = net.addTransition(g.getLabel()
										+ "_" + i++, this.subNet);
								t.setInvisible(true);
								net.addArc(pst, t, 1, this.subNet);

								net.addArc(t, ps, this.subNet);

							}
						} else {
							if (g.getGraph().getOutEdges(g).size() > 1
									&& g.getGraph().getInEdges(g).size() > 1) {
								i = 0;
								Place placecentral = net.addPlace(g.getLabel(),
										this.subNet);
								for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g
										.getGraph().getOutEdges(g)) {
									String source = s.getSource().getLabel();
									String target = s.getTarget().getLabel();

									Transition t = net.addTransition(
											g.getLabel() + "_" + i++,
											this.subNet);
									t.setInvisible(true);
									tranMap.put(target + source, t);

									Place pst = flowMap.get(s);

									net.addArc(t, pst, 1, this.subNet);

									net.addArc(placecentral, t, this.subNet);
								}
								i = 0;
								for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g
										.getGraph().getInEdges(g)) {
									String source = s.getSource().getLabel();
									String target = s.getTarget().getLabel();

									Place pst = flowMap.get(s);

									Transition t = net.addTransition(
											g.getLabel() + "_" + i++,
											this.subNet);
									t.setInvisible(true);
									net.addArc(pst, t, 1, this.subNet);

									net.addArc(t, placecentral, this.subNet);
									/*
									 * Place pst = flowMap.get(s);
									 * 
									 * for (Transition t : tranMap.values()) {
									 * 
									 * net.addArc(pst, t, 1, this.subNet);
									 * 
									 * }
									 */
								}
							}

						}
					}

				} else {
					if (g.getGatewayType().equals(GatewayType.PARALLEL)) {
						// gateway parallel fork
						if (g.getGraph().getOutEdges(g).size() > 1
								&& g.getGraph().getInEdges(g).size() == 1) {
							BPMNEdge<? extends BPMNNode, ? extends BPMNNode> so = g
									.getGraph().getInEdges(g).iterator().next();

							Place ps = flowMap.get(so);
							Transition t = net.addTransition(g.getLabel()
									+ "_fork", this.subNet);
							t.setInvisible(true);
							net.addArc(ps, t, 1, this.subNet);
							for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g
									.getGraph().getOutEdges(g)) {

								Place pst = flowMap.get(s);
								net.addArc(t, pst, 1, this.subNet);

							}

						} else {
							// gateway parallel Join
							if (g.getGraph().getOutEdges(g).size() == 1
									&& g.getGraph().getInEdges(g).size() > 1) {
								BPMNEdge<? extends BPMNNode, ? extends BPMNNode> so = g
										.getGraph().getOutEdges(g).iterator()
										.next();

								Place ps = flowMap.get(so);
								Transition t = net.addTransition(g.getLabel()
										+ "_join", this.subNet);
								t.setInvisible(true);
								net.addArc(t, ps, 1, this.subNet);
								for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g
										.getGraph().getInEdges(g)) {

									Place pst = flowMap.get(s);
									net.addArc(pst, t, 1, this.subNet);

								}

							}
						}
					} else {
						// gateway event-based
						if (g.getGatewayType().equals(GatewayType.EVENTBASED)) {
							// Exclusive event gateway
							if (g.getGraph().getOutEdges(g).size() > 1
									&& g.getGraph().getInEdges(g).size() == 1) {
								BPMNEdge<? extends BPMNNode, ? extends BPMNNode> so = g
										.getGraph().getInEdges(g).iterator()
										.next();

								Place ps = flowMap.get(so);
								int i = 0;
								for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g
										.getGraph().getOutEdges(g)) {

									Place pst = flowMap.get(s);

									Transition t = net.addTransition(
											g.getLabel() + "_" + i++,
											this.subNet);
									t.setInvisible(true);
									net.addArc(t, pst, 1, this.subNet);

									net.addArc(ps, t, this.subNet);

								}

							}

						}
					}
				}
			}
		}
	}

	private void translateEvent(BPMNDiagram bpmn,
			LinkedHashMap<Flow, Place> flowMap, PetrinetGraph net,
			Marking marking, ContainingDirectedGraphNode parent) {
		for (Event e : bpmn.getEvents()) {
			if (parent == e.getParentSubProcess()) {
				if (e.getEventType().equals(EventType.START)
						&& e.getEventTrigger().equals(EventTrigger.NONE)) {

					// Place p = new Place(e.getLabel(), net);
					String name = e.getLabel().toUpperCase();
					Place p = net.addPlace("p" + e.getLabel(), this.subNet);

					Transition t = net.addTransition("t_" + name, this.subNet);
					t.setInvisible(true);
					net.addArc(p, t, 1, this.subNet);
					SubProcess sub = e.getParentSubProcess();
					if (sub == null) {
						marking.add(p, 1);
					}else{
						
						//marking.add(p, 190);
					}
					for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : e
							.getGraph().getOutEdges(e)) {

						Place pst = flowMap.get(s);

						net.addArc(t, pst, 1, this.subNet);

					}

				}
				if (e.getEventType().equals(EventType.END)
						&& e.getEventTrigger().equals(EventTrigger.NONE)) {

					Place p = net.addPlace("p" + e.getLabel(), this.subNet);

					Transition t = net.addTransition("t_" + e.getLabel(),
							this.subNet);

					t.setInvisible(true);
					net.addArc(t, p, 1, this.subNet);

					for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : e
							.getGraph().getInEdges(e)) {

						Place pst = flowMap.get(s);

						net.addArc(pst, t, 1, this.subNet);

					}

				}
				if (e.getEventType().equals(EventType.INTERMEDIATE)
						&& !e.getEventTrigger().equals(EventTrigger.NONE)) {

					Transition t = net.addTransition(e.getLabel(), this.subNet);

					if (e.getBoundingNode() == null) {
						BPMNEdge<? extends BPMNNode, ? extends BPMNNode> g = e
								.getGraph().getInEdges(e).iterator().next();
						if (g instanceof Flow && g != null) {
							Place ps_pre = flowMap.get(g);

							g = e.getGraph().getOutEdges(e).iterator().next();
							;
							if (g instanceof Flow && g != null) {
								Place ps_post = flowMap.get(g);

								net.addArc(ps_pre, t, 1, this.subNet);
								net.addArc(t, ps_post, 1, this.subNet);
							}

						}
					} else {
						//  un evento di confine

					}

				}

			}
		}

	}

	private void layoutcreate(PluginContext c, PetrinetGraph net) {

		GraphLayoutConnection layout = new GraphLayoutConnection(net);
		try {
			layout = c.getConnectionManager().getFirstConnection(
					GraphLayoutConnection.class, c, net);
		} catch (ConnectionCannotBeObtained e) {
			/*
			 * Get a jgraph for this graph.
			 */

			ProMJGraph jgraph = ProMJGraphVisualizer.instance()
					.visualizeGraph(c, net).getGraph();
			/*
			 * Layout this jgraph.
			 */
			JGraphFacade facade = new JGraphFacade(jgraph);
			layOutFMJGraph(net, jgraph, facade);
			System.out.print("Creata Layout della PetriNet");
		}
	}

	private void layOutFMJGraph(
			DirectedGraph<? extends AbstractDirectedGraphNode, ? extends AbstractDirectedGraphEdge<?, ?>> graph,
			ProMJGraph jgraph, JGraphFacade facade) {
		JGraphHierarchicalLayout layout = new JGraphHierarchicalLayout();
		layout.setDeterministic(false);
		layout.setCompactLayout(false);
		layout.setFineTuning(true);
		layout.setParallelEdgeSpacing(20);
		layout.setOrientation(graph.getAttributeMap().get(
				AttributeMap.PREF_ORIENTATION, SwingConstants.SOUTH));

		facade.setOrdered(true);
		facade.setEdgePromotion(true);
		facade.setIgnoresCellsInGroups(false);
		facade.setIgnoresHiddenCells(false);
		facade.setIgnoresUnconnectedCells(false);
		facade.setDirected(false);
		facade.resetControlPoints();

		facade.run(layout, false);

		fixParallelTransitions(facade, 15);

		Map<?, ?> nested = facade.createNestedMap(true, false);

		jgraph.getGraphLayoutCache().edit(nested);
		jgraph.setUpdateLayout(layout);
	}

	private void fixParallelTransitions(JGraphFacade facade, double spacing) {
		ArrayList<Object> edges = getEdges(facade);
		for (Object edge : edges) {
			List<Object> points = getPoints(facade, edge);
			if (points.size() != 2) {
				continue;
			}
			Object sourceCell = facade.getSource(edge);
			Object targetCell = facade.getTarget(edge);
			Object sourcePort = facade.getSourcePort(edge);
			Object targetPort = facade.getTargetPort(edge);
			Object[] between = facade.getEdgesBetween(sourcePort, targetPort,
					false);
			if ((between.length == 1) && !(sourcePort == targetPort)) {
				continue;
			}
			Rectangle2D sCP = facade.getBounds(sourceCell);
			Rectangle2D tCP = facade.getBounds(targetCell);
			Point2D sPP = GraphConstants.getOffset(((ProMGraphPort) sourcePort)
					.getAttributes());

			if (sPP == null) {
				sPP = new Point2D.Double(sCP.getCenterX(), sCP.getCenterY());
			}
			Point2D tPP = GraphConstants.getOffset(((ProMGraphPort) targetPort)
					.getAttributes());
			// facade.getBounds(sourcePort);

			if (tPP == null) {
				tPP = new Point2D.Double(tCP.getCenterX(), tCP.getCenterY());
			}

			if (sourcePort == targetPort) {
				assert (sPP.equals(tPP));
				double x = sPP.getX();
				double y = sPP.getY();
				for (int i = 2; i < between.length + 2; i++) {
					List<Point2D> newPoints = new ArrayList<Point2D>(5);
					newPoints.add(new Point2D.Double(x
							- (spacing + i * spacing), y));
					newPoints.add(new Point2D.Double(x
							- (spacing + i * spacing), y
							- (spacing + i * spacing)));
					newPoints.add(new Point2D.Double(x, y
							- (2 * spacing + i * spacing)));
					newPoints.add(new Point2D.Double(x
							+ (spacing + i * spacing), y
							- (spacing + i * spacing)));
					newPoints.add(new Point2D.Double(x + (spacing), y
							- (spacing / 2 + i * spacing)));
					facade.setPoints(between[i - 2], newPoints);
				}

				continue;
			}

			double dx = (sPP.getX()) - (tPP.getX());
			double dy = (sPP.getY()) - (tPP.getY());
			double mx = (tPP.getX()) + dx / 2.0;
			double my = (tPP.getY()) + dy / 2.0;
			double slope = Math.sqrt(dx * dx + dy * dy);
			for (int i = 0; i < between.length; i++) {
				List<Point2D> newPoints = new ArrayList<Point2D>(3);
				double pos = 2 * i - (between.length - 1);
				if (facade.getSourcePort(between[i]) == sourcePort) {
					newPoints.add(sPP);
					newPoints.add(tPP);
				} else {
					newPoints.add(tPP);
					newPoints.add(sPP);
				}
				if (pos != 0) {
					pos = pos / 2;
					double x = mx + pos * spacing * dy / slope;
					double y = my - pos * spacing * dx / slope;
					newPoints.add(1, new SerializablePoint2D.Double(x, y));
				}
				facade.setPoints(between[i], newPoints);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private ArrayList<Object> getEdges(JGraphFacade facade) {
		return new ArrayList<Object>(facade.getEdges());
	}

	@SuppressWarnings("unchecked")
	private List<Object> getPoints(JGraphFacade facade, Object edge) {
		return facade.getPoints(edge);
	}

	private Collection<String> isWellFormed(BPMNDiagram bpmn) {
		Collection<String> maperror = new Vector<String>();
		// Elementi BPMN che non possono essere mappati:
		// Event-Start End != NONE
		// IntermediateCompensation-Event
		// EndCompensation-Event
		// EndLink-Event
		// StartLink-Event

		GatewayWellFormed(bpmn, maperror);
		EventWellFormed(bpmn, maperror);

		ActivityWellFormed(bpmn, maperror);

		// every object is on a path from a start event or an exception event to
		// an end event
		pathFromStartToEnd(bpmn, maperror);
		// if Q is a set of well-formed core BPMN processes and the relation HR
		// is a Direct Acyclic Graph, HR* is a connected graph
		// Controlliamo che nn ci siano sotto processi che vengono invocati da
		// più attività o che un sotto processo figlio invochi un
		// sottoprocesso padre
		acyclicSubProcess(bpmn, maperror);

		// Nessuna SequenceFlow deve essere collegata allo stesso elemento
		for (Flow g : bpmn.getFlows()) {
			BPMNNode s = g.getSource();
			BPMNNode t = g.getTarget();
			if (s.equals(t)) {
				maperror.add(s.getLabel()
						+ "SequenceFlow connessa allo stesso elemento+flow_s->t");
			}
		}

		return maperror;

	}

	private void acyclicSubProcess(BPMNDiagram bpmn, Collection<String> maperror) {
		Collection<SubProcess> subs = bpmn.getSubProcesses();
		for (SubProcess s : subs) {
			System.out.print(s);
			isacyclicsubprocess(s, maperror);
		}

	}

	private void isacyclicsubprocess(SubProcess s, Collection<String> maperror) {
		/*
		 * if(){
		 * 
		 * }
		 */

	}

	private void pathFromStartToEnd(BPMNDiagram bpmn,
			Collection<String> maperror) {
		// every object is on a path from a start event or an exception event to
		// an end event
		for (BPMNNode a : bpmn.getNodes()) {
			List<BPMNNode> listnodeatttoend = new LinkedList<BPMNNode>();
			List<BPMNNode> listnodeatttostart = new LinkedList<BPMNNode>();
			if (!(a instanceof Swimlane)) {
				pathFromNodeToEnd(a, maperror, a, listnodeatttoend);
				pathFromNodeToStart(a, maperror, a, listnodeatttostart);
			}

		}

	}

	private void pathFromNodeToStart(BPMNNode a, Collection<String> maperror,
			BPMNNode c, List<BPMNNode> listnodeatttostart) {
		if (!listnodeatttostart.contains(a)) {
			Collection<BPMNEdge<? extends BPMNNode, ? extends BPMNNode>> edges = a
					.getGraph().getInEdges(a);
			if (!edges.isEmpty()) {
				for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> edge : edges) {
					BPMNNode b = edge.getSource();
					if (b instanceof Event) {
						Event isend = (Event) b;
						if (isend.getEventTrigger() == null) {
							isend.setEventTrigger(EventTrigger.NONE);
						}
						if ((isend.getEventType() == EventType.START && isend
								.getEventTrigger() == EventTrigger.NONE)
								|| (isend.getEventType() == EventType.INTERMEDIATE && isend
										.getEventTrigger() == EventTrigger.ERROR)) {
							listnodeatttostart.add(a);
							break;
						}
					}
					if (b == null) {
						maperror.add("The path of element [[" + c.getLabel()
								+ "]] don't contain start event element");
					}
					listnodeatttostart.add(a);
					pathFromNodeToStart(b, maperror, c, listnodeatttostart); // /vero??
				}

			} else {
				if (a instanceof Event) {
					Event isstart = (Event) a;
					if (isstart.getEventTrigger() == null) {
						isstart.setEventTrigger(EventTrigger.NONE);
					}
					if (isstart.getEventType() != EventType.START
							&& isstart.getEventTrigger() != EventTrigger.NONE) {
						maperror.add("The path of element [[" + c.getLabel()
								+ "]] don't contain start event  element");
					}
				} else
					maperror.add("The path of element [[" + c.getLabel()
							+ "]] don't contain start event element");

			}
		}

	}

	private void pathFromNodeToEnd(BPMNNode a, Collection<String> maperror,
			BPMNNode c, List<BPMNNode> listnodeatt) {
		if (!listnodeatt.contains(a)) {
			Collection<BPMNEdge<? extends BPMNNode, ? extends BPMNNode>> edges = a
					.getGraph().getOutEdges(a);
			if (!edges.isEmpty()) {
				for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> edge : edges) {
					BPMNNode b = edge.getTarget();
					if (b instanceof Event) {
						Event isend = (Event) b;
						if (isend.getEventTrigger() == null) {
							isend.setEventTrigger(EventTrigger.NONE);
						}
						if (isend.getEventType() == EventType.END
								&& isend.getEventTrigger() == EventTrigger.NONE) {
							listnodeatt.add(a);
							break;
						}
					}
					if (b == null) {
						maperror.add("The path of element [[" + c.getLabel()
								+ "]] don't contain end event element");
					}
					listnodeatt.add(a);
					pathFromNodeToEnd(b, maperror, c, listnodeatt);
				}
			} else {
				if (a instanceof Event) {
					Event isend = (Event) a;
					if (isend.getEventTrigger() == null) {
						isend.setEventTrigger(EventTrigger.NONE);
					}
					if (isend.getEventType() != EventType.END
							&& isend.getEventTrigger() != EventTrigger.NONE) {
						maperror.add("The path of element [[" + c.getLabel()
								+ "]] don't contain end event element");
					}
				} else
					maperror.add("The path of element [[" + c.getLabel()
							+ "]] don't contain end event element");

			}
		}

	}

	private void ActivityWellFormed(BPMNDiagram bpmn,
			Collection<String> maperror) {

		for (Activity c : bpmn.getActivities()) {

			if (c.isBCompensation() || c.isBMultiinstance()) {
				maperror.add("Activity not valid [[" + c.getLabel() + "]]");
			}
			// activities events have an in-degree of one and an out-degree of
			// one
			if (c.getGraph().getInEdges(c).size() != 1
					|| c.getGraph().getOutEdges(c).size() != 1) {
				maperror.add("Activities events don't have an in-degree of one and an out-degree of one [["
						+ c.getLabel() + "]]");
			}
			if (c.getLabel().isEmpty()) {
				maperror.add("Activities without label [[" + c.getLabel()
						+ "]]");
			}
		}
	}

	private void GatewayWellFormed(BPMNDiagram bpmn, Collection<String> maperror) {
		for (Gateway g : bpmn.getGateways()) {
			// DATABASED, EVENTBASED, INCLUSIVE, COMPLEX, PARALLEL
			GatewayType gtype = g.getGatewayType();

			switch (gtype) {
			case INCLUSIVE:
				maperror.add("Gateway not valid [[" + g.getLabel() + "]]");
				break;
			case COMPLEX:
				maperror.add("Gateway not valid [[" + g.getLabel() + "]]");
				break;
			}
			if (g.getLabel().isEmpty()) {
				maperror.add("Gateway without [[ " + g.getLabel() + "]]");
			}

			// fork or decision gateways have an in-degree of one and an
			// out-degree of more than one,
			// join or merge gateways have an out-degree of one and an in-degree
			// of more than one
			if (gtype.equals(GatewayType.DATABASED)
					|| gtype.equals(GatewayType.PARALLEL)) {
				if (!(g.getGraph().getInEdges(g).size() == 1 && g.getGraph()
						.getOutEdges(g).size() > 1)) {

					if (!(g.getGraph().getInEdges(g).size() > 1 && g.getGraph()
							.getOutEdges(g).size() == 1)) {

						maperror.add(" Gateway don't have an out-degree of one and an in-degree of more than one [["
								+ g.getLabel() + "]]");

					}
				}

			}
		}
	}

	private void EventWellFormed(BPMNDiagram bpmn, Collection<String> maperror) {
		for (Event e : bpmn.getEvents()) {
			// START, INTERMEDIATE, END;
			EventType type = e.getEventType();
			// MESSAGE, TIMER, ERROR, CANCEL, COMPENSATION, CONDITIONAL, LINK,
			// SIGNAL, TERMINATE, MULTIPLE
			EventTrigger trigger = e.getEventTrigger();

			if (trigger == null)
				trigger = EventTrigger.NONE;
			switch (trigger) {
			case COMPENSATION:
				maperror.add("Event not valid [[" + e.getLabel() + "]]");
				break;
			case LINK:
				maperror.add("Event not valid [[" + e.getLabel() + "]]");
				break;
			case CONDITIONAL:
				maperror.add("Event not valid [[" + e.getLabel() + "]]");
				break;
			case SIGNAL:
				maperror.add("Event not valid [[" + e.getLabel() + "]]");
				break;
			case MESSAGE:
				maperror.add("Event not valid [[" + e.getLabel() + "]]");
				break;

			}

			if (e.getLabel().isEmpty()) {
				maperror.add("Event without label [[" + e.getLabel() + "]]");
			}
			// se trovo start o end che non sono di tipo NONE
			if (type.equals(EventType.START) || type.equals(EventType.END)) {
				if (!trigger.equals(EventTrigger.NONE)) {
					maperror.add("Event not valid [[" + e.getLabel() + "]]");
				}
			}

			// start events and exception events have an in-degree of zero and
			// an out-degree of one
			if (type.equals(EventType.START)
					&& (trigger.equals(EventTrigger.ERROR) || trigger
							.equals(EventTrigger.NONE))) {
				if (e.getGraph().getInEdges(e).size() != 0) {
					maperror.add("start events and exception events don't have an in-degree of zero [["
							+ e.getLabel() + "]]");
				}
				if (e.getGraph().getOutEdges(e).size() != 1) {
					maperror.add("start events and exception events don't have an out-degree of one [["
							+ e.getLabel() + "]]");
				}
			}

			// end events have an out-degree of zero and an in-degree of one
			if (type.equals(EventType.END) && trigger.equals(EventTrigger.NONE)) {
				if (e.getGraph().getInEdges(e).size() != 1) {
					maperror.add("end events don't have  an in-degree of one [["
							+ e.getLabel() + "]]");
				}
				if (e.getGraph().getOutEdges(e).size() != 0) {
					maperror.add("end events don't  have an out-degree of zero [["
							+ e.getLabel() + "]]");
				}
			}
			// non-exception intermediate events have an in-degree of one and an
			// out-degree of one
			if (type.equals(EventType.INTERMEDIATE)
					&& !trigger.equals(EventTrigger.ERROR)) {
				if (e.getGraph().getInEdges(e).size() != 1
						&& e.getGraph().getOutEdges(e).size() != 1) {
					maperror.add("non-exception intermediate events don't have an in-degree of one and an out-degree of one [["
							+ e.getLabel() + "]]");
				}
			}

		}
	}

}
