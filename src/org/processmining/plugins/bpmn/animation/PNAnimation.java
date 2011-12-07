package org.processmining.plugins.bpmn.animation;


import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.models.animation.Animation;
import org.processmining.models.animation.AnimationLog;
import org.processmining.models.animation.EdgeAnimation;
import org.processmining.models.animation.NodeAnimation;
import org.processmining.models.animation.NodeAnimationKeyframe;
import org.processmining.models.animation.TokenAnimation;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.AbstractDirectedGraphEdge;
import org.processmining.models.graphbased.directed.AbstractDirectedGraphNode;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.PetrinetSemantics;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;
import org.processmining.plugins.petrinet.replay.ReplayAction;
import org.processmining.plugins.petrinet.replay.Replayer;
import org.processmining.plugins.petrinet.replayfitness.ReplayFitnessCost;
import org.processmining.plugins.petrinet.replayfitness.ReplayFitnessSetting;


public class PNAnimation extends Animation {

	//private Petrinet pn;
	//private Map<XEventClass,Transition> mapevent2transition;
	//private Map<XEventClass,Collection<Arc>> MapArc;
	//private Map<XTrace,Map<XEventClass,Collection<Arc>>> MapMapArc;
	private XEventClasses classes;
	private Marking marking;
	Map<XTrace,Collection <ArcTime>> mapevent2arc= new HashMap<XTrace, Collection<ArcTime>>();

	//final private Map<XTrace,Collection<Transition>> MapTrace2ListTransition;

	public PNAnimation(PluginContext context, Petrinet pn, XLog log,Marking initMarking) {
		super(context, pn, log);
		//this.pn = pn;
		//MapTrace2ListTransition = new HashMap<XTrace, Collection<Transition>>();
		marking = new Marking(initMarking);
		createlisttransitionlogevent(context, log, pn);
	}

	private void createlisttransitionlogevent( PluginContext context,XLog log,Petrinet net){






		/*Marking marking = null;


		try {
			InitialMarkingConnection connection = context.getConnectionManager().getFirstConnection(
					InitialMarkingConnection.class, context, net);
			marking = connection.getObjectWithRole(InitialMarkingConnection.MARKING);
		} catch (ConnectionCannotBeObtained ex) {
			context.log("Petri net lacks initial marking");
			//return null;
		}*/

		ReplayFitnessSetting settings = new ReplayFitnessSetting();
		System.out.println("Settings: " + settings);
		settings.setAction(ReplayAction.INSERT_ENABLED_MATCH, true);
		settings.setAction(ReplayAction.INSERT_ENABLED_INVISIBLE, true);
		settings.setAction(ReplayAction.REMOVE_HEAD, false);
		settings.setAction(ReplayAction.INSERT_ENABLED_MISMATCH, false);
		settings.setAction(ReplayAction.INSERT_DISABLED_MATCH, true);
		settings.setAction(ReplayAction.INSERT_DISABLED_MISMATCH, false);

		classes = getEventClasses(log);
		//Map<XEventClass,Transition> maps = this.getMapping(classes, net);




		PetrinetSemantics semantics = PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class);
		Map<Transition, XEventClass> map = getMappings(classes, net);
		Replayer<ReplayFitnessCost> replayer = new Replayer<ReplayFitnessCost>(context, net, semantics, map, ReplayFitnessCost.addOperator);

