package org.processmining.plugins.petrinet;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Dimension;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.processmining.framework.util.ui.widgets.ProMList;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.plugins.bpmn.parameters.BpmnSelectDiagramParameters;


public class PNSelectDiagramDialog extends JPanel {
	int index=0;
	
	public PNSelectDiagramDialog(ArrayPetriNet pn) {
		double size[][] = { { TableLayoutConstants.FILL }, { TableLayoutConstants.FILL } };
		setLayout(new TableLayout(size));
		
		final JPanel panel = new JPanel();
		double panelSize[][] = { { TableLayoutConstants.FILL }, { TableLayoutConstants.FILL } };
		panel.setLayout(new TableLayout(panelSize));
		
		final DefaultListModel listModel = new DefaultListModel();
		 Petrinet last = null;
		for (Petrinet petrin: pn.getListpn()) {
			last = petrin;
			listModel.addElement(last);
		}
		final ProMList list = new ProMList("Select diagram", listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelection(last);
		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				List<Petrinet> selected = list.getSelectedValuesList();
				if (selected.size() == 1) {
					index = listModel.indexOf(selected.get(0));
					
				} else {
					index =0 ;
				}
			}
		});
		list.setPreferredSize(new Dimension(100, 100));
		add(list, "0, 0");
	}

	/**s
	 * 
	 */
	private static final long serialVersionUID =1L;

	public int getSelected() {
		// TODO Auto-generated method stub
		return index;
	}
	
	

}
