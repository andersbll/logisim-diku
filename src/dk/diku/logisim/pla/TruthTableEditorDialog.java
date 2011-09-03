package dk.diku.logisim.pla;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import java.awt.*;
import java.awt.event.*;

@SuppressWarnings("serial")public class TruthTableEditorDialog extends JDialog{
	private final Container cPane;
	private TruthTablePanel ttPanel;
	private final float fontSize = 8.8f;
	
	public TruthTableEditorDialog(Frame parent){
		super(parent, "", true);
		setSize(200,200);
		setLocation(300,200);
		setResizable(false);
		
		cPane = super.getContentPane();
		cPane.setLayout(new BorderLayout(5,5));
		
		createTablePanel();
		createButtonPanel();
	}
	
	public void createTablePanel(){
		cPane.add(ttPanel = new TruthTablePanel(), "Center");
	}
	
	public void createButtonPanel(){
		JPanel p = new JPanel();
		JButton b;

		b = new JButton("Ok");
		b.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				close(false);
			}
		});
		this.getRootPane().setDefaultButton(b);
		p.add(b);
		
		b = new JButton("Annuller");
		b.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				close(true);
			}
		});
		p.add(b);

		this.getRootPane().registerKeyboardAction(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				close(true);
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0), JComponent.WHEN_IN_FOCUSED_WINDOW);

		
		cPane.add(p, "South");
	}
	
	private TruthTable tt;
	
	private void close(boolean discard){
		if(!discard){
			tt.copyFrom(ttPanel.temporary);
		}
		
		setVisible(false);
	}
	
	
	public void showAndSet(TruthTable t, int tmpInSz, int tmpOutSz){
		tt = t;
		ttPanel.reset(t, tmpInSz, tmpOutSz);
		
		pack();
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(dim.width/2-getWidth()/2, dim.height/3-getHeight()/2);
		
		setVisible(true);
	}

	
	class TruthTablePanel extends JPanel{
		private TruthTable temporary;
		
		TruthTablePanel(){
			super();
			super.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		}
		
		/**
		 * Create a temporary copy of the truthtable, and set up visuals
		 * @param tt
		 */
		void reset(TruthTable tt, int tmpInSz, int tmpOutSz){
			temporary = tt.clone();
			temporary.resize(tmpInSz, tmpOutSz);
			removeAll();
			
			add(new TopRowPanel(tmpInSz, tmpOutSz));
			
			for(int r = 0; r<temporary.getRowCount();r++){
				add(new TruthTableRowPanel(temporary.getRow(r)));
			}
			add(new InsertRowPanel());
		}
	
		private void addRow(){
			temporary.addNewRow();
			add(new TruthTableRowPanel(temporary.getLastRow()) , getComponentCount()-1);
			pack();
		}
		private void deleteRow(TruthTableRowPanel row){
			temporary.deleteRow(row.ttRow);
			this.remove(row);
			pack();
		}
		
		
		
		class TruthTableRowPanel extends JPanel implements ActionListener{
			private final TruthTableRow ttRow;
			private final JTextField commentField;
			TruthTableRowPanel(TruthTableRow row){
				super(new FlowLayout());
				this.ttRow = row;
				commentField = new JTextField(null, ttRow.getComment(), 10);
				commentField.addKeyListener(new KeyAdapter(){
					public void keyReleased(KeyEvent e){
						ttRow.setComment(commentField.getText());
					}
				});
				
				JButton delButton = new JButton("Remove row");
				delButton.setFont(delButton.getFont().deriveFont(fontSize));
				delButton.addActionListener(this);
				int mUD = 1;
				int mLR = 1;
				delButton.setMargin(new Insets(mUD, mLR, mUD, mLR));
				add(delButton);
				
				
				JPanel bitPanel = new JPanel(new GridLayout(1, ttRow.getInSize()+ttRow.getOutSize()+2));
				bitPanel.add(new Box(BoxLayout.X_AXIS));
				for(int c=0;c<ttRow.getInSize();c++){
					bitPanel.add(new BitStateButton(ttRow.getInBit(c)));
				}
				bitPanel.add(new Box(BoxLayout.X_AXIS));
				for(int c=0;c<ttRow.getOutSize();c++){
					bitPanel.add(new BitStateButton(ttRow.getOutBit(c)));
				}
				
				add(bitPanel);
				add(commentField);
			}

			/**
			 * Fired from the deletebutton
			 */
			public void actionPerformed(ActionEvent e){
				deleteRow(this);
			}
		}
		
		class InsertRowPanel extends JPanel implements ActionListener{
			public InsertRowPanel(){
				//super(new FlowLayout(FlowLayout.LEFT));
				super(new FlowLayout(FlowLayout.CENTER));
				JButton addButton = new JButton("Add row");
				addButton.setFont(addButton.getFont().deriveFont(fontSize));
				addButton.addActionListener(this);
				int mUD = 1;
				int mLR = 20;
				addButton.setMargin(new Insets(mUD, mLR, mUD, mLR));
				add(addButton);
			}

			/**
			 * Fired from the addbutton
			 */
			public void actionPerformed(ActionEvent e){
				addRow();
			}
		}

	}
	
	class TopRowPanel extends JPanel{
		TopRowPanel(int inSz, int outSz){
			super();

			Dimension bDim = new Dimension(25,15);
			
			//Replacement for the button
			add(Box.createRigidArea(new Dimension(10,15)));
			add(new JLabel("Bits:"));
			add(Box.createRigidArea(new Dimension(30,15)));
			//add(Box.createRigidArea(new Dimension(29,15)));
			
			
			JPanel bitPanel = new JPanel(new GridLayout(1, inSz + outSz + 2));
			bitPanel.add(new Box(BoxLayout.X_AXIS));
			for(int c=inSz-1;c>=0;c--){
				JLabel l = new JLabel(""+c);
				l.setPreferredSize(bDim);
				bitPanel.add(l);
			}
			bitPanel.add(new Box(BoxLayout.X_AXIS));
			for(int c=outSz-1;c>=0;c--){
				JLabel l = new JLabel(""+c);
				l.setPreferredSize(bDim);
				bitPanel.add(l);
			}
			
			add(bitPanel);
			
			add(new JLabel("Comments"));
			add(Box.createRigidArea(new Dimension(30,15)));
		}
	}	

	//private static final Border stdBorder = BorderFactory.createLineBorder(Color.BLACK);
	//private static final Border clickBorder =  BorderFactory.createLineBorder(Color.LIGHT_GRAY);
	private static final Border stdBorder = BorderFactory.createEtchedBorder(); 
	private static final Border clickBorder =  BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
	private static final Dimension buttonSize = new Dimension(11,11);
	class BitStateButton extends JPanel {
		private final BitState state;
		private final JLabel text;
		BitStateButton(BitState s){
			super();
			setBorder(stdBorder);
			state = s;
			text = new JLabel(state.toString());
			text.setPreferredSize(buttonSize);
			add(text);
			
			addMouseListener(new MouseAdapter(){
				public void mousePressed(MouseEvent e){	setBorder(clickBorder); }
				public void mouseReleased(MouseEvent e){	setBorder(stdBorder); }
				public void mouseClicked(MouseEvent e){
					state.nextState();
					text.setText(state.toString());
				}
			});
		}
	} 
}