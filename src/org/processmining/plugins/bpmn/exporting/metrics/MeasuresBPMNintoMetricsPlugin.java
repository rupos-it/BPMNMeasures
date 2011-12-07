package org.processmining.plugins.bpmn.exporting.metrics;






import java.util.Collection;

import java.util.LinkedList;




import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.plugin.events.Logger.MessageLevel;


import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;

import org.processmining.models.graphbased.directed.bpmn.elements.Activity;



import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

import org.processmining.plugins.bpmn.BPMNtoPNConnection;

import org.processmining.plugins.petrinet.replay.conformance.ConformanceResult;
import org.processmining.plugins.petrinet.replay.conformance.TotalConformanceResult;
import org.processmining.plugins.petrinet.replay.performance.PerformanceData;
import org.processmining.plugins.petrinet.replay.performance.PerformanceResult;
import org.processmining.plugins.petrinet.replay.performance.TotalPerformanceResult;
import org.processmining.plugins.petrinet.replay.util.ReplayAnalysisConnection;




public class MeasuresBPMNintoMetricsPlugin {


	@Plugin(name = "BPMNMAnalisysDetailsintoMetricsConformance", parameterLabels = {  "TotalConformanceResult" }, returnLabels = { "BPMN Metrics traslate" }, returnTypes = {
			LinkedList.class }, userAccessible = true)
	@UITopiaVariant(affiliation = "Dipartimento Informatica Università di Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "BPMNMeasures")
	@PluginVariant(requiredParameterLabels = { 0 }, variantLabel = "Exporting  Total Conformance to BPMN Metrics")
	public LinkedList<BPMNConfMetrics> exportBPMNexportXPDL(PluginContext context, TotalConformanceResult totalconformanceresult) throws Exception {

		LinkedList<BPMNConfMetrics> metrics=null;
		try {
			ReplayAnalysisConnection connection = context.getConnectionManager().getFirstConnection(
					ReplayAnalysisConnection.class, context, totalconformanceresult);

			// connection found. Create all necessary component to instantiate inactive visualization panel
			// log = connection.getObjectWithRole(ReplayRuposConnection.XLOG);
			Petrinet net= connection.getObjectWithRole(ReplayAnalysisConnection.PNET);

			BPMNtoPNConnection connection2 = context.getConnectionManager().getFirstConnection(
					BPMNtoPNConnection.class, context, net);

			// connection found. Create all necessary component to instantiate inactive visualization panel

			BPMNDiagram bpmn = connection2.getObjectWithRole(BPMNtoPNConnection.BPMN);
			Collection<Place> placeFlowCollection = connection2.getObjectWithRole(BPMNtoPNConnection.PLACEFLOWCONNECTION);

			metrics = annotateMetricsConformance(totalconformanceresult,net,placeFlowCollection,bpmn);



		} catch (ConnectionCannotBeObtained e) {
			// No connections available
			context.log("Connection does not exist", MessageLevel.DEBUG);

		}

		return metrics;

	}


	private LinkedList<BPMNConfMetrics> annotateMetricsConformance(
			TotalConformanceResult totalconformanceresult, Petrinet net,
			Collection<Place> placeFlowCollection, BPMNDiagram bpmn) {

		LinkedList<BPMNConfMetrics> listaMetrics = new LinkedList<BPMNConfMetrics>();

		for( ConformanceResult conformance: totalconformanceresult.getList()){

			BPMNConfMetrics metric = annotateMetricConformance(conformance,net,placeFlowCollection,bpmn);
			listaMetrics.add(metric);
		}



		return listaMetrics;
	}




	private BPMNConfMetrics annotateMetricConformance(
			ConformanceResult conformance, Petrinet net,
			Collection<Place> placeFlowCollection, BPMNDiagram bpmn) {

		BPMNConfMetrics metric = new BPMNConfMetrics();

		metric.setTraceName(conformance.getTracename());
		Marking remaning = conformance.getRemainingMarking();
		Marking missing = conformance.getMissingMarking();


		for (Transition t : net.getTransitions()) {
			if (!t.isInvisible() &&  !t.getLabel().startsWith("ArtificialEnd")) {
				String tname = t.getLabel();
				String name = (String) tname.subSequence(0, tname.indexOf("+"));

				Activity activity = null;

				// cerco l'attività bpmn a cui collegare le metriche
				for (Activity a : bpmn.getActivities()) {
					if (a.getLabel().equals(name)) {
						activity = a;
						break;
					}
				}
				TaskConfMetrics metritask = new TaskConfMetrics();

				for (Place p : remaning) {
					if (p.getLabel().equals(name)&& tname.endsWith("start")) {
						metritask.addMissingCompletitions();

					} else if (p.getLabel().startsWith(name) && !tname.endsWith("start") ) {
						metritask.addInterruptedExecutions();
					}
				}
				for (Place p : missing) {
					if (p.getLabel().equals(name)&& tname.endsWith("start")) {
						metritask.addInternalFailure();
					}
					if(p.getLabel().endsWith(name)&& tname.endsWith("start")){
						metritask.addUnsoundExecutions();
					}
				}
				String label = activity.getLabel();

				if (activity != null && !metritask.isEmpty()) {



					if(!metric.getTaskMetrics().containsKey(label)){

						metric.addTaskMetrics(label, metritask);



					}else{

						TaskConfMetrics exmetric = metric.getTaskMetrics().get(label);
						exmetric.updateMetric(metritask);

					}


				}

			}else{






				// cerco la transizione del fork
				//t.getLabel().endsWith("_fork")			
				if (t.getGraph().getOutEdges(t).size()>1 && t.isInvisible()) {
					Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> p = t
							.getGraph().getOutEdges(t);

					for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> e : p) {
						Place target = (Place) e.getTarget();
						if(remaning.contains(target)){

							ForkConfMetrics forkmetric = new ForkConfMetrics();

							forkmetric.addInterruptedBranches(target.getLabel(),remaning.occurrences(target));
							metric.addForkMetrics(t.getLabel(), forkmetric);
						}

					}

				}
			}
			// metto gli attraversamenti sugli archi bpmn

		}

		return metric;
	}


