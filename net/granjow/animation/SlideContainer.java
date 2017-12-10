package net.granjow.animation;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import java.io.File;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.log10;
import static java.lang.Math.sqrt;
import static java.lang.Math.PI;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JPanel;

/**
 * Example implementation:
 * <code>*
public static class TestWindow extends JDialog {

    public TestWindow(String s, int width, int height) {
	super();
	setContentPane(new SlideContainer(s, 3, width, height, Animation.HILL, 30));
	setSize(width, height);
	setVisible(true);
	addKeyListener( new KeyListener() {
		public void keyPressed(KeyEvent e) {}
		public void keyReleased(KeyEvent e) {
		    switch (e.getKeyCode()) {
		    case KeyEvent.VK_LEFT:
			((SlideContainer)getContentPane()).showLeft();
			break;
		    case KeyEvent.VK_RIGHT:
			((SlideContainer)getContentPane()).showRight();
			break;
		    case KeyEvent.VK_ESCAPE:
			System.exit(0);
			break;
		    }
		}
		public void keyTyped(KeyEvent e) {}
	    });
    }

    public void showPart(int i) {
	((SlideContainer)getContentPane()).showPart(i);
    }
    public void setAnimation(Animation a) {
	((SlideContainer)getContentPane()).setAnimationType(a);
    }
}
 * </code>
 */
public class SlideContainer extends JPanel {

    private final Image pic;
    private final Dimension windowSize;
    private final int size;

    private long animationTime = 250;
    private int _fps = 30;

    private FPSThread fpsThread;
    private FakeThread fakeThread;

    private boolean animate = false;
    private long animationStart = 0;
    private int animationSrc = 0;
    private volatile int animationDest = 0;
    private Animation animationType = Animation.LINEAR;

    private MiniIntQueue q;

    /** The available animations */
    public enum Animation {
	LINEAR, COSINE, COSINE_FULL, SINE, SINE2, SINE_, SQUARE, HILL
    }

    /** 
     * Creates a new slide container.
     * @param imgPath The path to the background image
     * @param size Number of parts that can be shown
     * @param width Width of each part
     * @param height Height of the image
     * @param animationType Which animation type to take (linear, cosine, etc.)
     * @param fps Frames per second to (try to) use
     */
    public SlideContainer(String imgPath, int size, int width, int height, Animation animationType, int fps) {
	this(new ImageIcon(imgPath), size, width, height, animationType, fps);
    }
    /** 
     * Creates a new slide container.
     * @param ii The background ImageIcon
     * @param size Number of parts that can be shown
     * @param width Width of each part
     * @param height Height of the image
     * @param animationType Which animation type to take (linear, cosine, etc.)
     * @param fps Frames per second to (try to) use
     */
    public SlideContainer(ImageIcon ii, int size, int width, int height, Animation animationType, int fps) {
	super(true);
	this.size = size;
	this.animationType = animationType;

	_fps = fps;
	fpsThread = new FPSThread(this, _fps, animationTime);
	pic = ii.getImage();
	windowSize = new Dimension(width, height);
	q = new MiniIntQueue(size);

	System.out.println("Icon height: " + ii.getIconHeight());
    }
    /**
     * Sets a new animation type.
     * @param animationType Type of the animation
     */
    public void setAnimationType(Animation animationType) {
	this.animationType = animationType;
    }

    /**
     * <p>Paints the currently visible part of the image.</p>

     * <p>If the animation has been started, the position will be calculated
     * by looking at the passed time, the total time an animation should take,
     * and the selected transition.</p>
     * @see #setAnimationType
     */
    public void paintComponent(Graphics g) {
	g.clearRect(0, 0, (int)windowSize.getWidth(), (int)windowSize.getHeight());

	int x = animationSrc * (int)windowSize.getWidth();
	long timeDiff = System.currentTimeMillis() - animationStart;

	//System.err.print("src: " + animationSrc + "; dst: " + animationDest + "\t");
	if (timeDiff < animationTime) {
	    int d = (animationDest - animationSrc) * (int) windowSize.getWidth();
	    float completedFrac = (float)(System.currentTimeMillis() - animationStart) / animationTime;
	    double multiplier;

	    switch (animationType) {
	    case LINEAR:
		multiplier = completedFrac;
		break;
	    case COSINE:
		multiplier = cos(PI/2 * completedFrac);
		break;
	    case COSINE_FULL:
		multiplier = cos(PI * completedFrac)/2;
		break;
	    case SINE:
		multiplier = sin(PI/2 * completedFrac);
		break;
	    case SINE2:
		multiplier = sin(PI/2 * completedFrac) * sin(PI/2 * completedFrac);
		break;
	    case SINE_:
		multiplier = Math.pow(sin(PI/2 * completedFrac), 1.5);
	    case SQUARE:
		multiplier = sqrt(completedFrac);
		break;
	    case HILL:
		multiplier = -2.5*completedFrac*completedFrac + 3.5*completedFrac;
		break;
	    default:
		multiplier = completedFrac;
		break;
	    }
	    System.err.print("\tMultiplier: " + animationType + " = " + multiplier);
	    x += multiplier * d;
	}
	x = -x;
	System.err.println("\tx is " + x);
	g.drawImage(pic, x, 0, null);
    }

