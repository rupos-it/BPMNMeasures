package org.processmining.plugins.bpmn.exporting;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;


import javax.swing.JComponent;
import javax.swing.JFileChooser;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;


import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;

import org.processmining.framework.connections.ConnectionCannotBeObtained;

import org.processmining.framework.plugin.events.Logger.MessageLevel;

import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramExt;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;

import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.jgraph.ProMJGraphVisualizer;
import org.processmining.models.jgraph.visualization.ProMJGraphPanel;

import org.processmining.plugins.bpmn.BPMNtoPNConnection;

import org.processmining.plugins.petrinet.replay.conformance.LegendConformancePanel;
import org.processmining.plugins.petrinet.replay.conformance.TotalConformanceResult;
import org.processmining.plugins.petrinet.replay.util.LogViewInteractivePanel;
import org.processmining.plugins.petrinet.replay.util.PetriNetDrawUtil;
import org.processmining.plugins.petrinet.replay.util.ReplayAnalysisConnection;
import org.processmining.plugins.xpdl.Xpdl;
import org.processmining.plugins.xpdl.converter.BPMN2XPDLConversionExt;



public class BPMNMeasuresPanelConformance extends JPanel{




	/**
	 * 
	 */

	private static final long serialVersionUID = -7258945029957912308L;
	private  ProMJGraphPanel netPNView;
	private  JComponent netBPMNView;
	
	private LegendConformancePanel legendInteractionPanel;
	private LogViewInteractivePanel logInteractionPanel;
	private Collection<Place> placeFlowCollection;
	private TotalConformanceResult tovisualize;
	private UIPluginContext context;
	private Petrinet net;
	
	
	private BPMNDiagram bpmn;
	private TabTraceConfPanel tabinteractivepanel;
	private XLog log;
	private BPMNDiagramExt bpmnext;


	

	
   public void fill(){
	   Petrinet netx = PetrinetFactory.clonePetrinet(net);
		PetriNetDrawUtil.drawconformance(netx,tovisualize.getTotal());
		
		netPNView = ProMJGraphVisualizer.instance().visualizeGraph(context, netx);
		legendInteractionPanel = new LegendConformancePanel(netPNView, "Legend");
		netPNView.addViewInteractionPanel(legendInteractionPanel, SwingConstants.NORTH);

		
		netBPMNView= ProMJGraphVisualizer.instance().visualizeGraph(context, bpmnext);

		//JComponent logView = new LogViewUI(log);
	
		logInteractionPanel = new LogViewInteractivePanel(netPNView, log);
		netPNView.addViewInteractionPanel(logInteractionPanel, SwingConstants.SOUTH);

		tabinteractivepanel = new TabTraceConfPanel(netPNView, "Trace_Sel", tovisualize, this);
		netPNView.addViewInteractionPanel(tabinteractivepanel, SwingConstants.SOUTH);
		
		
		double size[][] = { { TableLayoutConstants.FILL }, { TableLayoutConstants.FILL,  TableLayoutConstants.FILL} };
		setLayout(new TableLayout(size));
		
		add(netBPMNView, "0, 0");
		
		add(netPNView, "0, 1");
   }
	



	public BPMNMeasuresPanelConformance(UIPluginContext c,
			TotalConformanceResult resultc) {
		
		
		context=c;
		try {
			ReplayAnalysisConnection connection = context.getConnectionManager().getFirstConnection(
					ReplayAnalysisConnection.class, context, resultc);

			// connection found. Create all necessary component to instantiate inactive visualization panel
			 log = connection.getObjectWithRole(ReplayAnalysisConnection.XLOG);
			 net= connection.getObjectWithRole(ReplayAnalysisConnection.PNET);
		
			BPMNtoPNConnection connection2 = context.getConnectionManager().getFirstConnection(
					BPMNtoPNConnection.class, context, net);

			// connection found. Create all necessary component to instantiate inactive visualization panel
			
		    bpmn = connection2.getObjectWithRole(BPMNtoPNConnection.BPMN);
		    placeFlowCollection = connection2.getObjectWithRole(BPMNtoPNConnection.PLACEFLOWCONNECTION);
			
			bpmnext = BPMNDecorateUtil.exportConformancetoBPMN(bpmn, net, resultc.getTotal(), placeFlowCollection);
			
			 tovisualize = resultc;
				fill();


		} catch (ConnectionCannotBeObtained e) {
			// No connections available
			context.log("Connection does not exist", MessageLevel.DEBUG);
			
		}
	}







	public void savefile() {
		JFileChooser saveDialog = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
		        "XPDL", "xpdl");
		saveDialog.setFileFilter(filter);

		saveDialog.setSelectedFile(new File(bpmn.getLabel()+"Summary.xpdl")); 
		if (saveDialog.showSaveDialog(context.getGlobalContext().getUI()) == JFileChooser.APPROVE_OPTION) {
			File outFile = saveDialog.getSelectedFile();
			try {
				BufferedWriter outWriter = new BufferedWriter(new FileWriter(outFile));
				BPMN2XPDLConversionExt xpdlConversion = new BPMN2XPDLConversionExt(bpmnext);
				Xpdl newxpdl = xpdlConversion.fills_layout(context);
				outWriter.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +newxpdl.exportElement());
				outWriter.flush();
				outWriter.close();
				JOptionPane.showMessageDialog(context.getGlobalContext().getUI(),
						"BPMN has been saved\nto XPDL file!", "BPMN saved.",
						JOptionPane.INFORMATION_MESSAGE);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}




	public void updateone(int i) {
		Petrinet netx = PetrinetFactory.clonePetrinet(net);
		PetriNetDrawUtil.drawconformance(netx,tovisualize.getList().get(i));
		 bpmnext = BPMNDecorateUtil.exportConformancetoBPMN(bpmn,net, tovisualize.getList().get(i),placeFlowCollection);
		remove(netPNView);
		remove(netBPMNView);
		netPNView = ProMJGraphVisualizer.instance().visualizeGraph(context, netx);
		netPNView.addViewInteractionPanel(legendInteractionPanel, SwingConstants.NORTH);
		
		netPNView.addViewInteractionPanel(logInteractionPanel, SwingConstants.SOUTH);
		netPNView.addViewInteractionPanel(tabinteractivepanel, SwingConstants.SOUTH);
		
		netBPMNView = ProMJGraphVisualizer.instance().visualizeGraph(context, bpmnext);
		//add (netPNView, "1, 5, 5, 5");
		//add (netBPMNView, "1, 1, 5, 1");
		add(netBPMNView, "0, 0");
		
		add(netPNView, "0, 1");
		revalidate();
		repaint();
	}




	public void updateall() {
		Petrinet netx = PetrinetFactory.clonePetrinet(net);
		PetriNetDrawUtil.drawconformance(netx,tovisualize.getTotal());
		bpmnext = BPMNDecorateUtil.exportConformancetoBPMN(bpmn,net, tovisualize.getTotal(),placeFlowCollection);
		remove(netPNView);
		remove(netBPMNView);
		netPNView = ProMJGraphVisualizer.instance().visualizeGraph(context, netx);
		netPNView.addViewInteractionPanel(legendInteractionPanel, SwingConstants.NORTH);
		netPNView.addViewInteractionPanel(logInteractionPanel, SwingConstants.SOUTH);
		netPNView.addViewInteractionPanel(tabinteractivepanel, SwingConstants.SOUTH);
		
		netBPMNView = ProMJGraphVisualizer.instance().visualizeGraph(context, bpmnext);
		
		add(netBPMNView, "0, 0");
		
		add(netPNView, "0, 1");
		
		revalidate();
		repaint();
	}

	
	

}