	@Plugin(name = "BPMNMAnalisysDetailsintoMetricsPerformance", parameterLabels = {  "TotalPerformanceResult" }, returnLabels = { "BPMN Metrics traslate" }, returnTypes = {
			LinkedList.class }, userAccessible = true)

	@UITopiaVariant(affiliation = "Dipartimento Informatica Università di Pisa", author = "R.Guanciale,G.Spagnolo et al.", email = "spagnolo@di.unipi.it", pack = "BPMNMeasures")
	@PluginVariant(requiredParameterLabels = { 0 }, variantLabel = "BPMN Performance traslate")
	public LinkedList<BPMNPerfMetrics> exportBPMNexportXPDL(PluginContext context,  TotalPerformanceResult totalPerformanceresult) throws Exception {


		try {
			ReplayAnalysisConnection connection = context.getConnectionManager().getFirstConnection(
					ReplayAnalysisConnection.class, context, totalPerformanceresult);

			// connection found. Create all necessary component to instantiate inactive visualization panel
			//XLog log = connection.getObjectWithRole(ReplayRuposConnection.XLOG);
			Petrinet net= connection.getObjectWithRole(ReplayAnalysisConnection.PNET);

			BPMNtoPNConnection connection2 = context.getConnectionManager().getFirstConnection(
					BPMNtoPNConnection.class, context, net);

			// connection found. Create all necessary component to instantiate inactive visualization panel

			BPMNDiagram bpmn = connection2.getObjectWithRole(BPMNtoPNConnection.BPMN);
			Collection< Place>  placeFlowCollection = connection2.getObjectWithRole(BPMNtoPNConnection.PLACEFLOWCONNECTION);

			//cambiare con total
			return  annotateMetricsPerformance(totalPerformanceresult,net,placeFlowCollection,bpmn);



		} catch (ConnectionCannotBeObtained e) {
			// No connections available
			context.log("Connection does not exist", MessageLevel.DEBUG);
			return null;
		}

	}


	private LinkedList<BPMNPerfMetrics> annotateMetricsPerformance(
			TotalPerformanceResult totalPerformanceresult, Petrinet net,
			Collection<Place> placeFlowCollection, BPMNDiagram bpmn) {

		LinkedList<BPMNPerfMetrics> listaMetrics = new LinkedList<BPMNPerfMetrics>();

		for( PerformanceResult performance: totalPerformanceresult.getListperformance()){

			BPMNPerfMetrics metric = annotateMetricPerformance(performance,net,placeFlowCollection,bpmn);
			listaMetrics.add(metric);
		}



		return listaMetrics;
	}


	private BPMNPerfMetrics annotateMetricPerformance(
			PerformanceResult performance, Petrinet net,
			Collection<Place> placeFlowCollection, BPMNDiagram bpmn) {


		BPMNPerfMetrics metric = new BPMNPerfMetrics(performance.getTraceName());



		for (Transition t : net.getTransitions()) {
			if (!t.isInvisible() &&  !t.getLabel().startsWith("ArtificialEnd") ) {
				String tname = t.getLabel();
				String name = (String) tname.subSequence(0, tname.indexOf("+"));
				Activity activity = null;
				// cerco l'attività bpmn a cui collegare l'artifacts
				for (Activity a : bpmn.getActivities()) {
					if (a.getLabel().equals(name)) {
						activity = a;
						break;
					}
				}
				Place preplace = (Place) t.getGraph().getInEdges(t).iterator()
						.next().getSource();

				TaskPerfMetrics taskmetric = new TaskPerfMetrics();

				//PerformanceData ps = getPerfResult(preplace, performance.getList());
				PerformanceData ps = performance.getList().get(preplace);
				if (ps != null) {
					if (t.getLabel().endsWith("start")) {
						if (ps.getWaitTime() > 0) {
							taskmetric.setActivationTime(ps.getWaitTime());

						}
					} else if (t.getLabel().endsWith("complete")) {
						if (ps.getWaitTime() > 0) {
							taskmetric.setExecutionTime(ps.getWaitTime());

						}
					}
					String label = activity.getLabel();

					if (activity != null && !taskmetric.isEmpty()) {


						if(!metric.getTaskMetrics().containsKey(label)){

							metric.addTaskMetrics(label, taskmetric);



						}else{

							TaskPerfMetrics exmetric = metric.getTaskMetrics().get(label);
							exmetric.updateMetric(taskmetric);

						}


					}


				}
			} else {
				//t.getLabel().endsWith("_join")
				if (t.getGraph().getInEdges(t).size()>1) {

					// controlla la presenza di sync time e inserisce il souj
					// per ogni ramo parallelo

					for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> s: t.getGraph().getInEdges(t) ){
						if(s instanceof Arc){
							Arc a = (Arc) s;
							Place p = (Place) a.getSource();
							PerformanceData ps = performance.getList().get(p);
							if(ps.getSynchTime()>0){
								JoinPerfMetrics join = new JoinPerfMetrics();
								join.setSynchronizationTime(p.getLabel(), ps.getSynchTime());
								metric.addJoinMetrics(t.getLabel(), join);
							}

						}
					}


				}
			}

		}



		// i sync time sono sempre sulle piazze "arco", quindi cerco l'arco a
		// cui si riferisco i place con sync time ed aggiungo
		// il tooltip all'arco e lo coloro di rosso.



		return metric;
	}




}




