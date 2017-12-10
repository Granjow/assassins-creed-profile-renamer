package net.granjow.animation;

import javax.swing.JPanel;

public class FPSThread extends Thread {

    private final SlideContainer panel;
    private volatile int fps = 30;
    private volatile long duration = 1000;
    private volatile int counter;
    private long startTime;
    private volatile long endTime;

    public FPSThread(SlideContainer panel, int fps, long duration) {
	this.panel = panel;
	this.fps = fps;
	this.duration = duration;
    }

    public void setAnimationTime(long duration) {
	this.duration = duration;
    }

    public void run() {
	startTime = System.currentTimeMillis();
	endTime = startTime + duration;

	float dt = (1000/fps);
	counter = 0;
	long wait;

	do {
	    panel.repaint();
	    counter++;
	    wait = (int)((counter * dt) - (System.currentTimeMillis() - startTime));
	    if (wait < 0) {
		wait = 10;
		System.err.println("Few time");
	    }

	    try {
		Thread.sleep((int) wait);
	    } catch (InterruptedException e) {}
	} while (System.currentTimeMillis() < endTime);

	panel.repaint();
	panel.animationCleanup();

    }

}