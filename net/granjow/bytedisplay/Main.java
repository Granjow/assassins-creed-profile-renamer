package net.granjow.bytedisplay;

import java.awt.Dimension;
import javax.swing.JFrame;

public class Main extends JFrame {

    private ByteDisplay disp = null;
    private Dimension SIZE = new Dimension(500, 300);

    public Main() {
	super();
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	setLayout(null);
	getContentPane().add(getDisp());
	setSize(SIZE);

	setVisible(true);
    }
    public void setText(String text) {
	getDisp().showText(text);
    }
    
    private ByteDisplay getDisp() {
	if (disp == null) {
	    disp = new ByteDisplay(1);
	    disp.setBounds(50, 50, (int)SIZE.getWidth()-100, (int)SIZE.getHeight()-100);
	}
	return disp;
    }

    public static void main(String[] args) {
	Main m = new Main();
	m.setText("Test für AC");
    }
}