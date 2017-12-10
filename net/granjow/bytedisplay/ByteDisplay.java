package net.granjow.bytedisplay;

import java.lang.Thread;
import javax.swing.JTextArea;

public class ByteDisplay extends JTextArea {

    private Thread thread;
    private final int delay;

    public ByteDisplay(int delay) {
	super();
	this.delay = delay;
    }

    public void showText(String text) {
	thread = new Thread(new ByteDisplayThread(this, delay, 50, text));
	thread.start();
    }

}