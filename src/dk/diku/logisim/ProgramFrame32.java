/* The following is adapted by kwalsh@cs.cornell.edu from HexFrame.java.
 * Original copyright follows.
 * 
 * Copyright (c) 2006, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
 
package dk.diku.logisim;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.File;
import java.io.IOException;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.cburch.hex.HexEditor;
import com.cburch.hex.HexModel;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.util.WindowMenuItemManager;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;

public class ProgramFrame32 extends JFrame {

    private class WindowMenuManager extends WindowMenuItemManager {
        WindowMenuManager() { super("MIPS Program Listing", false); }
        public JFrame getJFrame(boolean create) { return ProgramFrame32.this; }
    }
	
	private class Change extends Action {
		String oldsrc;
		String newsrc;
		File file;
		boolean completed = false;
        
        Change(String oldsrc, String newsrc) {
			this.oldsrc = oldsrc;
			this.newsrc = newsrc;
        }

        public String getName() { return "Load MIPS Program"; }

        public void doIt(Project proj) {
            if (completed) return;
			completed = true;
			try {
			    code.setSource(newsrc);
			} catch (IOException e) { }
			Program32.State state = code.getState();
			if (state != null) state.codeChanged();
			model.fireChanged();
        }

        public void undo(Project proj) {
            if(!completed) return;
			completed = false;
			try {
			    code.setSource(oldsrc);
			} catch (IOException e) { }
			Program32.State state = code.getState();
			if (state != null) state.codeChanged();
			model.fireChanged();
        }
        
    }

    private class MyListener implements ActionListener {
        private File lastFile = null;
        
        public void actionPerformed(ActionEvent event) {
            Object src = event.getSource();
            if (src == load) {
                JFileChooser chooser = new JFileChooser();
                if (lastFile != null) chooser.setSelectedFile(lastFile);
                chooser.setDialogTitle("Load MIPS Program");
                int choice = chooser.showOpenDialog(ProgramFrame32.this);
                if(choice == JFileChooser.APPROVE_OPTION) {
                    File f = chooser.getSelectedFile();
                    try {
			String oldsrc = code.src;
			code.load(f);
			String newsrc = code.src;
			Program32.State state = code.getState();
			if (state != null) {
				state.codeChanged(); // update output
				Project proj = state.getProject(); 
				if (proj != null)
					proj.doAction(new Change(oldsrc, newsrc)); // sets undo action, refreshes screen
			}
                    } catch(IOException e) {
                        JOptionPane.showMessageDialog(ProgramFrame32.this, e.getMessage(),
                                "Error loading MIPS program" , JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else if(src == close) {
                WindowEvent e = new WindowEvent(ProgramFrame32.this,
                        WindowEvent.WINDOW_CLOSING);
                ProgramFrame32.this.processWindowEvent(e);
            }
        }
    }
    
    private WindowMenuManager windowManager = new WindowMenuManager();
    private MyListener myListener = new MyListener();
	private Program32.Listing code;
	private ListingModel model;
	private JTable listing;

    private JButton load = new JButton();
    private JButton close = new JButton();

	private class ListingModel extends AbstractTableModel {
		public int getColumnCount() { return 3; }
		public int getRowCount() { return code.src_lines.size(); }
		public Object getValueAt(int row, int col) {
			try {
				if (col == 0) {
					Integer i = (Integer)code.addr_map.get(row);
					if (i == null) return "";
					else return StringUtil.toHexString(32, i.intValue());
				} else if (col == 1) {
					Integer i = (Integer)code.addr_map.get(row);
					if (i == null) return "";
					int instr = code.instr(i.intValue()/4);
					return StringUtil.toHexString(32, instr);
				}
				else return code.src_lines.get(row);
			} catch (Exception e) {
				return "???";
			}
		}
		public void fireChanged() {
			fireTableStructureChanged();
		}
		public String getColumnName(int col) {
			switch(col) {
			    case 0: return "Address";
			    case 1: return "Binary";
			    default: return "Assembly";
			}
		}
	}

    public ProgramFrame32(Program32.Listing code) {
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        
        this.code = code;

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(load);
        buttonPanel.add(close);
        load.addActionListener(myListener);
        close.addActionListener(myListener);

		setTitle("MIPS Program Listing");
		load.setText("Load Program...");
		close.setText("Close Window");
      
	    model = new ListingModel();
	    listing = new JTable(model);
		Font font = new Font("Monospaced", Font.PLAIN, 12);
		listing.setFont(font);
		FontMetrics fm = listing.getFontMetrics(font);
		int w = fm.stringWidth("00000000");
		listing.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		TableColumn tc = listing.getColumnModel().getColumn(0);
		tc.setMaxWidth(w+10);
		tc.setMinWidth(w+10);
		tc = listing.getColumnModel().getColumn(1);
		tc.setMaxWidth(w+10);
		tc.setMinWidth(w+10);
		listing.setShowHorizontalLines(false);
		listing.setShowVerticalLines(true);
        JScrollPane scroll = new JScrollPane(listing,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(400, 300));
        //scroll.getViewport().setBackground(editor.getBackground());

        Container contents = getContentPane();
        contents.add(scroll, BorderLayout.CENTER);
        contents.add(buttonPanel, BorderLayout.SOUTH);

        pack();

    }
    
    public void setVisible(boolean value) {
        if(value && !isVisible()) windowManager.frameOpened(this);
        super.setVisible(value);
    }
}
