package org.processmining.plugins.bpmn.animation;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.models.animation.Animation;
import org.processmining.models.animation.AnimationLog;
import org.processmining.models.animation.EdgeAnimation;
import org.processmining.models.animation.NodeAnimation;
import org.processmining.models.animation.NodeAnimationKeyframe;
import org.processmining.models.animation.TokenAnimation;
import org.processmining.models.graphbased.directed.AbstractDirectedGraphEdge;
import org.processmining.models.graphbased.directed.AbstractDirectedGraphNode;
import org.processmining.models.graphbased.directed.DirectedGraph;

import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.elements.Activity;
import org.processmining.models.graphbased.directed.bpmn.elements.Event;
import org.processmining.models.graphbased.directed.bpmn.elements.Gateway;
import org.processmining.models.graphbased.directed.bpmn.elements.IGraphElementDecoration;
import org.processmining.models.graphbased.directed.bpmn.elements.Gateway.GatewayType;

import org.processmining.plugins.bpmn.exporting.metrics.BPMNPerfMetrics;

public class BPMNAnimation extends Animation {

	BPMNDiagram bpmn;
	List<BPMNPerfMetrics> metrics;
	XLog log;
	
	public BPMNAnimation(
			PluginContext context,
			DirectedGraph<? extends AbstractDirectedGraphNode, ? extends AbstractDirectedGraphEdge<?, ?>> graph,
			XLog log, List<BPMNPerfMetrics> metrics) {
		super(context, graph, log);
		this.bpmn=(BPMNDiagram) graph;
		this.log=log;
		this.metrics=metrics;
		
		
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
		for (int i = 0; i < trace.size(); i++) {
			/*
			 * Update progress counter.
			 */
			counter++;
			if (counter > 299) {
				progress.setValue(progress.getValue() + counter);
				counter = 0;
			}

			// Transition corresponding to current (i) event in trace.
			//Transition transition = ts.getTransition(trace, i);
		//	if (transition != null) {
				// Source state of transition.
			//	State sourceState = transition.getSource();
				// Target state of transition.
			//	State targetState = transition.getTarget();
				// Time of current event in trace.
				long eventTime = getEventTime(trace.get(i)).getTime();

				/*
				 * The animation has some problems if the start and end time of
				 * transitions coincide. To prevent this, we tweak both the
				 * start and the end time.
				 */
				long startTime = (i > 0 ? prevEventTime : eventTime);
				/*
				 * Start time should be at least the last used end time.
				 * Otherwise, causality might be violated.
				 */
				if (startTime < lastEndTime) {
					startTime = lastEndTime;
				}
				long endTime = eventTime;
				/*
				 * The end time should exceed the start time.
				 */
				if (endTime <= startTime) {
					endTime = startTime + 1;
				}
				/*
				 * Remember the end time.
				 */
				lastEndTime = endTime;

				/*
				 * Create a token animation for the current transition. Use time
				 * of current event as time when case arrives at target state.
				 * Use of time of previous event (if any) as time when case
				 * departs from source state.
				 */
			//	addTokenAnimation(transition, new TokenAnimation(trace, startTime, endTime));

				/*
				 * Create a state animation for the source state if this is the
				 * first event. Case is always created in this state. Case is
				 * only terminated in this state if trace contains only one
				 * event.
				 */
				if (i == 0) {
			//		addKeyframe(sourceState, new NodeAnimationKeyframe(trace, startTime, true, trace.size() == 1));
				}

				/*
				 * Create a state animation for the target state. Case is never
				 * created in this state. Case is only terminated in this state
				 * if this event is last event.
				 */
			//	addKeyframe(targetState, new NodeAnimationKeyframe(trace, endTime, false, i == trace.size() - 1));

				/*
				 * Update the boundaries for the entire animation, if needed.
				 */
				updateBoundaries(eventTime);

				/*
				 * Update previous event time.
				 */
				prevEventTime = eventTime;
			//}
		}
		/*
		 * Update progress counter.
		 */
		progress.setValue(progress.getValue() + counter);
		counter = 0;
		
	}

	@Override
	public void paintNodeBackground(AbstractDirectedGraphNode node,
			Graphics2D g2d, double x, double y, double width, double height) {
		/*if(node instanceof Activity){
			Rectangle2D rect = new Rectangle2D.Double(x, y, width, height);
			g2d.fill(rect);
		}else if(node instanceof Event){
			Ellipse2D ellipse = new Ellipse2D.Double(x, y, width, height);
			g2d.fill(ellipse);
		}else if(node instanceof Gateway){
			Gateway gw = (Gateway) node;
			GeneralPath gatewayDecorator = new GeneralPath();
			drawSigns(g2d, gatewayDecorator, gw.getGatewayType() );

			double scalefactor = width / 33;
			double scaleX = 0;
			double scaleY = 0;
			
			AffineTransform at = new AffineTransform();
			//at.scale(scalefactor, scalefactor);
			//gatewayDecorator.transform(at);

			at = new AffineTransform();
			at.translate(x+scaleX, y+scaleY);
			gatewayDecorator.transform(at);
			if (gw.getGatewayType() == GatewayType.DATABASED) {
				g2d.fill(gatewayDecorator);
			} else {
				g2d.fill(gatewayDecorator);
			}
		}*/
		
	}

