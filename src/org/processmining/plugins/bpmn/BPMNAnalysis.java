package org.processmining.plugins.bpmn;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.replay.conformance.ReplayConformancePlugin;
import org.processmining.plugins.petrinet.replay.conformance.ReplayConformancePluginNew;
import org.processmining.plugins.petrinet.replay.conformance.TotalConformanceResult;
import org.processmining.plugins.petrinet.replay.performance.ReplayPerformancePlugin;
import org.processmining.plugins.petrinet.replay.performance.TotalPerformanceResult;
import org.processmining.plugins.petrinet.replayfitness.ReplayFitnessSetting;


public class BPMNAnalysis {
	/*@Plugin(name = "BPMN Conformance Analysis", returnLabels = { "Conformace Total" }, returnTypes = { TotalConformanceResult.class }, parameterLabels = {"Log","BPMNDiagram"}, userAccessible = true)
	@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "BPMNMeasures")
	@PluginVariant(variantLabel = "BPMN Log", requiredParameterLabels = { 0,1})
	public TotalConformanceResult getConformanceDetails( PluginContext context, XLog log, BPMNDiagram net) {
		BPMNtoPN converter = new BPMNtoPN();
		Object[] objects = (Object[]) converter.BPMN2PN(context, net);
		Petrinet netPetri = (Petrinet) objects[0] ;
		Marking  marking =(Marking) objects[1];
		ReplayConformancePlugin rcp = new ReplayConformancePlugin();
		return rcp.getConformanceDetails(context, log, netPetri);
		
		
	}*/ 
	@Plugin(name = "BPMN Conformance Analysis New", returnLabels = { "Conformace Total" }, returnTypes = { TotalConformanceResult.class }, parameterLabels = {"Log","BPMNDiagram"}, userAccessible = true)
	@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "BPMNMeasures")
	@PluginVariant(variantLabel = "BPMN Log", requiredParameterLabels = { 0,1})
	public TotalConformanceResult getConformanceDetails( UIPluginContext context, XLog log, BPMNDiagram net) {
		BPMNtoPN converter = new BPMNtoPN();
		Object[] objects = (Object[]) converter.BPMN2PN(context, net);
		Petrinet netPetri = (Petrinet) objects[0] ;
		Marking  marking =(Marking) objects[1];
		ReplayConformancePluginNew rcp = new ReplayConformancePluginNew();
		return rcp.getConformanceDetails(context, log, netPetri);
		
		
	}
	
	
	@Plugin(name = "BPMN Performance Analysis", parameterLabels = { "Log", "BPMNDiagram" }, returnLabels = { "Result of Performance" }, returnTypes = { TotalPerformanceResult.class })
	@PluginVariant(requiredParameterLabels = { 0,1 }, variantLabel = "PerformanceDetailsSettings")
	@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "PetriNetReplayAnalysis")
	public TotalPerformanceResult getPerformanceDetails(UIPluginContext context, XLog log,BPMNDiagram net ) {

		BPMNtoPN converter = new BPMNtoPN();
		Object[] objects = (Object[]) converter.BPMN2PN(context, net);
		Petrinet netPetri = (Petrinet) objects[0] ;
		Marking  marking =(Marking) objects[1];
		ReplayPerformancePlugin rpp = new ReplayPerformancePlugin();
		return rpp.getPerformanceDetails(context, log, netPetri);
		
	}

}
