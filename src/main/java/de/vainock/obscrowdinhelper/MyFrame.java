package de.vainock.obscrowdinhelper;

import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

public class MyFrame extends JFrame {

	final JPasswordField jPasswordField = new JPasswordField(20);
	final JButton jButton = new JButton();
	private final Thread currentThread = Thread.currentThread();

	public MyFrame() {
		setTitle("OBSCrowdinHelper");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Dimension minSize = new Dimension(600, 100);
		setSize(minSize);
		setMinimumSize(minSize);
		JPanel panel = new JPanel();
		panel.add(new JLabel("Personal Access Token:"));
		panel.add(jPasswordField);
		panel.add(jButton);
		getContentPane().add(panel);
		setVisible(true);
	}

	void waitForButtonPress() throws Exception {
		new Thread(() -> this.jButton.addActionListener(e -> {
			synchronized (currentThread) {
				currentThread.notify();
			}
		})).start();
		synchronized (currentThread) {
			currentThread.wait();
		}
	}
}
