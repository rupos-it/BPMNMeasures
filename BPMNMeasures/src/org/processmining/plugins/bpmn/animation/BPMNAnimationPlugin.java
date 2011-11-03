package org.processmining.plugins.bpmn.animation;



import java.util.List;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.bpmn.exporting.metrics.BPMNPerfMetrics;


@Plugin(name = "Animate BPMN", parameterLabels = {
		"BPMN  ", "Log", "BPMN Performace Metrics" }, returnLabels = { "BPMN System Animation" }, returnTypes = { BPMNAnimation.class }, userAccessible = true)
public class BPMNAnimationPlugin {

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "GOs", email = "DI.unipi")
	@PluginVariant( requiredParameterLabels = { 0, 1, 2})
	public BPMNAnimation options(final UIPluginContext context,
			final BPMNDiagram bpmn, final XLog log , final List<BPMNPerfMetrics> metrics) {
		BPMNAnimation animation = new BPMNAnimation(context, bpmn , log, metrics);
		animation.initialize(context, bpmn, log );
		return animation;
	}

}