	@Override
	public void paintNodeBorder(AbstractDirectedGraphNode node, Graphics2D g2d,
			double x, double y, double width, double height) {
		if(node instanceof Activity){
			Rectangle2D rect = new Rectangle2D.Double(x, y, width, height);
			g2d.draw(rect);
		}else if(node instanceof Event){
			Ellipse2D ellipse = new Ellipse2D.Double(x, y, width, height);
			g2d.draw(ellipse);
		}else if(node instanceof Gateway){
			Gateway gw = (Gateway) node;
			GeneralPath gatewayDecorator = new GeneralPath();
			drawSigns(g2d, gatewayDecorator, gw.getGatewayType() );

			double scalefactor = width / 33;
			double scaleX = 0;
			double scaleY = 0;
			
			AffineTransform at = new AffineTransform();
			//at.scale(scalefactor, scalefactor);
			//gatewayDecorator.transform(at);

			at = new AffineTransform();
			at.translate(x+scaleX, y+scaleY);
			gatewayDecorator.transform(at);
			
			g2d.draw(gatewayDecorator);
			
		}
	}

	@Override
	public void paintNodeText(AbstractDirectedGraphNode node, Graphics2D g2d,
			double x, double y, double width, double height) {
		String label =		node.getLabel();
		
		Rectangle2D bound = g2d.getFontMetrics().getStringBounds(label, g2d);
		
		//g2d.drawString(label, (int) (x + (width - bound.getWidth()) / 2) , (int) (y + (height + bound.getHeight()) / 2));
	}

	@Override
	public void paintTokenLabel(AbstractDirectedGraphEdge<?, ?> edge,
			XTrace trace, Graphics2D g2d, double x, double y) {
		String traceName = AnimationLog.getTraceName(trace);
		Rectangle2D bound = g2d.getFontMetrics().getStringBounds(traceName, g2d);
		g2d.drawString(traceName, (int) x, (int) (y - bound.getHeight() / 2));
		String name = edge.getSource().getLabel();
		g2d.drawString(name, (int) x, (int) (y + bound.getHeight() / 2));
		
	}

	@Override
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
	
	private void drawSigns(Graphics2D g2d, GeneralPath gatewayDecorator, GatewayType gatewayType) {
		/*gatewayDecorator.moveTo(6.75F, 16F);
		gatewayDecorator.lineTo(16F, 6.75F);
		gatewayDecorator.moveTo(16F, 6.75F);
		gatewayDecorator.lineTo(25.75F, 16F);
		gatewayDecorator.moveTo(25.75F, 16F);
		gatewayDecorator.lineTo(16F, 25.75F);
		gatewayDecorator.moveTo(16F, 25.75F);
		gatewayDecorator.lineTo(6.75F, 16F);*/
		
		if (gatewayType == GatewayType.DATABASED ) {
			gatewayDecorator.moveTo(8.75F, 7.55F);
			gatewayDecorator.lineTo(12.75F, 7.55F);
			gatewayDecorator.lineTo(23.15F, 24.45F);
			gatewayDecorator.lineTo(19.25F, 24.45F);
			gatewayDecorator.closePath();
			gatewayDecorator.moveTo(8.75F, 24.45F);
			gatewayDecorator.lineTo(19.25F, 7.55F);
			gatewayDecorator.lineTo(23.15F, 7.55F);
			gatewayDecorator.lineTo(12.75F, 24.45F);
			gatewayDecorator.closePath();
		} else if (gatewayType == GatewayType.EVENTBASED) {
			gatewayDecorator.append(new Ellipse2D.Double(7.5F, 7.5F, 17F, 17F), false);
			gatewayDecorator.append(new Ellipse2D.Double(5F, 5F, 22F, 22F), false);
			gatewayDecorator.moveTo(20.327514F, 21.344972F);
			gatewayDecorator.lineTo(11.259248F, 21.344216F);
			gatewayDecorator.lineTo(9.4577203F, 13.719549F);
			gatewayDecorator.lineTo(15.794545F, 9.389969F);
			gatewayDecorator.lineTo(22.130481F, 13.720774F);
			gatewayDecorator.closePath();
		} else if (gatewayType == GatewayType.INCLUSIVE ) {
			gatewayDecorator.append(new Ellipse2D.Double(7.5F, 7.5F, 17F, 17F), false);
			g2d.setStroke(new BasicStroke(2.5F));
		} else if (gatewayType == GatewayType.COMPLEX) {
			gatewayDecorator.moveTo(6.25F, 16F);
			gatewayDecorator.lineTo(25.75F, 16F);
			gatewayDecorator.moveTo(16F, 6.25F);
			gatewayDecorator.lineTo(16F, 25.75F);
			gatewayDecorator.moveTo(8.85F, 8.85F);
			gatewayDecorator.lineTo(23.15F, 23.15F);
			gatewayDecorator.moveTo(8.85F, 23.15F);
			gatewayDecorator.lineTo(23.15F, 8.85F);
			g2d.setStroke(new BasicStroke(2.5F));
		} else if (gatewayType == GatewayType.PARALLEL) {
			gatewayDecorator.moveTo(6.75F, 16F);
			gatewayDecorator.lineTo(25.75F, 16F);
			gatewayDecorator.moveTo(16F, 6.75F);
			gatewayDecorator.lineTo(16F, 25.75F);
			g2d.setStroke(new BasicStroke(3));
		}
	}

}
