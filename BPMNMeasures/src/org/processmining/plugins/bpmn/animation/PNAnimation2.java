package org.processmining.plugins.bpmn.animation;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import javax.swing.JComponent;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;
import org.processmining.connections.logmodel.LogPetrinetConnection;
import org.processmining.connections.logmodel.LogPetrinetConnectionImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.models.animation.Animation;
import org.processmining.models.animation.AnimationLog;
import org.processmining.models.animation.EdgeAnimation;
import org.processmining.models.animation.NodeAnimation;
import org.processmining.models.animation.NodeAnimationKeyframe;
import org.processmining.models.animation.TokenAnimation;
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
import org.processmining.plugins.connectionfactories.logpetrinet.LogPetrinetConnectionFactoryUI;
import org.processmining.plugins.petrinet.replay.ReplayAction;
import org.processmining.plugins.petrinet.replay.Replayer;
import org.processmining.plugins.petrinet.replayfitness.ReplayFitnessCost;
import org.processmining.plugins.petrinet.replayfitness.ReplayFitnessSetting;

public class PNAnimation2 extends Animation {

	private XEventClasses classes;
	private Marking marking;
	Map<XTrace,Collection<ArcTime>> mapevent2arc= new HashMap<XTrace, Collection<ArcTime>>();


	public PNAnimation2(UIPluginContext context, Petrinet pn, XLog log,Marking initMarking) {
		super(context, pn, log);

		marking = new Marking(initMarking);
		createlisttransitionlogevent(context, log, pn);
	}

