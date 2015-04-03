package org.processmining.plugins.bpmn;
import info.clearthought.layout.TableLayout;

import com.fluxicon.slickerbox.factory.SlickerFactory;




import javax.swing.JComboBox;
import javax.swing.JPanel;

public class BPMNLifecycleUI extends JPanel {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JComboBox SelectionCbBox;
	/**
	 * 
	 */

	public BPMNLifecycleUI(){
		initComponents();
	}
	
	public void  initComponents() {
		
		

		SlickerFactory slickerFactory = SlickerFactory.instance();

		double size[][] = { { TableLayout.FILL,0.3 }, { TableLayout.FILL,0.3  } };
		setLayout(new TableLayout(size));

		
		
		
		//JLabel label = slickerFactory.createLabel(stringlabel);
		//add(label,"0,0");
		String[] LifecycleStrings = { "Start-Complete", "Only-Complete"};
		SelectionCbBox = slickerFactory.createComboBox(LifecycleStrings);
		
		add(SelectionCbBox,"0,0");
		
	}
	
	public int getselectcb(){
		return SelectionCbBox.getSelectedIndex();
	}
	
}





/*public class ReplayAnalysisUI extends JPanel {

	*//**
	 * 
	 *//*
	private static final long serialVersionUID = -7950570396098585391L;

	private final Map<ReplayAction, NiceIntegerSlider> sliderMap;
	private final Map<ReplayAction, JCheckBox> checkBoxMap;
	private final ReplayFitnessSetting setting;

	public ReplayAnalysisUI(ReplayFitnessSetting setting) {
		sliderMap = new HashMap<ReplayAction, NiceIntegerSlider>();
		checkBoxMap = new HashMap<ReplayAction, JCheckBox>();
		this.setting = setting;
	}

	*//**
	 * Initialize user interface
	 *//*
	public JComponent initComponents() {
		JPanel panel = new JPanel();
		SlickerFactory slickerFactory = SlickerFactory.instance();

		double size[][] = { { TableLayoutConstants.FILL, 30, TableLayoutConstants.FILL },
				{ 30, 30, 30, 30, 30, 30, 30 } };
		panel.setLayout(new TableLayout(size));

		ChangeListener changeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				
				for (ReplayAction action : ReplayAction.values()) {
					sliderMap.get(action).setVisible(checkBoxMap.get(action).isSelected());
				}
			}
		};

		for (ReplayAction action : ReplayAction.values()) {
			sliderMap.put(action, slickerFactory.createNiceIntegerSlider("", 1, 1000, setting.getWeight(action),
					Orientation.HORIZONTAL));
			sliderMap.get(action).setPreferredSize(new Dimension(220, 20));
			checkBoxMap.put(action, slickerFactory.createCheckBox("", setting.isAllowed(action)));
			checkBoxMap.get(action).addChangeListener(changeListener);
			panel.add(slickerFactory.createLabel("<html><h3>" + action.getLabel() + "</h3>"), "0, "
					+ (action.getValue() + 1));
			panel.add(checkBoxMap.get(action), "1, " + (action.getValue() + 1));
			panel.add(sliderMap.get(action), "2, " + (action.getValue() + 1));
		}

		changeListener.stateChanged(null);

		return panel;
	}

	*//**
	 * return weight configuration chosen by user
	 *//*
	public void setWeights() {
		for (ReplayAction action : ReplayAction.values()) {
			setting.setWeight(action, sliderMap.get(action).getValue());
			setting.setAction(action, checkBoxMap.get(action).isSelected());
		}
	}
}*/
