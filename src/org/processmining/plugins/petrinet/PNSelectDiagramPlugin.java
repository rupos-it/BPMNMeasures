package org.processmining.plugins.petrinet;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;


@Plugin(name = "Select PN Diagram", parameterLabels = { "Petri Net Set" , "Element"}, returnLabels = { "PN Diagram", "Marking" }, returnTypes = { Petrinet.class, Marking.class }, userAccessible = true)
public class PNSelectDiagramPlugin  {

	
	@UITopiaVariant(affiliation = "Department of Computer Science University of Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it",uiLabel = UITopiaVariant.USEPLUGIN, pack ="")
	@PluginVariant( requiredParameterLabels = { 0,1 })
	public Object[]  selectDialog( PluginContext context,  ArrayPetriNet pn, Integer index) {
		if (index>pn.size()){
			
			index=0;
		}
		Marking marking= pn.getm(index);
		Petrinet net = pn.getp(index);
		
		return new Object[] { net,marking};
	}
	@UITopiaVariant(affiliation ="ISTI CNR Pisa", author = "G.Spagnolo", email = "spagnolo@isti.cnr.it", uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(requiredParameterLabels = { 0 })
	public Object[] selectDialog( UIPluginContext context,  ArrayPetriNet pn) {
	
		PNSelectDiagramDialog dialog = new PNSelectDiagramDialog(pn);
		InteractionResult result = context.showWizard("Select PN diagram", true, true, dialog);
		if (result != InteractionResult.FINISHED) {
			return null;
		}
		Marking marking= pn.getm(dialog.getSelected());;
		Petrinet net = pn.getp(dialog.getSelected());
		
		return new Object[] {net, marking};
	}
	
	

}