	private void createlisttransitionlogevent(UIPluginContext context, XLog log,
			Petrinet net) {

		XEventClassifier classifier = XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo summary = XLogInfoFactory.createLogInfo(log, classifier);
		classes = summary.getEventClasses(classifier);

		ReplayFitnessSetting settings = new ReplayFitnessSetting();
		System.out.println("Settings: " + settings);
		settings.setAction(ReplayAction.INSERT_ENABLED_MATCH, true);
		settings.setAction(ReplayAction.INSERT_ENABLED_INVISIBLE, true);
		settings.setAction(ReplayAction.REMOVE_HEAD, false);
		settings.setAction(ReplayAction.INSERT_ENABLED_MISMATCH, false);
		settings.setAction(ReplayAction.INSERT_DISABLED_MATCH, true);
		settings.setAction(ReplayAction.INSERT_DISABLED_MISMATCH, false);

		//Build and show the UI to make the mapping
		LogPetrinetConnectionFactoryUI lpcfui = new LogPetrinetConnectionFactoryUI(log, net);

		//Create map or not according to the button pressed in the UI
		Map<Transition, XEventClass> map=null;
		InteractionResult result =null;
		/*
		 * The wizard loop.
		 */
		boolean sem=true;
		/*
		 * Show the current step.
		 */
		JComponent mapping = lpcfui.initComponents();
		result = context.showWizard("Mapping Petrinet - Log", true, true, mapping );

		switch (result) {

		case FINISHED :
			/*
			 * Return  final step.
			 */
			map = lpcfui.getMap();
			sem=false;
			break;
		default :
			/*
			 * Should not occur.
			 */
			context.log("press Cancel");
			break;
		}

		context.getConnectionManager().addConnection(new LogPetrinetConnectionImpl(log, classes, net, map));
		PetrinetSemantics semantics = PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class);

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



	}


	private void updatemapping(Petrinet net, Marking initMarking,
			List<Transition> sequence, XTrace trace,
			Map<Transition, XEventClass> map, Collection<ArcTime> collArcTime,
			Map<Place, ArcTime> mapplace2arc) {

		XAttributeTimestampImpl date  = (XAttributeTimestampImpl)(trace.get(0).getAttributes().get("time:timestamp"));
		long pre = date.getValue().getTime()-1000000;

		//XAttributeTimestampImpl date  = null;
		long timeStartTrace =1;
		long timeEndTrace =0;
		/*Date datazeo = new Date(2009, 6, 32, 10, 12, 24);
		 GregorianCalendar liftOffApollo11 = new GregorianCalendar(2011, Calendar.MAY, 16, 9, 32);
		 datazeo = liftOffApollo11.getTime();
		long pre=datazeo.getTime();*/
		
		Marking newmarking = new Marking(initMarking);
		for (Place p : newmarking.baseSet()){

			//mapplace2arc.put(p, new ArcTime(p.getGraph().getOutEdges(p).iterator().next(), timeStartTrace, -1));

		}
		Map<Place,Long> placetime = new HashMap<Place, Long>();

		Map<Arc, Vector<Arc>> arcarc = new HashMap<Arc, Vector<Arc>>();

		Collection<Arc> carc = new Vector<Arc>();


		int iTrace = -1;
		for(Transition t :sequence){
			if (map.containsKey(t)) {
				iTrace+=1;
			}

			if(!t.isInvisible()){
				if(t.getLabel().endsWith("start")){
					
					date  = (XAttributeTimestampImpl)(trace.get(iTrace).getAttributes().get("time:timestamp"));
					timeStartTrace = date.getValue().getTime();
					Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outedge = t.getGraph().getOutEdges(t);
					for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : outedge) {

						Arc arc = (Arc) edge;
						Place p = (Place)arc.getTarget();
						ArcTime arttime = new ArcTime(arc, timeStartTrace);

						mapplace2arc.put(p, arttime);

						placetime.put(p, timeStartTrace);
						
					}
					if(!carc.isEmpty()){
						int tot = carc.size();
						long delta = (timeStartTrace-pre)/(tot+1);
						int i=0;
						
						for(Arc a : carc){
							long  x = pre+(delta*i);
							i++;
							ArcTime arctime = new ArcTime(a, x);
							arctime.setEndDeltaTime(delta);
							collArcTime.add(arctime); 
							if(arcarc.containsKey(a)){
								Vector<Arc> varc = arcarc.get(a);
								for(Arc aa : varc){
									ArcTime arctimee = new ArcTime(aa, x);
									arctimee.setEndDeltaTime(delta);
									collArcTime.add(arctimee);
									Place p = (Place) a.getTarget();
									placetime.put(p, arctimee.getEndTime());
								}
							}
							if(i==tot){
								Place p = (Place) a.getTarget();
								placetime.put(p, arctime.getEndTime());
								
							}
						}
						carc.clear();
						Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inedges = t.getGraph().getInEdges(t);
						for( PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge  : inedges ){
							Arc arc = (Arc) edge;
							Place p = (Place)arc.getSource();
							if(placetime.containsKey(p)){
								
							long temp = placetime.get(p);
							collArcTime.add( new  ArcTime(arc, temp, timeStartTrace));
							
							}
							
							
						}
						
					}

				}
				if(t.getLabel().endsWith("complete")){
					date  = (XAttributeTimestampImpl)(trace.get(iTrace).getAttributes().get("time:timestamp"));
					pre = timeEndTrace = date.getValue().getTime();
					Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inedge = t.getGraph().getInEdges(t);
					for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : inedge) {

						Arc arc = (Arc) edge;
						Place p = (Place)arc.getSource();

						long starttime = placetime.get(p);
						long delta = (timeEndTrace-starttime)/2;

						ArcTime arttime = mapplace2arc.remove(p);
						arttime.setEndDeltaTime(delta);


						collArcTime.add(arttime);

						ArcTime arttim = new ArcTime(arc, timeEndTrace-delta, timeEndTrace);

						collArcTime.add(arttim);  
					}
				}

			}else{
				//invisible transition
				int numout = t.getGraph().getOutEdges(t).size();
				int numin = t.getGraph().getInEdges(t).size();
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outedge = t.getGraph().getOutEdges(t);
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inedge = t.getGraph().getInEdges(t);
				if(numout==1 && numin==1){
					Arc a = (Arc) outedge.iterator().next();
					Place p = (Place) a.getTarget();
					
					Arc b = (Arc) inedge.iterator().next();
					//Place q = (Place) b.getSource();
					carc.add(b);
					carc.add(a);

				}else if(numout>numin){
					Arc b = (Arc) inedge.iterator().next();
					//Place q = (Place) b.getSource();
					carc.add(b);
					int i=-1;
					Arc c = null;
					for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge: outedge){
						i++;
						Arc a = (Arc) edge;
						//	Place p = (Place) a.getTarget();
						if(i==0){
							carc.add(a);
						}else{
							if(!arcarc.containsKey(c)){
								Vector<Arc> sd = new Vector<Arc>();
								sd.add(a);
								arcarc.put(c,sd );
							}else{
								Vector<Arc> sd =arcarc.get(c);
								sd.add(a);
							}
						}
						c=a;
					}


				}else if(numout<numin){

				}
			}
		}
		/*int iTrace = -1;
		for (int iTrans=0; iTrans<sequence.size(); iTrans++) {
			Transition transition = sequence.get(iTrans);

			long timeEventTransition=0;



			if (map.containsKey(transition)) {
				iTrace+=1;
			}

			if(iTrace>=0){
				XEvent event = trace.get(iTrace);
				XAttributeTimestampImpl date1  = (XAttributeTimestampImpl)(event.getAttributes().get("time:timestamp"));
				timeEventTransition = date1.getValue().getTime();
			}

			float deltaTime = timeEventTransition-timeStartTrace;

			if((deltaTime<timeEventTransition)&&(deltaTime>0)){

			}*/

		//UpdateTimeArc(transition,timeEventTransition,timeStartTrace,collArcTime,mapplace2arc,newmarking);

		//UpdateMarking(newmarking,transition);

		//timeStartTrace =timeEventTransition;
		//}

	}

	private List<XEventClass> getList(XTrace trace, XEventClasses classes) {
		List<XEventClass> list = new ArrayList<XEventClass>();
		for (XEvent event : trace) {
			list.add(classes.getClassOf(event));
		}
		return list;
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
	@Override
	protected void createAnimations(XTrace trace, Progress progress)
	throws IndexOutOfBoundsException {


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

		/*
		 * Update progress counter.
		 */
		progress.setValue(progress.getValue() + counter);
		counter = 0;

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
}