		int i=0;
		for (XTrace trace : log) {
			List<XEventClass> list = getList(trace, classes);
			try {
				System.out.println("Replay :"+i++);
				List<Transition> sequence = replayer.replayTrace(marking, list, settings);
				System.out.println("Replayed");
				sequence = sortHiddenTransection(net, sequence, map);
				Collection<ArcTime> collArcTime = new Vector<ArcTime>();
				Map<Place,ArcTime> mapplace2arc = new  HashMap<Place, ArcTime>();
				
				updatemapping(net,marking,sequence,trace,map,collArcTime,mapplace2arc);

				//MapTrace2ListTransition.put(trace,sequence);
				mapevent2arc.put(trace,collArcTime);

			} catch (InterruptedException e) {
				System.out.println("Failed");
				context.log("Replay of trace " + trace + " failed ");
				e.printStackTrace();
			} catch (ExecutionException e) {
				System.out.println("Failed");
				context.log("Replay of trace " + trace + " failed ");
				e.printStackTrace();
			}
			///} catch (Exception ex) {
			//	System.out.println("Failed"+ex.getMessage());
			//	context.log("Replay of trace " + trace + " failed: " + ex.getMessage());

			//}
		}


		//return mapevent2arc;
	}







	private void updatemapping(Petrinet net, Marking initMarking,
			List<Transition> sequence, XTrace trace,
			Map<Transition, XEventClass> map, Collection<ArcTime> collArcTime, Map<Place, ArcTime> mapplace2arc) {

		XAttributeTimestampImpl date  = (XAttributeTimestampImpl)(trace.get(0).getAttributes().get("time:timestamp"));
		long timeStartTrace = date.getValue().getTime();



		Marking newmarking = new Marking(initMarking);

		for (Place p : newmarking.baseSet()){

			mapplace2arc.put(p, new ArcTime(p.getGraph().getOutEdges(p).iterator().next(), timeStartTrace, -1));

		}

		int iTrace = -1;
		for (int iTrans=0; iTrans<sequence.size(); iTrans++) {
			Transition transition = sequence.get(iTrans);

			long timeEventTransition=timeStartTrace;

			if (map.containsKey(transition)) {
				iTrace+=1;
			}

			if(iTrace>=0){
				XEvent event = trace.get(iTrace);
				XAttributeTimestampImpl date1  = (XAttributeTimestampImpl)(event.getAttributes().get("time:timestamp"));
				timeEventTransition = date1.getValue().getTime();
			}
			float deltaTime = timeEventTransition-timeStartTrace;


			UpdateTimeArc(transition,timeEventTransition,timeStartTrace,collArcTime,mapplace2arc,newmarking);

			//UpdateMarking(newmarking,transition);

			timeStartTrace =timeEventTransition;


		}


	}

	private void UpdateTimeArc(Transition trans,
			long timeEventTransition,  long timeStartTrace, Collection<ArcTime> collArcTime, Map<Place, ArcTime> mapplace2arc, Marking newmarking) {
		
		Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edgesin = trans.getGraph().getInEdges(trans);
		for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge: edgesin){

			ArcTime arc = mapplace2arc.remove(edge.getSource());
			if(arc==null){
				arc= new ArcTime(edge, timeEventTransition, timeEventTransition+1);
			}else arc.setEndTime(timeEventTransition);
			arc.setArc(edge);
			newmarking.remove(edge.getSource());

			/*long delta = 100;
			long endtime = timeEventTransition==timeStartTrace ? timeEventTransition+delta : timeEventTransition;
			//ArcTime arc = new ArcTime(edge, timeStartTrace,endtime);*/
			collArcTime.add(arc);

		}
		Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edgesout = trans.getGraph().getOutEdges(trans);
		for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge: edgesout){

			ArcTime arc=null ;
			long delta = 0;
			
			//long endtime = timeEventTransition==timeStartTrace ? timeEventTransition+delta : timeEventTransition;
			 arc = new ArcTime(edge,timeEventTransition ,timeEventTransition+delta);
			 //timeEventTransition+=1;
			collArcTime.add(arc);

		}

		fixparallel(newmarking,mapplace2arc,timeEventTransition );

		edgesout = trans.getGraph().getOutEdges(trans);
		for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge: edgesout){

			Place p = (Place) edge.getTarget();
			newmarking.add(p);
			mapplace2arc.put(p, new ArcTime(edge,timeEventTransition ,-1));

		}
	}

	private void fixparallel(Marking newmarking,
			Map<Place, ArcTime> mapplace2arc, long timeEventTransition) {

		for(Place place : mapplace2arc.keySet()){

			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edgesout = place.getGraph().getOutEdges(place);
			int placeMarking = newmarking.occurrences(place);
			int minMarking = placeMarking;

			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge :edgesout){
				Transition t = (Transition) edge.getTarget();
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge1 : t.getGraph().getInEdges(t)) {
					Arc arc1 = (Arc) edge1;
					Place p1 = (Place)arc1.getSource();
					int tokens = newmarking.occurrences(p1);
					minMarking = Math.min(minMarking, tokens);
				}
			}
			if(minMarking==0){
				mapplace2arc.get(place).setStartTime(timeEventTransition);
			}

		}

	}

	/**
	 * Creates animations for the given trace. Shows progress using the given
	 * progress bar.
	 * 
	 * @param trace
	 *            The given trace.
	 * @param progress
	 *            The given progress bar.
	 * @throws IndexOutOfBoundsException
	 */
	protected void createAnimations(XTrace trace, Progress progress) throws IndexOutOfBoundsException {



		/*
		 * Create a case animation for this trace.
		 */
		// Time of first event in trace.
		long start = getEventTime(trace.get(0)).getTime();
		// Time of last event in trace.
		long end = getEventTime(trace.get(trace.size() - 1)).getTime();
		addTokenAnimation(new TokenAnimation(trace, start, end));

		/*
		 * Create state and transition animations for this trace.
		 */
		// Progress counter.
		int counter = 0;
		// Time of previous event in trace. 
		long prevEventTime = -1;
		long lastEndTime = -1;
		//Collection<Transition> MapTransition = this.MapTrace2ListTransition.get(trace);

		Collection<ArcTime> collarc = this.mapevent2arc.get(trace);

		for(ArcTime a : collarc){
			PetrinetNode Source = a.getArc().getSource();

			addKeyframe(Source, new NodeAnimationKeyframe(trace,a.getStartTime() , false,false ));

			addTokenAnimation(a.getArc(), new TokenAnimation(trace, a.getStartTime(),a.getEndTime() ));

			PetrinetNode target = a.getArc().getTarget();

			addKeyframe(target, new NodeAnimationKeyframe(trace,a.getEndTime() , false,false ));

			updateBoundaries(a.getStartTime());
			//updateBoundaries(a.getEndTime());

			counter++;
			if (counter > 299) {
				progress.setValue(progress.getValue() + counter);
				counter = 0;
			}

		}

		//mapevent2transition
		/*for (int i = 0; i < trace.size(); i++) {


		 * Update progress counter.

			counter++;
			if (counter > 299) {
				progress.setValue(progress.getValue() + counter);
				counter = 0;
			}

			// Transition corresponding to current (i) event in trace.
			String lt = trace.get(i).getAttributes().get("lifecycle:transition").toString();
			String cn = trace.get(i).getAttributes().get("concept:name").toString();
			String evento = cn+"+"+lt;
			String traceName = XConceptExtension.instance().extractName(trace.get(i));
			String lifecycle =  XLifecycleExtension.instance().extractTransition(trace.get(i));




			//Transition transition =  mapevent2transition.get(xec);

			//if (!CollArc.isEmpty()) {
			// Source state of transition.
			//State sourceState = transition.getSource();
			// Target state of transition.
			//State targetState = transition.getTarget();
			// Time of current event in trace.
			long eventTime = getEventTime(trace.get(i)).getTime();


		 * The animation has some problems if the start and end time of
		 * transitions coincide. To prevent this, we tweak both the
		 * start and the end time.

			long startTime = (i > 0 ? prevEventTime : eventTime);

		 * Start time should be at least the last used end time.
		 * Otherwise, causality might be violated.

			if (startTime < lastEndTime) {
				startTime = lastEndTime;
			}
			long endTime = eventTime;

		 * The end time should exceed the start time.

			if (endTime <= startTime) {
				endTime = startTime + 2;
			}

		 * Remember the end time.

			lastEndTime = endTime;

			long medtime = ((end - startTime)>0 ? (end - startTime): 1 ) ;

		 * Create a token animation for the current transition. Use time
		 * of current event as time when case arrives at target state.
		 * Use of time of previous event (if any) as time when case
		 * departs from source state.

			Collection<Transition> MapTransitiondel = new Vector<Transition>();

			Collection<PetrinetNode> colpn = MapNodeTracia(MapTransition);


			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>  colledge = MapEdge(colpn,evento);

			int sizes = colledge.size();
			for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge :colledge ){
				if(i==0){
					PetrinetNode node = edge.getSource();
					addKeyframe(node, new NodeAnimationKeyframe(trace, startTime, true, trace.size() == 1));
				}else{
					PetrinetNode node = edge.getTarget();
					addKeyframe(node, new NodeAnimationKeyframe(trace, endTime, false, i == trace.size() - 1));
				}
				addTokenAnimation(edge, new TokenAnimation(trace, startTime,endTime ));


			}

			for(Transition t :MapTransition){
				//e' la transione che cerco 
				//no quindi è invibile trovo i suoi preset se ci sono token faccio attivare l'arco
				//trovo tutti i postset e ci metto il token ad attivo gli archi
				//la elimino dalla lista
				//si allora la disegno come prima la cancello ed esco dal for
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edges = t.getGraph().getInEdges(t);
				for( PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edgein:edges ){
					if(edgein.getSource() instanceof Place){
						Place in = (Place) edgein.getSource();

						//if(marking.occurrences(in)>0){
						//marking.remove(in);
						//anima il place e
						//aggiungi animazione a questo arco	
						addTokenAnimation(edgein, new TokenAnimation(trace, startTime, endTime-1));

						if(i==0){
							addKeyframe(in, new NodeAnimationKeyframe(trace, startTime, false,i == trace.size() - 1) );
						}//else addKeyframe(in, new NodeAnimationKeyframe(trace,startTime , false,trace.size() == 1) );

						//addKeyframe(in, new NodeAnimationKeyframe(trace, startTime, true, true));
						//}
					}
				}
				// rimuovila 
				MapTransitiondel.add(t);
				//anima la transiozione
				addKeyframe(t, new NodeAnimationKeyframe(trace, endTime, false, i == trace.size() - 1));
				//archi uscenti
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> postset = t.getGraph().getOutEdges(t);
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edgeout : postset) {
					if(edgeout.getTarget() instanceof Place){
						Place out = (Place) edgeout.getTarget();
						//marking.add(out);
						//aggiungo l'animazione del l'arco e del place
						addTokenAnimation(edgeout, new TokenAnimation(trace,endTime, endTime+1));
						addKeyframe(out, new NodeAnimationKeyframe(trace, endTime+1, false,i == trace.size() - 1 ));
					}
				}

				if(!t.isInvisible()){
					if(t.getLabel()==evento){
						break;
					}
					break;
				}


			}
			for(Transition t : MapTransitiondel){
				MapTransition.remove(t);
			}
			int num = CollArc.size();

				Integer index = 1;
				long fendTime = endTime/num;

				for(Arc a : CollArc) {

					addTokenAnimation(a, new TokenAnimation(trace, startTime, fendTime));
					startTime=fendTime;
					fendTime+=fendTime;
				}


		 * Create a state animation for the source state if this is the
		 * first event. Case is always created in this state. Case is
		 * only terminated in this state if trace contains only one
		 * event.

			if (i == 0) {
				//			addKeyframe(sourceState, new NodeAnimationKeyframe(trace, startTime, true, trace.size() == 1));
			}


		 * Create a state animation for the target state. Case is never
		 * created in this state. Case is only terminated in this state
		 * if this event is last event.

			//	addKeyframe(targetState, new NodeAnimationKeyframe(trace, endTime, false, i == trace.size() - 1));


		 * Update the boundaries for the entire animation, if needed.

			updateBoundaries(eventTime);


		 * Update previous event time.

			prevEventTime = eventTime;
			//}
		}*/
		/*
		 * Update progress counter.
		 */
		progress.setValue(progress.getValue() + counter);
		counter = 0;

	}


	private Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> MapEdge(
			Collection<PetrinetNode> mapTransition, String evento) {
		Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> mapEdge = new Vector<PetrinetEdge<? extends PetrinetNode,? extends PetrinetNode>>();
		Collection<PetrinetNode> MapTransitiondel = new Vector<PetrinetNode>();
		for(PetrinetNode t :mapTransition){

			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edges = t.getGraph().getInEdges(t);
			for( PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edgein:edges ){
				if(edgein.getSource() instanceof Place){
					Place in = (Place) edgein.getSource();
					mapEdge.add(edgein);

				}
			}
			MapTransitiondel.add(t);
			/*Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> postset = t.getGraph().getOutEdges(t);
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edgeout : postset) {
				//if(edgeout.getTarget() instanceof Place){
				//	Place out = (Place) edgeout.getTarget();
					mapEdge.add(edgeout);
				//}
			}*/
			if(t instanceof Transition){
				Transition tt = (Transition) t;
				if(!tt.isInvisible()){
					break;
				}
			}

		}
		for(PetrinetNode t : MapTransitiondel){
			mapTransition.remove(t);
		}


		return mapEdge;
	}

	private Collection<PetrinetNode> MapNodeTracia(Collection<Transition> mapTransition) {

		Collection<PetrinetNode>	mapnode = new Vector<PetrinetNode>();

		for(Transition t :mapTransition){

			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edges = t.getGraph().getInEdges(t);
			for( PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edgein:edges ){
				if(edgein.getSource() instanceof Place){
					Place in = (Place) edgein.getSource();
					if(!mapnode.contains(in)){
						mapnode.add(in);
					}
				}
			}

			mapnode.add(t);

			/*Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> postset = t.getGraph().getOutEdges(t);
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edgeout : postset) {
				if(edgeout.getTarget() instanceof Place){
					Place out = (Place) edgeout.getTarget();
					mapnode.add(in);
				}
			}*/

		}

		return mapnode;
	}

	public void paintNodeBackground(AbstractDirectedGraphNode node, Graphics2D g2d, double x, double y, double width, double height) {
		if(node instanceof Transition){
			Rectangle2D rect = new Rectangle2D.Double(x, y, width, height);
			g2d.fill(rect);
		}else if(node instanceof Place){
			Ellipse2D ellipse = new Ellipse2D.Double(x, y, width, height);
			g2d.fill(ellipse);
		}

	}

	public void paintNodeBorder(AbstractDirectedGraphNode node, Graphics2D g2d, double x, double y, double width, double height) {
		if(node instanceof Transition){
			Rectangle2D rect = new Rectangle2D.Double(x, y, width, height);
			g2d.draw(rect);
		}else if(node instanceof Place){
			Ellipse2D ellipse = new Ellipse2D.Double(x, y, width, height);
			g2d.draw(ellipse);
		}
	}

	public void paintNodeText(AbstractDirectedGraphNode node, Graphics2D g2d, double x, double y, double width, double height) {
		String label;
		if(node instanceof Transition){
			Transition state = (Transition) node;
			label = state.getLabel();
		}else{
			Place state = (Place) node;
			label = state.getLabel();
		}

		Rectangle2D bound = g2d.getFontMetrics().getStringBounds(label, g2d);
		g2d.drawString(label, (int) (x + (width - bound.getWidth()) / 2) , (int) (y + (height + bound.getHeight()) / 2));
	}

	public void paintTokenLabel(AbstractDirectedGraphEdge<?, ?> edge, XTrace trace, Graphics2D g2d, double x, double y) {
		String traceName = AnimationLog.getTraceName(trace);
		Rectangle2D bound = g2d.getFontMetrics().getStringBounds(traceName, g2d);
		g2d.drawString(traceName, (int) x, (int) (y - bound.getHeight() / 2));
		String name = edge.getSource().getLabel();
		g2d.drawString(name, (int) x, (int) (y + bound.getHeight() / 2));
	}

	public float getActivity(long modelTime, long maxTaskDelay) {
		float maxActivity = 0;
		float activity = 0;
		for (NodeAnimation anim : getNodeAnimations()) {
			maxActivity++;
			if (anim.getKeyframe(modelTime).getTime() > maxTaskDelay) {
				activity++;
			}
		}
		for (EdgeAnimation anim : getEdgeAnimations()) {
			maxActivity++;
			activity += anim.getKeyframe(modelTime).getTokenAnimations().size();
		}
		return activity / maxActivity;
	}
	private List<XEventClass> getList(XTrace trace, XEventClasses classes) {
		List<XEventClass> list = new ArrayList<XEventClass>();
		for (XEvent event : trace) {
			list.add(classes.getClassOf(event));
		}
		return list;
	}
	private XEventClasses getEventClasses(XLog log) {
		XEventClassifier classifier = XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo summary = XLogInfoFactory.createLogInfo(log, classifier);
		XEventClasses eventClasses = summary.getEventClasses(classifier);
		return eventClasses;
	}

	private Map<XEventClass,Transition> getMapping(XEventClasses classes, Petrinet net) {
		Map<XEventClass,Transition> map = new HashMap<XEventClass,Transition>();

		for (Transition transition : net.getTransitions()) {
			for (XEventClass eventClass : classes.getClasses()) {
				if (eventClass.getId().equals(transition.getAttributeMap().get(AttributeMap.LABEL))) {
					map.put(eventClass,transition);
				}
			}
		}
		return map;
	}
	/*
	private void addArcUsage(Arc arc, XEventClass xec) {
		if(MapArc.containsKey(xec)){
			Collection<Arc> arcusage = MapArc.get(xec);
			arcusage.add(arc);
		}else{
			Collection<Arc> a = new Vector<Arc>();
			a.add(arc);
			MapArc.put(xec, a);		
		}


	}*/
	private Map<Transition, XEventClass> getMappings(XEventClasses classes, Petrinet net) {
		Map<Transition, XEventClass> map = new HashMap<Transition, XEventClass>();

		for (Transition transition : net.getTransitions()) {
			for (XEventClass eventClass : classes.getClasses()) {
				if (eventClass.getId().equals(transition.getAttributeMap().get(AttributeMap.LABEL))) {
					map.put(transition, eventClass);
				}
			}
		}
		return map;
	}
	private List<Transition> sortHiddenTransection(Petrinet net, List<Transition> sequence,
			Map<Transition, XEventClass> map) {
		for (int i=1; i<sequence.size(); i++) {
			Transition current = sequence.get(i);
			// Do not move visible transitions
			if (map.containsKey(current)) {
				continue;
			}
			Set<Place> presetCurrent = new HashSet<Place>();
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getInEdges(current)) {
				if (! (edge instanceof Arc))
					continue;
				Arc arc = (Arc) edge;
				Place place = (Place)arc.getSource();
				presetCurrent.add(place);
			}

			int k = i-1;
			while (k >= 0) {
				Transition prev = sequence.get(k);
				Set<Place> postsetPrev = new HashSet<Place>();
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getOutEdges(prev)) {
					if (! (edge instanceof Arc))
						continue;
					Arc arc = (Arc) edge;
					Place place = (Place)arc.getTarget();
					postsetPrev.add(place);
				}

				// Intersection
				Set<Place> intersection = new HashSet<Place>();
				for (Place place : postsetPrev) {
					if (presetCurrent.contains(place))
						intersection.add(place);
				}
				if (intersection.size() > 0)
					break;

				// Swap Transitions
				sequence.remove(k);
				sequence.add(k+1, prev);

				k-=1;
			}
		}
		return sequence;
	}


}
