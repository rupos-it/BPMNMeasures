package org.processmining.plugins.bpmn.animation;



import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;


@Plugin(name = "Animate PetriNet System", parameterLabels = {
		"Petrinet  ", "Log" , "Marking"}, returnLabels = { "PN System Animation" }, returnTypes = { PNAnimation.class }, userAccessible = true)
public class PNAnimationPlugin {

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "GOs", email = "DI.unipi")
	@PluginVariant(variantLabel = "Select conversions to use", requiredParameterLabels = {
			0, 1,2 })
	public PNAnimation options(final UIPluginContext context,
			final Petrinet pn, final XLog log, final Marking mark) {
		PNAnimation animation = new PNAnimation(context, pn, log,  mark);
		animation.initialize(context, pn, log );
		return animation;
	}

}
