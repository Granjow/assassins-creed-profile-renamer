package net.granjow.bytedisplay;

import java.lang.Runnable;

import javax.swing.text.JTextComponent;

public class ByteDisplayThread extends Thread {

    private JTextComponent jtc;
    private byte[] bytes;
    private int delay;
    private float multiplicator;

    public ByteDisplayThread(JTextComponent jtc, int delay, float multiplicator, String text) {
	this.jtc = jtc;
	this.delay = delay;
	this.multiplicator = multiplicator;
	setByteText(text);
    }
    public ByteDisplayThread(JTextComponent jtc, int delay, float multiplicator, byte[] bytes) {
	this.jtc = jtc;
	this.delay = delay;
	this.multiplicator = multiplicator;
	setByteText(bytes);
    }
    public void setByteText(byte[] bytes) {
	this.bytes = bytes;
    }
    public void setByteText(String text) {
	bytes = new byte[text.length()];
	for (int i = 0; i < text.length(); i++) {
	    bytes[i] = (byte)text.charAt(i);
	}
    }

    public void run() {
	System.err.println("ByteDisplayThread is being run.");
	String currentText = "";
	int currentChar, targetChar;
	char c;
	System.err.println("Length: " + bytes.length);
	for (int i = 0; i < bytes.length; i++) {
	    targetChar = ((int)((char)bytes[i]))&0xff;
	    for (currentChar = 0; currentChar < targetChar; currentChar++) {
		jtc.setText(String.format("%s%02x ", currentText, currentChar));
		try {
		    Thread.sleep(delay);
		} catch (InterruptedException e) {}
	    }
	    try {
		Thread.sleep((int)(multiplicator*delay));
	    } catch (InterruptedException e) {}
	    currentText += String.format("%02x ", targetChar);
	    jtc.setText(currentText);
	}
    }

}