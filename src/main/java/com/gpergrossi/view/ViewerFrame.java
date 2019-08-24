package com.gpergrossi.view;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class ViewerFrame extends JFrame {

	private static final long serialVersionUID = -3995478660714859610L;

	private ViewerPanel viewerPanel;

	/**
	 * Create the frame.
	 */
	public ViewerFrame(View view) {
		setTitle("Viewer");
		setVisible(true);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		viewerPanel = new ViewerPanel(view);
		view.setViewerPane(viewerPanel);
		
		viewerPanel.setFocusable(true);
		setContentPane(viewerPanel);
		pack();
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				ViewerFrame.this.close();
			}
		});
		
		viewerPanel.requestFocus(false);
		viewerPanel.start();
	}

	public void close() {
		System.out.println("Closing");
		this.viewerPanel.stop();
		this.dispose();
	}

}
