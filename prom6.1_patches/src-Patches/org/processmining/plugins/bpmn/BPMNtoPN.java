package org.processmining.plugins.bpmn;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.deckfour.xes.model.XLog;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.AttributeMap.SerializablePoint2D;
import org.processmining.contexts.uitopia.annotations.UIImportPlugin;
import org.processmining.framework.abstractplugins.AbstractImportPlugin;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.NodeID;
import org.processmining.models.graphbased.directed.AbstractDirectedGraphEdge;
import org.processmining.models.graphbased.directed.AbstractDirectedGraphNode;
import org.processmining.models.graphbased.directed.DirectedGraph;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramFactory;
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
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.ExpandableSubNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;
import org.processmining.models.jgraph.ProMJGraph;
import org.processmining.models.jgraph.ProMJGraphVisualizer;
import org.processmining.models.jgraph.elements.ProMGraphPort;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.replay.ReplayAction;
import org.processmining.plugins.petrinet.replayfitness.ReplayFitnessSetting;
import org.processmining.plugins.xpdl.Xpdl;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.hierarchical.JGraphHierarchicalLayout;

@Plugin(name = "Import BPMN model from XPDL 2.1 file to PetriNet", parameterLabels = { "Filename" }, returnLabels = {
		"Petri Net", "Marking",  "BPMNDiagram", "Traslate Result","xpdl","ReplayFitnessSetting" }, returnTypes = { Petrinet.class, Marking.class, BPMNDiagram.class,TraslateBPMNResult.class,Xpdl.class,ReplayFitnessSetting.class })
		@UIImportPlugin(description = "XPDL 2.1 files to PN", extensions = { "xpdl" })
		public class BPMNtoPN extends AbstractImportPlugin {

	private ExpandableSubNet subNet = null;

	protected FileFilter getFileFilter() {
		return new FileNameExtensionFilter("XPDL 2.1 files", "xpdl");
	}

	protected Object importFromStream(PluginContext context, InputStream input,
			String filename, long fileSizeInBytes) throws Exception {
		Xpdl xpdl = importXpdlFromStream(context, input, filename,
				fileSizeInBytes);
		if (xpdl == null) {
			/*
			 * No PNML found in file. Fail.
			 */
			return null;
		}
		/*
		 * XPDL file has been imported. Now we need to convert the contents to a
		 * BPMN diagram.
		 */
		BPMNDiagram bpmn = BPMNDiagramFactory.newBPMNDiagram(filename);
		Map<String, BPMNNode> id2node = new HashMap<String, BPMNNode>();

		/*
		 * Initialize the BPMN diagram from the XPDL element.
		 */
		xpdl.convertToBpmn(bpmn, id2node );

		/*
		 * Set the label of the BPMN diagram.
		 */
		context.getFutureResult(0).setLabel(filename);

		Collection<String> error = this.isWellFormed(bpmn);



		return this.BPMN2Translate(context,bpmn,xpdl,id2node,error);
	}

	private void translateTask(BPMNDiagram bpmn, Map<String, Place> placeMap,
			PetrinetGraph net) {

		for (Activity c : bpmn.getActivities()) {
			String id = c.getLabel();

			Transition t = net.addTransition(id + "+start", this.subNet);
			Place p = net.addPlace(id, this.subNet);
			Arc a = net.addArc(t, p, 1, this.subNet);
			Transition t1 = net.addTransition(id + "+complete", this.subNet);
			Arc a1 = net.addArc(p, t1, 1, this.subNet);

			for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : c
					.getGraph().getInEdges(c)) {
				String source = s.getSource().getLabel();
				String target = s.getTarget().getLabel();

				Place pst = placeMap.get(source + target);

				Arc st = net.addArc(pst, t, 1, this.subNet);

			}
			for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : c
					.getGraph().getOutEdges(c)) {
				if(s instanceof Flow){
					String source = s.getSource().getLabel();
					String target = s.getTarget().getLabel();

					Place pst = placeMap.get(source + target);

					Arc st = net.addArc(t1, pst, 1, this.subNet);
				}
			}

		}

	}

	private void translateGateway(BPMNDiagram bpmn,	Map<String, Place> placeMap, PetrinetGraph net) {
		for (Gateway g : bpmn.getGateways()) {
			//gateway data-based
			if (g.getGatewayType().equals(GatewayType.DATABASED)) {
				int i = 0;
				Map<String, Transition> tranMap = new HashMap<String, Transition>();
				//gateway data-based if branch 
				if (g.getGraph().getOutEdges(g).size()>1 && g.getGraph().getInEdges(g).size()==1 ){
					for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g.getGraph().getOutEdges(g)) {
						String source = s.getSource().getLabel();
						String target = s.getTarget().getLabel();

						Transition t = net.addTransition(g.getLabel() + "_" + i++,
								this.subNet);
						t.setInvisible(true);
						tranMap.put(target + source, t);

						Place pst = placeMap.get(source + target);

						net.addArc(t, pst, 1, this.subNet);

					}
					for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g.getGraph().getInEdges(g)) {
						String source = s.getSource().getLabel();
						String target = s.getTarget().getLabel();

						Place pst = placeMap.get(source + target);

						for (Transition t : tranMap.values()) {

							net.addArc(pst, t, 1, this.subNet);

						}
					}
				}else{
					//gateway merge
					if (g.getGraph().getOutEdges(g).size()==1 && g.getGraph().getInEdges(g).size()>1 ){
						String out = g.getGraph().getOutEdges(g).iterator().next().getSource().getLabel();
						String in  = g.getGraph().getOutEdges(g).iterator().next().getTarget().getLabel();
						Place ps = placeMap.get(out + in);
						i=0;
						for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g.getGraph().getInEdges(g)){
							String source = s.getSource().getLabel();
							String target = s.getTarget().getLabel();


							Place pst = placeMap.get(source + target);

							Transition t = net.addTransition(g.getLabel() + "_" + i++,this.subNet );
							t.setInvisible(true);
							net.addArc( pst,t, 1, this.subNet);

							net.addArc(t, ps, this.subNet);

						}
					}
				}

			}else{
				if (g.getGatewayType().equals(GatewayType.PARALLEL)) {
					//gateway parallel fork 
					if (g.getGraph().getOutEdges(g).size()>1 && g.getGraph().getInEdges(g).size()==1 ){
						String so = g.getGraph().getInEdges(g).iterator().next().getSource().getLabel();
						String ta  = g.getGraph().getInEdges(g).iterator().next().getTarget().getLabel();
						Place ps = placeMap.get(so+ta);
						Transition t = net.addTransition(g.getLabel() + "_fork",this.subNet );
						t.setInvisible(true);
						net.addArc( ps,t, 1, this.subNet);
						for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g.getGraph().getOutEdges(g)){
							String source = s.getSource().getLabel();
							String target = s.getTarget().getLabel();


							Place pst = placeMap.get(source + target);
							net.addArc(t, pst, 1, this.subNet);

						}


					}else{
						//gateway parallel Join 
						if (g.getGraph().getOutEdges(g).size()==1 && g.getGraph().getInEdges(g).size()>1 ){
							String so = g.getGraph().getOutEdges(g).iterator().next().getSource().getLabel();
							String ta  = g.getGraph().getOutEdges(g).iterator().next().getTarget().getLabel();
							Place ps = placeMap.get(so+ta);
							Transition t = net.addTransition(g.getLabel() + "_join",this.subNet );
							t.setInvisible(true);
							net.addArc( t,ps, 1, this.subNet);
							for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g.getGraph().getInEdges(g)){
								String source = s.getSource().getLabel();
								String target = s.getTarget().getLabel();


								Place pst = placeMap.get(source + target);
								net.addArc(pst,t, 1, this.subNet);

							}


						}
					}
				}else{
					//gateway event-based
					if (g.getGatewayType().equals(GatewayType.EVENTBASED)) {
						//Exclusive event gateway 
						if (g.getGraph().getOutEdges(g).size()>1 && g.getGraph().getInEdges(g).size()==1 ){
							String so = g.getGraph().getInEdges(g).iterator().next().getSource().getLabel();
							String ta  = g.getGraph().getInEdges(g).iterator().next().getTarget().getLabel();
							Place ps = placeMap.get(so+ta);
							int i=0;
							for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : g.getGraph().getOutEdges(g)){
								String source = s.getSource().getLabel();
								String target = s.getTarget().getLabel();


								Place pst = placeMap.get(source + target);

								Transition t = net.addTransition(g.getLabel() + "_" + i++,this.subNet );
								t.setInvisible(true);
								net.addArc( t,pst, 1, this.subNet);

								net.addArc( ps,t , this.subNet);

							}


						}


					}
				}
			}
		}
	}

	private void translateEvent(BPMNDiagram bpmn,	Map<String, Place> placeMap, PetrinetGraph net, Marking marking){
		for (Event e : bpmn.getEvents()) {
			if (e.getEventType().equals(EventType.START) && e.getEventTrigger().equals(EventTrigger.NONE)) {

				// Place p = new Place(e.getLabel(), net);
				Place p = net.addPlace("p"+e.getLabel(), this.subNet);

				Transition t = net.addTransition("t_"+e.getLabel(), this.subNet);
				t.setInvisible(true);
				Arc a = net.addArc(p, t, 1, this.subNet);
				marking.add(p, 1);

				for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : e.getGraph().getOutEdges(e)) {
					String source = s.getSource().getLabel();
					String target = s.getTarget().getLabel();


					Place pst = placeMap.get(source + target);

					net.addArc(t, pst, 1, this.subNet);

				}


			}
			if (e.getEventType().equals(EventType.END) && e.getEventTrigger().equals(EventTrigger.NONE)) {


				Place p = net.addPlace("p"+e.getLabel(), this.subNet);

				Transition t = net.addTransition("t_"+e.getLabel(), this.subNet);

				t.setInvisible(true);
				Arc a = net.addArc(t, p, 1, this.subNet);

				for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> s : e.getGraph().getInEdges(e)) {
					String source = s.getSource().getLabel();
					String target = s.getTarget().getLabel();


					Place pst = placeMap.get(source + target);

					net.addArc( pst, t, 1, this.subNet);

				}

			}
			if (e.getEventType().equals(EventType.INTERMEDIATE) && !e.getEventTrigger().equals(EventTrigger.NONE)) {



				Transition t = net.addTransition(e.getLabel(), this.subNet);			



				String g = e.getGraph().getInEdges(e).iterator().next().getSource().getLabel();
				String s  = e.getGraph().getInEdges(e).iterator().next().getTarget().getLabel();
				Place ps_pre = placeMap.get(g+s);


				g  = e.getGraph().getOutEdges(e).iterator().next().getSource().getLabel();
				s  = e.getGraph().getOutEdges(e).iterator().next().getTarget().getLabel();
				Place ps_post = placeMap.get(g+s);

				net.addArc(ps_pre,t, 1, this.subNet);
				net.addArc(t,ps_post, 1, this.subNet);



			}

		}


	}
	/**
	 * @param bpmn
	 * @return
	 */
	private Object BPMN2Translate(PluginContext c ,BPMNDiagram bpmn, Xpdl xpdl, Map<String, BPMNNode> id2node, Collection<String> error) {
		Map<String, Place> placeMap = new HashMap<String, Place>();


		PetrinetGraph net = PetrinetFactory.newPetrinet(bpmn.getLabel());
		Marking marking = new Marking();


		//gli argchi del diagramma BPMN diventano piazze della rete BPMN
		for (Flow g : bpmn.getFlows()) {
			String f = g.getSource().getLabel();
			String z = g.getTarget().getLabel();

			Place p = net.addPlace(f + z, this.subNet);
			placeMap.put(f + z, p);


		}

		translateTask(bpmn, placeMap, net);

		translateGateway(bpmn, placeMap, net);

		translateEvent(bpmn, placeMap, net, marking);

		layoutcreate(c,net);

		TraslateBPMNResult result = new TraslateBPMNResult(bpmn, (Petrinet) net, marking, placeMap,xpdl,id2node,error);
		Object[] objects = new Object[6];
		objects[0] = net;
		objects[1] = marking;
		objects[2] = bpmn;
		objects[3] = result;
		objects[4] = xpdl;

		c.addConnection(new InitialMarkingConnection(net, marking));


		ReplayFitnessSetting settings = new ReplayFitnessSetting();
		System.out.println("Settings: " + settings);
		settings.setAction(ReplayAction.INSERT_ENABLED_MATCH, true);
		settings.setAction(ReplayAction.INSERT_ENABLED_INVISIBLE, true);
		settings.setAction(ReplayAction.REMOVE_HEAD, false);
		settings.setAction(ReplayAction.INSERT_ENABLED_MISMATCH, false);
		settings.setAction(ReplayAction.INSERT_DISABLED_MATCH, true);
		settings.setAction(ReplayAction.INSERT_DISABLED_MISMATCH, false);
		objects[5] = settings;
		return objects;
		//return result;

	}

	private void layoutcreate(PluginContext c, PetrinetGraph net){

		GraphLayoutConnection layout = new GraphLayoutConnection(net);
		try {
			layout = c.getConnectionManager().getFirstConnection(GraphLayoutConnection.class, c, net);
		} catch (ConnectionCannotBeObtained e) {
			// TODO Auto-generated catch block
			/*
			 * Get a jgraph for this graph.
			 */

			ProMJGraph jgraph = ProMJGraphVisualizer.instance().visualizeGraph(c, net).getGraph();
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
		layout.setOrientation(graph.getAttributeMap().get(AttributeMap.PREF_ORIENTATION, SwingConstants.SOUTH));

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
			Object[] between = facade.getEdgesBetween(sourcePort, targetPort, false);
			if ((between.length == 1) && !(sourcePort == targetPort)) {
				continue;
			}
			Rectangle2D sCP = facade.getBounds(sourceCell);
			Rectangle2D tCP = facade.getBounds(targetCell);
			Point2D sPP = GraphConstants.getOffset(((ProMGraphPort) sourcePort).getAttributes());

			if (sPP == null) {
				sPP = new Point2D.Double(sCP.getCenterX(), sCP.getCenterY());
			}
			Point2D tPP = GraphConstants.getOffset(((ProMGraphPort) targetPort).getAttributes());
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
					newPoints.add(new Point2D.Double(x - (spacing + i * spacing), y));
					newPoints.add(new Point2D.Double(x - (spacing + i * spacing), y - (spacing + i * spacing)));
					newPoints.add(new Point2D.Double(x, y - (2 * spacing + i * spacing)));
					newPoints.add(new Point2D.Double(x + (spacing + i * spacing), y - (spacing + i * spacing)));
					newPoints.add(new Point2D.Double(x + (spacing), y - (spacing / 2 + i * spacing)));
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

	private Collection<String> isWellFormed(BPMNDiagram bpmn){
		Collection<String> maperror = new Vector<String>();
		//  Elementi BPMN che non possono essere mappati:
		//  Event-Start  End != NONE
		//  IntermediateCompensation-Event 
		//  EndCompensation-Event 
		//  EndLink-Event 
		//  StartLink-Event 

		GatewayWellFormed(bpmn,maperror);
		EventWellFormed(bpmn,maperror);

		ActivityWellFormed(bpmn,maperror);

		//every object is on a path from a start event or an exception event to an end event
		pathFromStartToEnd(bpmn,maperror);
		//if Q is a set of well-formed core BPMN processes and the relation HR is a Direct Acyclic Graph, HR* is a connected graph
		//Controlliamo che nn ci siano sotto processi che vengono invocati da più attività o che un sotto processo figlio invochi un sottoprocesso padre
		acyclicSubProcess(bpmn,maperror);
		

		// Nessuna SequenceFlow deve essere collegata allo stesso elemento
		for(Flow g : bpmn.getFlows()) {
			BPMNNode s = g.getSource();
			BPMNNode t = g.getTarget();
			if(s.equals(t)){
				maperror.add(s.getLabel()+"SequenceFlow connessa allo stesso elemento+flow_s->t");
			}
		}



		return maperror;



	}

	private void acyclicSubProcess(BPMNDiagram bpmn, Collection<String> maperror) {
		Collection<SubProcess> subs = bpmn.getSubProcesses();
		for(SubProcess s : subs){
			System.out.print(s);
			isacyclicsubprocess( s , maperror);
		}
		
	}

	private void isacyclicsubprocess(SubProcess s, Collection<String> maperror) {
		/*if(){
			
		}*/
		
	}

	private void pathFromStartToEnd(BPMNDiagram bpmn,
			Collection<String> maperror) {
		//every object is on a path from a start event or an exception event to an end event
		for(BPMNNode a : bpmn.getNodes()){
			pathFromNodeToEnd(a,maperror,a);
			pathFromNodeToStart(a,maperror,a);
			
		}

	}

	private void pathFromNodeToStart(BPMNNode a, Collection<String> maperror,
			BPMNNode c) {
		Collection<BPMNEdge<? extends BPMNNode, ? extends BPMNNode>> edges = a.getGraph().getInEdges(a);
		if(!edges.isEmpty()){
			for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> edge : edges){
				BPMNNode b = edge.getSource();
				if(b instanceof Event){
					Event isend = (Event) b;
					if((isend.getEventType()==EventType.START && isend.getEventTrigger()==EventTrigger.NONE)||(isend.getEventType()==EventType.INTERMEDIATE&& isend.getEventTrigger()==EventTrigger.ERROR)){
						break;
					}
				}
				if(b==null){
					maperror.add("il path dell'elemento inizia con start "+c.getLabel());
				}
				pathFromNodeToEnd(b,maperror, c);
			}
			
		}else{
			if(a instanceof Event){
				Event isstart = (Event) a;
				if(isstart.getEventType()!=EventType.START && isstart.getEventTrigger()!=EventTrigger.NONE){
					maperror.add(c.getLabel()+" il path dell'elemento inizia con start ");
				}
			}else maperror.add(c.getLabel()+"il path dell'elemento inizia con start ");
			
		}	
		
	}

	private void pathFromNodeToEnd(BPMNNode a, Collection<String> maperror, BPMNNode c) {
		Collection<BPMNEdge<? extends BPMNNode, ? extends BPMNNode>> edges = a.getGraph().getOutEdges(a);
		if(!edges.isEmpty()){
			for (BPMNEdge<? extends BPMNNode, ? extends BPMNNode> edge : edges){
				BPMNNode b = edge.getTarget();
				if(b instanceof Event){
					Event isend = (Event) b;
					if(isend.getEventType()==EventType.END && isend.getEventTrigger()==EventTrigger.NONE){
						break;
					}
				}
				if(b==null){
					maperror.add("il path dell'elemento nn termina con end "+c.getLabel());
				}
				pathFromNodeToEnd(b,maperror, c);
			}
		}else{
			if(a instanceof Event){
				Event isend = (Event) a;
				if(isend.getEventType()!=EventType.END && isend.getEventTrigger()!=EventTrigger.NONE){
					maperror.add("il path dell'elemento nn termina con end "+c.getLabel());
				}
			}else maperror.add("il path dell'elemento nn termina con end "+c.getLabel());
			
		}		

	}

	private void ActivityWellFormed(BPMNDiagram bpmn, Collection<String> maperror) {

		for (Activity c : bpmn.getActivities()) {

			if(c.isBCompensation() || c.isBMultiinstance()){
				maperror.add("Attività non valida "+c.getLabel());
			}
			// activities  events have an in-degree of one and an out-degree of one
			if (c.getGraph().getInEdges(c).size()!=1 || c.getGraph().getOutEdges(c).size()!=1){
				maperror.add("Attività non valida troppi archi o troppo pochi "+c.getLabel());
			}
			if(c.getLabel().isEmpty()){
				maperror.add("manca il nome dell'attività "+c.getLabel()); 
			}
		}
	}

	private void GatewayWellFormed(BPMNDiagram bpmn,Collection<String> maperror ){
		for (Gateway g : bpmn.getGateways()) {
			//DATABASED, EVENTBASED, INCLUSIVE, COMPLEX, PARALLEL
			GatewayType gtype = g.getGatewayType();

			switch (gtype) {
			case  INCLUSIVE :   maperror.add("Gateway non valido "+g.getLabel());    break;
			case COMPLEX : maperror.add("Gateway non valido "+g.getLabel()); break;
			}
			if(g.getLabel().isEmpty()){
				maperror.add("manca il nome del gateway "+g.getLabel()); 
			}

			//fork or decision gateways have an in-degree of one and an out-degree of more than one,
			//join or merge gateways have an out-degree of one and an in-degree of more than one
			if(gtype.equals(GatewayType.DATABASED) || gtype.equals(GatewayType.PARALLEL)){
				if(!(g.getGraph().getInEdges(g).size()==1 && g.getGraph().getOutEdges(g).size()>1 )){

					if(!(g.getGraph().getInEdges(g).size()>1 && g.getGraph().getOutEdges(g).size()==1 )){
						
						maperror.add(" Gateway non valido troppi archi in entrata o in uscita "+g.getLabel()); 
						
					}
				}

			}
		}
	}

	private void EventWellFormed(BPMNDiagram bpmn,Collection<String> maperror ){
		for (Event e : bpmn.getEvents()){
			//START, INTERMEDIATE, END;
			EventType type = e.getEventType();
			//MESSAGE, TIMER, ERROR, CANCEL, COMPENSATION, CONDITIONAL, LINK, SIGNAL, TERMINATE, MULTIPLE
			EventTrigger trigger = e.getEventTrigger();

			if(trigger==null)
				trigger=EventTrigger.NONE;
			switch (trigger) {
			case COMPENSATION :   maperror.add("Evento non valido "+e.getLabel());    break;
			case LINK : maperror.add("Evento non valido "+e.getLabel()); break;
			case CONDITIONAL : maperror.add("Evento non valido "+e.getLabel()); break;
			case SIGNAL : maperror.add("Evento non valido "+e.getLabel()); break;


			}

			if(e.getLabel().isEmpty()){
				maperror.add("manca il nome dell'evento "+e.getLabel()); 
			}
			//se trovo start o end che non sono di tipo NONE
			if(type.equals(EventType.START) || type.equals(EventType.END)){
				if (!trigger.equals(EventTrigger.NONE)){
					maperror.add("Evento non valido "+e.getLabel());
				}
			}

			// start events and exception events have an in-degree of zero and an out-degree of one
			if(type.equals(EventType.START) && ( trigger.equals(EventTrigger.ERROR) || trigger.equals(EventTrigger.NONE) )){
				if(e.getGraph().getInEdges(e).size()!=0){
					maperror.add("Errore Evento start con ramo in entrata "+e.getLabel());
				}
				if(e.getGraph().getOutEdges(e).size()!=1){
					maperror.add("Errore Evento start senza o con troppi rami in uscita "+e.getLabel());
				}
			}

			//end events have an out-degree of zero and an in-degree of one
			if(type.equals(EventType.END) &&  trigger.equals(EventTrigger.NONE)){
				if(e.getGraph().getInEdges(e).size()!=1){
					maperror.add("Errore Evento end senza ramo o con troppi rami in entrata "+e.getLabel());
				}
				if(e.getGraph().getOutEdges(e).size()!=0){
					maperror.add("Errore Evento end con ramo in uscita "+e.getLabel());
				}
			}
			//  non-exception intermediate events have an in-degree of one and an out-degree of one
			if(type.equals(EventType.INTERMEDIATE) &&  !trigger.equals(EventTrigger.ERROR)){
				if(e.getGraph().getInEdges(e).size()!=1 && e.getGraph().getOutEdges(e).size()!=1){
					maperror.add("Evento  con piu di un arco "+e.getLabel());
				}
			}

		}
	}

	private Xpdl importXpdlFromStream(PluginContext context, InputStream input,
			String filename, long fileSizeInBytes) throws Exception {
		/*
		 * Get an XML pull parser.
		 */
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();
		/*
		 * Initialize the parser on the provided input.
		 */
		xpp.setInput(input, null);
		/*
		 * Get the first event type.
		 */
		int eventType = xpp.getEventType();
		/*
		 * Create a fresh XPDL object.
		 */
		Xpdl xpdl = new Xpdl();

		/*
		 * Skip whatever we find until we've found a start tag.
		 */
		while (eventType != XmlPullParser.START_TAG) {
			eventType = xpp.next();
		}
		/*
		 * Check whether start tag corresponds to XPDL start tag.
		 */
		if (xpp.getName().equals(xpdl.tag)) {
			/*
			 * Yes it does. Import the XPDL element.
			 */
			xpdl.importElement(xpp, xpdl);
		} else {
			/*
			 * No it does not. Return null to signal failure.
			 */
			xpdl.log(xpdl.tag, xpp.getLineNumber(), "Expected " + xpdl.tag
					+ ", got " + xpp.getName());
		}
		if (xpdl.hasErrors()) {
			context.getProvidedObjectManager().createProvidedObject(
					"Log of XPDL import", xpdl.getLog(), XLog.class, context);
			return null;
		}
		return xpdl;
	}
}