    /**
     * Switches to the given part dest. Switching will be animated.
     * @param dest Target part
     * @return Success. May be successless if an animation is already in progress.
     * @see #showLeft
     * @see #showRight
     */
    public boolean showPart(int dest) {
	System.err.print("Thread state: " + fpsThread.getState());
	if (fpsThread.isAlive() || fpsThread.isInterrupted()) {
	    // Put the next part into the queue.
	    if (dest == animationSrc) {}
	    else {
		System.err.println("\tLiving/interrupted, putting on Queue (" + animationSrc + "/" + dest + ")");
		if (dest < animationSrc) {
		    q.put(MiniIntQueue.Direction.LEFT);
		} else {
		    q.put(MiniIntQueue.Direction.RIGHT);
		}
	    }

	} else {
	    if (dest < 0 || dest >= size) {
		// Do nothing.
	    } else if (fpsThread.getState() == Thread.State.TERMINATED
		|| fpsThread.getState() == Thread.State.NEW) {
		animationStart = System.currentTimeMillis();

		animationDest = dest;
		System.err.println("\tStarting new thread " + animationSrc + "->" + animationDest);
		fpsThread = new FPSThread(this, _fps, animationTime);
		fpsThread.start();
		return true;
	    }
	}
	return false;
    }
    /**
     * Has to be called at the end of the animation
     * to put the image to its final position.
     */
    public void animationCleanup() {
	System.err.println("Cleanup called: " + animationDest);
	animationSrc = animationDest;
	repaint();

	/**
	 * Allow fpsThread to terminate
	 */
	fakeThread = new FakeThread(fpsThread, this);
	fakeThread.start();
    }
    /**
     * Switches to the left part, if possible.
     * @return Success; May be successless if the first part 
     * is already shown or if an animation is in progress.
     * @see #showRight
     * @see #showPart
     */
    public boolean showLeft() {
	return showPart(animationSrc-1);
    }
    /**
     * Switches to the right part, if possible.
     * @return Success; May be successless if the last part 
     * is already shown or if an animation is in progress.
     * @see #showRight
     * @see #showPart
     */
    public boolean showRight() {
	return showPart(animationSrc+1);
    }
    /**
     * Tries to do show another part
     * if there is one waiting in the queue.
     */
    public boolean workQueue() {
	if (animationSrc <= 0) {
	    q.purgeElements(MiniIntQueue.Direction.LEFT);
	}
	if (animationSrc >= (size-1)) {
	    q.purgeElements(MiniIntQueue.Direction.RIGHT);
	}
	try {
	    MiniIntQueue.Direction newDirection = q.get();
	    int newPart = animationSrc + 1*newDirection.value;
	    if (newPart >= 0 && newPart < size) {
		System.err.println("More work to do: " + newDirection);
		return showPart(newPart);
	    } else {
		System.err.println("Index out of bounds: " + newPart);
		// out of bounds
	    }
	} catch (MiniIntQueue.QueueEmptyException e) {
	    System.err.println("\t" + e.getMessage());
	}
	return false;
    }

    /**
     * @return The index of the currently visible (i.e. selected) part.
     */
    public int getSelectedPart() {
	return animationSrc;
    }








    /*
     * Classes
     */


    /**
     * A thread which allows the FPSThread to terminate
     * and start another FPSThread from the queue.
     */
    private class FakeThread extends Thread {

	private final FPSThread otherThread;
	private final SlideContainer container;

	public FakeThread(FPSThread otherThread, SlideContainer container) {
	    this.otherThread = otherThread;
	    this.container = container;
	}

	public void run() {
	    // Wait until the other thread has finished
	    try {
		otherThread.join();
		System.err.print("Joined other thread");
	    } catch (InterruptedException e) {}
	    System.err.println("\ttrying to work Queue.");
	    container.workQueue();
	}

    }

    /**
     * Kind of a buffer if the user wants to show another part 
     * while the animation is still playing. Remember the part
     * to show and display it after the animation has finished.
     */
    private static class MiniIntQueue {

	public final int size;

	public enum Direction {
	    LEFT(-1), RIGHT(1);

	    public final int value;
	    Direction(int value) {
		this.value = value;
	    }

	}

	private Direction[] array;
	private int n = 0, start = 0;
	
	public MiniIntQueue(int size) {
	    this.size = size;
	    array = new Direction[size];
	}

	public void put(Direction dir) {
	    if (n < size) {
		array[(start + n) % size] = dir;
		n++;
	    }
	}
	public Direction get() throws QueueEmptyException {
	    if (n > 0) {
		Direction val = array[start];
		n--;
		start++;
		start %= size;
		return val;
	    } else {
		throw new QueueEmptyException();
	    }
	}
	/**
	 * Purges all elements at the end of the queue
	 * which are of the same type. Has to be called
	 * when the first/last slide element has been reached.
	 */
	public void purgeElements(Direction like) {
	    try {
		while (n > 0 && array[start] == like) get();
	    } catch (QueueEmptyException e) {}
	}

	public class QueueEmptyException extends Exception {
	    public QueueEmptyException() {
		super("Queue is empty!");
	    }
	}

    }


}