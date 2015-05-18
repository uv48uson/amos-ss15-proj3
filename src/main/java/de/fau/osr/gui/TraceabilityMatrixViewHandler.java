package de.fau.osr.gui;
import java.awt.EventQueue;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;

import de.fau.osr.bl.RequirementsTraceabilityMatrix;
import de.fau.osr.bl.Tracker;
import de.fau.osr.core.vcs.impl.GitVcsClient;
import de.fau.osr.core.vcs.interfaces.VcsClient;
import de.fau.osr.gui.util.MatrixTableModel;
import de.fau.osr.util.matrix.MatrixIndex;

public class TraceabilityMatrixViewHandler extends JFrame {
	

	private JPanel contentPane;
	private JTable table;
	private JScrollPane scrollPane;
	private RequirementsTraceabilityMatrix requirementsTraceabilityMatrix;
	


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					TraceabilityMatrixViewHandler frame = new TraceabilityMatrixViewHandler(null);
					frame.initTable();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 * @throws IOException 
	 */
	public TraceabilityMatrixViewHandler(RequirementsTraceabilityMatrix requirementsTraceabilityMatrix) throws IOException {
		this.requirementsTraceabilityMatrix = requirementsTraceabilityMatrix;
		setTitle("SpiceTraceability-Requirement Traceability Matrix");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1157, 679);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		scrollPane = new JScrollPane();
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addContainerGap()
					.addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 1094, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(16, Short.MAX_VALUE))
		);
		gl_contentPane.setVerticalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addContainerGap()
					.addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 578, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(19, Short.MAX_VALUE))
		);
		
		
		contentPane.setLayout(gl_contentPane);
	}
	
	public void initTable()
	{
		
			final RequirementsTraceabilityMatrix traceabilityMatrix = this.requirementsTraceabilityMatrix;
			table = new JTable(new MatrixTableModel(traceabilityMatrix)){
				public String getToolTipText(java.awt.event.MouseEvent event){
					String toolTipText = null;
	                java.awt.Point p = event.getPoint();
	                int rowIndex = rowAtPoint(p);
	                int columIndex = columnAtPoint(p);

	                try {
	                	List<String> fileStringList = traceabilityMatrix.getTraceabilityMatrixForRequirements().getAt(new MatrixIndex(rowIndex, columIndex)).getFiles();
	                	StringBuilder toolTipTextBuilder = new StringBuilder();
	                	toolTipTextBuilder.append("<html>");
	                	for(String file:fileStringList){
	                		toolTipTextBuilder.append(file);
	                		toolTipTextBuilder.append("<br>");
	                		
	                	}
	                	toolTipTextBuilder.append("</html>");
	                	toolTipText = toolTipTextBuilder.toString();
	                } catch (RuntimeException e1) {
	                    //catch null pointer exception if mouse is over an empty line
	                }

	                return toolTipText;
				}
			};
			table.setCellSelectionEnabled(true);
			Font columnHeaderFont = new Font("Arial",Font.BOLD,10);
			table.getTableHeader().setFont(columnHeaderFont);
			scrollPane.setViewportView(table);
		
		
	}
}
