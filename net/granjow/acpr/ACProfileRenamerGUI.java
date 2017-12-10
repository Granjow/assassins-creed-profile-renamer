package net.granjow.acpr;

import net.granjow.bytedisplay.ByteDisplayThread;

import net.granjow.animation.SlideContainer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.Thread;
import java.net.URL;

import javax.swing.*;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowEvent;

public class ACProfileRenamerGUI extends JFrame {

    /** Dimension of the frame */
    public static final Dimension SIZE = new Dimension(840, 525);

    /** Background images */
    private static BackgroundImagePanel loadScreen, nameScreen, doneScreen, doneRestoreScreen, failedScreen;

    /** To select the task: Backup/Restore/Rename */
    private TaskSlider taskSlider = null;


    enum State {
	TASK, LOAD, NAME, DONE, FAILED, HELP, BACKUP_RENAME, RESTORE, TEMP;
    };
    private State currentState = State.LOAD;
    private State nextState = State.LOAD;
    private State prevState = State.LOAD;
    private boolean ignoreNextKey = false;


    private ImageFrame helpFrame = null;
    private JTextFieldC jtfName = null;
    private JTextAreaT jtfDProfilename = null;
    private JTextAreaT jtaDFilename = null;
    private JButtonI jbSave = null;
    private JTextAreaT jtaErrorMessage = null;
    private ACByteDisplay disp = null;


    private String sName = "";
    private ACProfileFile hdrFile;
    private ACProfileFile hdrTargetFile;
    private String errMsg = "";


    private static final String newline = System.getProperty("line.separator");
    private static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();




    public ACProfileRenamerGUI() {
	super("Assassin's Creed Profile Renamer");
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	setSize(SIZE);
	setUndecorated(true); // No title bar
	setResizable(false);
	setLayout(null); // We'll pixel around

	loadScreen = 
	    new BackgroundImagePanel(ACPR_Resources.i().iiLoad.getImage(), SIZE);
	nameScreen = 
	    new BackgroundImagePanel(ACPR_Resources.i().iiName.getImage(), SIZE);
	doneScreen = 
	    new BackgroundImagePanel(ACPR_Resources.i().iiDone.getImage(), SIZE);
	doneRestoreScreen = 
	    new BackgroundImagePanel(ACPR_Resources.i().iiDoneRestore.getImage(), SIZE);
	failedScreen = 
	    new BackgroundImagePanel(ACPR_Resources.i().iiFailed.getImage(), SIZE);


	addKeyListener(nextStateLogic());
	addMouseListener(mousie());
	addFocusListener(new FocusListener() {
		public void focusGained(FocusEvent e) {
		    System.err.println("Regained focus. " + currentState);
		}
		public void focusLost(FocusEvent e) {}
	    });
	addWindowFocusListener( new WindowFocusListener() {
		public void windowGainedFocus(WindowEvent e) {
		    System.err.print("Got the focus again. Updating state: ");
		    updateState();
		}
		public void windowLostFocus(WindowEvent e) {}
	    });

	setLocation((int)(screenSize.getWidth()-SIZE.getWidth())/2, (int)(screenSize.getHeight()-SIZE.getHeight())/2);
	paintLoad();
	setVisible(true);
    }


    /*
     * Listener
     */
    private KeyListener nextStateLogic() {
	return new KeyListener() {
	    public void keyReleased(KeyEvent e) {
		System.err.printf("(Key: %d) ", e.getKeyCode());
		if (ignoreNextKey) {
		    System.err.println("\tWill ignore this key.");
		    ignoreNextKey = false;

		} else if (e.getKeyCode() == KeyEvent.VK_F1) {
			if (currentState != State.HELP) {
			    getHelpFrame().setVisible(true);
			    nextState = State.HELP;
			    prevState = currentState;
			    System.err.println("Previous state was " + prevState);
			} else {
				System.err.println("Ignoring; F1 pressed again, no change required.");
			}

		} else if (nextState == State.TEMP) {
		    // Do nothing

		} else {
		    switch (currentState) {
		    case HELP:
			nextState = prevState;
			break;

		    case TASK:
			switch (e.getKeyCode()) {
			case KeyEvent.VK_RIGHT:
			    getTaskSlider().showRight();
			    break;
			case KeyEvent.VK_LEFT:
			    getTaskSlider().showLeft();
			    break;
			case KeyEvent.VK_Q:
			case KeyEvent.VK_ESCAPE:
			    nextState = State.LOAD;
			    break;
			case KeyEvent.VK_SHIFT:
			case KeyEvent.VK_ALT:
			case KeyEvent.VK_CONTROL:
			case KeyEvent.VK_META:
			case KeyEvent.VK_WINDOWS:
			case KeyEvent.VK_COMPOSE:
			case KeyEvent.VK_ALT_GRAPH:
			case KeyEvent.VK_CAPS_LOCK:
			case KeyEvent.VK_SCROLL_LOCK:
			case KeyEvent.VK_NUM_LOCK:
			case KeyEvent.VK_PAUSE:
			case KeyEvent.VK_PRINTSCREEN:
			case KeyEvent.VK_UP:
			case KeyEvent.VK_DOWN:
			    break;
			default:
			    switch (getTaskSlider().getSelectedPart()) {
			    case 0:
				getTaskSlider().dispose();
				nextState = State.TEMP;
				if (
				    loadFile()
				    && loadTargetFile()
				    ) {
				    try {
					hdrFile.copyFileGroup(ACPR_Resources.i().acDir, hdrTargetFile.getBasename(false));
					hdrFile = hdrTargetFile;
					System.err.println("Will show renaming screen.");
				    } catch (Exception ex) {
					errMsg += ex.getMessage();
					nextState = State.FAILED;
					break;
				    }
				    nextState = State.BACKUP_RENAME;
				} else {
				    //errMsg += "Error while trying to load file!";
				    System.err.println("Loading error!");
				    nextState = State.FAILED;
				}
				break;
			    case 1:
				getTaskSlider().dispose();
				if (loadBackupFile()) {
				    nextState = State.RESTORE;
				} else {
				    nextState = State.FAILED;
				}
				break;
			    case 2:
				getTaskSlider().dispose();
				if (loadFile()) nextState = State.NAME;
				break;
			    }
			    break;
			}
			break;

		    case LOAD:
			switch (e.getKeyCode()) {
			case KeyEvent.VK_ESCAPE:
			    System.exit(0);
			    break;
			case KeyEvent.VK_SHIFT:
			case KeyEvent.VK_ALT:
			case KeyEvent.VK_CONTROL:
			case KeyEvent.VK_META:
			case KeyEvent.VK_WINDOWS:
			case KeyEvent.VK_COMPOSE:
			case KeyEvent.VK_ALT_GRAPH:
			case KeyEvent.VK_CAPS_LOCK:
			case KeyEvent.VK_SCROLL_LOCK:
			case KeyEvent.VK_NUM_LOCK:
			case KeyEvent.VK_PAUSE:
			case KeyEvent.VK_PRINTSCREEN:
			    break;
			default:
			    nextState = State.TASK;
			    break;
			}
			break;

		    case NAME:
		    case BACKUP_RENAME:
			sName = getJtfName().getText();
			switch (e.getKeyCode()) {
			case KeyEvent.VK_ESCAPE:
			    nextState = State.LOAD;
			    break;
			case KeyEvent.VK_ENTER:
			    break;
			default:
			    checkCorrectLength();
			    break;
			}
			break;

		    case DONE:
			switch (e.getKeyCode()) {
			case KeyEvent.VK_Q:
			    System.exit(0);
			    break;
			case KeyEvent.VK_SHIFT:
			case KeyEvent.VK_ALT:
			case KeyEvent.VK_CONTROL:
			case KeyEvent.VK_META:
			case KeyEvent.VK_WINDOWS:
			case KeyEvent.VK_COMPOSE:
			case KeyEvent.VK_ALT_GRAPH:
			case KeyEvent.VK_CAPS_LOCK:
			case KeyEvent.VK_SCROLL_LOCK:
			case KeyEvent.VK_NUM_LOCK:
			case KeyEvent.VK_PAUSE:
			case KeyEvent.VK_PRINTSCREEN:
			    //case KeyEvent.VK_ENTER:
			    break;
			case KeyEvent.VK_ESCAPE:
			default:
			    nextState = State.LOAD;
			    break;
			}
			break;
		    
		    case FAILED:

			switch (e.getKeyCode()) {
			case KeyEvent.VK_ESCAPE:
			    nextState = State.LOAD;
			    break;
			}
			break;

		    default:
			break;

		    } // end switch (currentState)

		}
		updateState();
	    }
	    public void keyPressed(KeyEvent e) {}
	    public void keyTyped(KeyEvent e) {}

	};
    }
    private MouseListener mousie() {
	return new MouseListener() {
	    public void mouseExited(MouseEvent e) {}
	    public void mouseEntered(MouseEvent e) {}
	    public void mouseReleased(MouseEvent e) {}
	    public void mousePressed(MouseEvent e) {}
	    public void mouseClicked(MouseEvent e) {
		switch (currentState) {
		case LOAD:
		    nextState = State.TASK;
		    break;
		default:
		    break;
		}
		updateState();
	    }
	};
    }


    /*
     * I/O
     */
    private boolean loadFile() {
	ACProfileRenamerFilechooser fc = new ACProfileRenamerFilechooser();
	try {
	    ignoreNextKey = true;
	    hdrFile = fc.getHdrFile();
	    return true;
	} catch (ACProfileRenamerFilechooser.NoFileChosenException e) {
	    errMsg += e.getMessage();
	}
	catch (ACProfileFile.NoHdrExtensionException e) {}
	return false;
    }
    private boolean writeFile() {

	try {
	    hdrFile.writeProfileName(getJtfName().getText());
	    return true;
	} catch (ACProfileRenamerWriter.WritingException e) {
	    e.printStackTrace();
	    errMsg += e.getMessage() + newline;
	} catch (ACProfileRenamerWriter.BackupNotWritableException e) {
	    e.printStackTrace();
	    errMsg += e.getMessage() + newline;
	} catch (ACProfileRenamerGenerator.NameTooLongException e) {
	    e.printStackTrace();
	    errMsg += e.getMessage() + newline;
	}

	return false;
    }
    private boolean loadTargetFile() {
	ACProfileRenamerFilechooser fc = new ACProfileRenamerFilechooser();
	try {
	    ignoreNextKey = true;
	    hdrTargetFile = fc.getHdrTargetFile();
	    return true;
	} 
	catch (ACProfileRenamerFilechooser.NoFileChosenException e) {
	    errMsg += e.getMessage() + newline;
	    e.printStackTrace();
	}
	catch (ACProfileFile.NoHdrExtensionException e) {
	    errMsg += e.getMessage() + newline;
	    e.printStackTrace();
	}
	return false;
    }
    private boolean loadBackupFile() {
	ACProfileRenamerFilechooser fc = new ACProfileRenamerFilechooser();
	try {
	    ignoreNextKey = true;
	    hdrFile = fc.getHdrBackupFile();
	    return true;
	}
	catch (ACProfileRenamerFilechooser.FileNotExistingException e) {
	    errMsg += e.getMessage() + newline;
	    e.printStackTrace();
	}
	catch (ACProfileRenamerFilechooser.NoFileChosenException e) {
	    errMsg += e.getMessage() + newline;
	    e.printStackTrace();
	}
	catch (ACProfileFile.NoHdrExtensionException e) {
	    errMsg += e.getMessage() + newline;
	    e.printStackTrace();
	}
	return false;
    }
    private boolean createBackup() {
	try {
	    hdrFile.copyFileGroup(ACPR_Resources.i().savesDir, hdrTargetFile.getBasename(false));
	    System.err.println("Backup created.");
	    return true;
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	    errMsg += e.getMessage();
	} catch (IOException e) {
	    e.printStackTrace();
	    errMsg += e.getMessage();
	} catch (ACProfileFile.IsNoDirectoryException e) {
	    e.printStackTrace();
	    errMsg += e.getMessage();
	} catch (ACProfileFile.MissingFileException e) {
	    e.printStackTrace();
	    errMsg += e.getMessage();
	}
	return false;
    }



    /*
     * Loader
     */
    private void paintTask() {
	getTaskSlider().setVisible(true);
	getTaskSlider().requestFocus();
    }
    private void paintLoad() {
	setContentPane(loadScreen);
	destroy();
	requestFocus();
    }
    private void paintName() {
	setContentPane(nameScreen);
	destroy();
	getContentPane().add(getJtfName());
	getContentPane().add(getJbSave());
	getJtfName().grabFocus();
    }
    private void paintDone() {
	setContentPane(doneScreen);
	destroy();
	getContentPane().add(getJtfDProfilename());
	getContentPane().add(getJtaDFilename());
	getContentPane().add(getDisp());
	getDisp().showText(sName);
	requestFocus();
    }
    private void paintDoneRestore() {
	setContentPane(doneRestoreScreen);
	destroy();
	requestFocus();
    }
    private void paintFailed() {
	setContentPane(failedScreen);
	destroy();
	getContentPane().add(getJtaErrorMessage());
	requestFocus();
    }


    private void destroy() {
	getContentPane().removeAll();
	helpFrame = null;
	jtfName = null;
	jbSave = null;
	jtaErrorMessage = null;
	jtaDFilename = null;
	jtfDProfilename = null;
	disp = null;
	setLayout(null);
	if (taskSlider != null) getTaskSlider().dispose();
	taskSlider = null;
    }


    /*
     * Updater
     */
    private void updateState() {
	System.err.println(currentState + "->" + nextState);
	switch (currentState) {

	case HELP:
	    nextState = prevState;
	    System.err.println("Correction: " + currentState + "->" + nextState);
	    break;

	case TASK:
	    switch (nextState) {
	    case LOAD:
		getTaskSlider().dispose();
		paintLoad();
		break;
	    case NAME:
		try {
		    sName = hdrFile.getProfileName();
		} catch (Exception e) {}
		paintName();
		break;
	    case BACKUP_RENAME:
		try {
		    sName = hdrFile.getProfileName();
		} catch (Exception e) {
		    e.printStackTrace();
		}
		paintName();
		break;
	    case RESTORE:
		try {
		    hdrFile.copyFileGroup(ACPR_Resources.i().acDir, hdrFile.getBasename(false));
		    System.err.println("Backup restored.");

		    nextState = State.DONE;
		    paintDoneRestore();
		} catch (Exception ex) {
		    errMsg += ex.getMessage();
		    nextState = State.FAILED;
		    paintFailed();
		}
		break;
	    case FAILED:
		paintFailed();
		break;
	    }
	    break;

	case LOAD:
	    switch (nextState) {
	    case NAME:
		try {
		    sName = hdrFile.getProfileName();
		} catch (Exception e) {}
		paintName();
		break;
	    case TASK:
		paintTask();
		break;
	    default:
		break;
	    }
	    break;

	case NAME:
	    switch (nextState) {
	    case LOAD:
		paintLoad();
		break;
	    case DONE:
		if (writeFile()) {
		    paintDone();
		} else {
		    paintFailed();
		    nextState = State.FAILED;
		}
		break;
	    default:
		break;
	    }
	    break;

	case BACKUP_RENAME:
	    switch (nextState) {
	    case LOAD:
		paintLoad();
		break;
	    case DONE:
		if (writeFile() && createBackup()) {
		    paintDone();
		} else {
		    paintFailed();
		    nextState = State.FAILED;
		}
		break;
	    }
	    break;

	case RESTORE:
	    System.err.println("State RESTORE reached");
	    switch(nextState) {
	    case DONE:
		paintDoneRestore();
		break;
	    case FAILED:
		paintFailed();
		break;
	    default:
		break;
	    }
	    break;

	case DONE:
	    switch (nextState) {
	    case LOAD:
		paintLoad();
		break;
	    default:
		break;
	    }
	    break;

	case FAILED:
	    switch (nextState) {
	    case LOAD:
		// Clear error message: New problem now.
		errMsg = "";
		paintLoad();
		break;
	    default:
		break;
	    }
	    break;}
	    
	// Update the current state, if it is not only a temp state
	// (This would break the next state logic)
	if (nextState != State.TEMP) {
	    currentState = nextState;
	}
	if (helpFrame != null && currentState != State.HELP) {
	    helpFrame.dispose();
	    helpFrame = null;
	} else {
	    //System.err.println(">> " + currentState + ", " + helpFrame);
	}
    }

    /**
     * Checks whether the profiel name isn't too long or empty
     * and make it visible in the GUI.
     */
    private void checkCorrectLength() {
	if (ACProfileRenamerGenerator.isInBounds(getJtfName().getText())) {
	    getJtfName().setCorrectColor();
	    getJbSave().setEnabled(true);
	} else {
	    getJtfName().setIncorrectColor();
	    getJbSave().setEnabled(false);
	    System.err.printf("Out of Bounds: %s\n", getJtfName().getText ());
	}
    }



    /*
     * Builder
     */
    private ImageFrame getHelpFrame() {
	if (helpFrame == null) {
	    helpFrame = new ImageFrame(this, new Dimension(100,180), ACPR_Resources.i().iiHelp);
	}
	return helpFrame;
    }
    private JTextFieldC getJtfName() {
	if (jtfName == null) {

	    jtfName = new JTextFieldC(sName);
	    jtfName.setBounds(230, 235, 440, 30);

	    jtfName.addKeyListener(nextStateLogic());
	    jtfName.addMouseListener(new MouseListener() {
		    public void mouseExited(MouseEvent e) {}
		    public void mouseEntered(MouseEvent e) {}
		    public void mouseReleased(MouseEvent e) {
			checkCorrectLength();
		    }
		    public void mousePressed(MouseEvent e) {}
		    public void mouseClicked(MouseEvent e) {}
		});
	    jtfName.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			if (getJbSave().isEnabled()) {
			    nextState = State.DONE;

			    /* Prevent the next keyRelease event to already
			     * go to the next state */
			    ignoreNextKey = true;
			    updateState();
			} else {
			    System.err.println("Won't save, name too long");
			}
		    }
		});


	    checkCorrectLength();

	}
	return jtfName;
    }
    private JTextAreaT getJtfDProfilename() {
	if (jtfDProfilename == null) {
	    jtfDProfilename = new JTextAreaT(Color.WHITE, Color.WHITE, sName);
	    jtfDProfilename.setBounds(260, 201, 400, 80);
	}
	return jtfDProfilename;
    }
    private JTextAreaT getJtaDFilename() {
	if (jtaDFilename == null) {
	    jtaDFilename = new JTextAreaT(Color.WHITE, Color.WHITE, hdrFile.getAbsolutePath());
	    jtaDFilename.setBounds(260, 285, 400, 40);
	}

	return jtaDFilename;
    }
    private JButtonI getJbSave() {
	if (jbSave == null) {
	    jbSave = new JButtonI(ACPR_Resources.urlBtnSave, ACPR_Resources.urlBtnSaveInactive);
	    jbSave.setBounds(683, 237, 36, 28);

	    jbSave.addKeyListener(nextStateLogic());
	    jbSave.addActionListener( new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			ignoreNextKey = true;
			nextState = State.DONE;
			updateState();
		    }
		});
	}
	return jbSave;
    }
    private JTextAreaT getJtaErrorMessage() {
	if (jtaErrorMessage == null) {
	    jtaErrorMessage = new JTextAreaT(Color.WHITE, Color.WHITE);
	    jtaErrorMessage.setBounds(110, 245, 550, 220);
	    jtaErrorMessage.setText(errMsg);
	}
	return jtaErrorMessage;
    }
    private ACByteDisplay getDisp() {
	if (disp == null) {
	    disp = new ACByteDisplay(2, 40);
	    disp.setBounds(120, 370, 540, 100);
	    disp.showText(ACProfileRenamerGenerator.getUntaggedName(sName));
	}
	return disp;
    }
    private TaskSlider getTaskSlider() {
	if (taskSlider == null) {
	    taskSlider = new TaskSlider(this);
	}
	return taskSlider;
    }



    /*
     * Classes
     */

    private class BackgroundImagePanel extends JPanel {
	private Image img;
	public BackgroundImagePanel(Image img, Dimension size) {
	    this.img = img;
	    setSize(size);
	}

	public void paintComponent(Graphics g) {
	    g.drawImage(img, 0, 0, null);
	}
    }
    private class JTextFieldC extends JTextField {

	private final Color incorrectColor = new Color(190,130,46);
	private final Color correctColor = new Color(255,255,255);
	private final Color selectionColor = new Color(216, 228, 238, 80);
	private final Color selectedTextColor = new Color(152, 92, 70);

	public JTextFieldC(String text) {
	    super(text);
	    setSelectionColor(selectionColor);

	    setOpaque(false);
	    setBorder(null);
	    
	    setCorrectColor();

	}

	public void setIncorrectColor() {
	    setForeground(incorrectColor);
	}
	public void setCorrectColor() {
	    setForeground(correctColor);
	}
    }
    private class JButtonI extends JButton {

	private ImageIcon iActive, iInactive;

	public JButtonI(URL iconActive, URL iconInactive) {
	    super();
	    /* Image only: not setOpaque! */
	    //	    setOpaque(false);
	    setContentAreaFilled(false);
	    setBorder(null);
	    setBorderPainted(false);
	    setFocusPainted(false);
	    setMargin(new Insets(0, 0, 0, 0));

	    iActive = new ImageIcon(iconActive);
	    iInactive = new ImageIcon(iconInactive);

	    setIcon(iActive);
	    setDisabledIcon(iInactive);

	    setEnabled(true);
	}


    }
    public static class JTextAreaT extends JTextArea {

	private String defaultText = "";

	private final Color defaultTextColor;
	private final Color textColor;

	/**
	 * Creates a new JTextArea with the possibility to set
	 * a default text with special formattings.
	 * 
	 * @param defaultTextColor Will be used for #setDefaultText
	 * @param textColor Will be used for #setText
	 */
	public JTextAreaT(Color defaultTextColor, Color textColor) {
	    super();
	    this.defaultTextColor = defaultTextColor;
	    this.textColor = textColor;

	    setOpaque(false);
	    setBorder(null);
	    setForeground(defaultTextColor);

	    setFocusable(false);
	    setEditable(false);

	    setLineWrap(true);
	    setWrapStyleWord(true);

	}
	public JTextAreaT(Color defaultTextColor, Color textColor, String defaultText) {
	    this(defaultTextColor, textColor);
	    this.defaultText = defaultText;
	    setText(defaultText);
	}

	/**
	 * Set text with specified default color, italic
	 */
	public void setDefaultText() {
	    super.setText(defaultText);

	    setForeground(defaultTextColor);
	    setFont(getFont().deriveFont(java.awt.Font.ITALIC));
	}

	/**
	 * Set text with specified color, unformatted.
	 */
	public void setText(String s) {
	    super.setText(s);

	    setForeground(textColor);
	    setFont(getFont().deriveFont(java.awt.Font.PLAIN));
	}

    }
    public class ACByteDisplay extends JTextArea {

	private final int delay;
	private final float multiplicator;
	private ByteDisplayThread thread;

	public ACByteDisplay(int delay, float multiplicator) {
	    super();
	    this.delay = delay;
	    this.multiplicator = multiplicator;
	    setForeground(new Color(255,255,255));
	    setOpaque(false);
	    setBorder(null);
	    setFocusable(false);
	    setLineWrap(true);
	    setWrapStyleWord(true);
	}

	public void showText(String profileName) {
	    byte[] bytes = {0};
	    try {
		bytes = ACProfileRenamerGenerator.getByteSequence(profileName);
	    } catch (ACProfileRenamerGenerator.NameTooLongException e) { }

	    for (byte b : bytes) System.err.printf("%02d ", b);
	    System.out.println();

	    if (thread != null) {
		thread.setByteText(bytes);
	    } else {
		thread = new ByteDisplayThread(this, delay, multiplicator, bytes);
	    }
	    try {
		thread.start();
	    } catch (IllegalThreadStateException e) {
		System.err.println("Thread already started!");
	    }
	}

    }
    public class ImageFrame extends JWindow {
	// jWindow instead of JFrame: Not visible in the taskbar

	public ImageFrame(JFrame parent, Dimension offset, ImageIcon background) {
	    super();
	    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	    setResizable(false);
	    setLayout(null);
	    setSize(new Dimension(background.getIconWidth(), background.getIconHeight()));
	    setContentPane(new BackgroundImagePanel(ACPR_Resources.i().iiHelp.getImage(), getSize()));
	    setLocation((int)(parent.getLocation().getX() + offset.getWidth()), (int)(parent.getLocation().getY() + offset.getHeight()));
	    setVisible(true);
	    addKeyListener( new KeyListener() {
		    public void keyPressed(KeyEvent e) {}
		    public void keyReleased(KeyEvent e) {
			dispose();
			System.out.println("test");
		    }
		    public void keyTyped(KeyEvent e) {}
		});
	    addFocusListener( new FocusListener() {
		    public void focusGained(FocusEvent e) {}
		    public void focusLost(FocusEvent e) {
			dispose();
		    }
		});
	}
	public void setVisible(boolean visible) {
	    super.setVisible(visible);
	    if (visible) requestFocus();
	}
    }

    public class TaskSlider extends JWindow {

	private SlideContainer slider;

	public TaskSlider(JFrame parent) {
	    super();
	    Dimension d = new Dimension(600, 300);
	    setResizable(false);
	    setLayout(null);
	    slider = new  SlideContainer(ACPR_Resources.i().iiSlider, 
					 3, (int)d.getWidth(), (int)d.getHeight(), SlideContainer.Animation.SINE_, 30);
	    setSize((int)d.getWidth(), (int)d.getHeight());
	    setContentPane(slider);
	    addKeyListener( new KeyListener() {
		    public void keyTyped(KeyEvent e) {}
		    public void keyPressed(KeyEvent e) {}
		    public void keyReleased(KeyEvent e) {
			System.err.println("Key Pressed!");
			switch (e.getKeyCode()) {
			case KeyEvent.VK_RIGHT:
			    slider.showRight();
			    break;
			case KeyEvent.VK_LEFT:
			    slider.showLeft();
			    break;
			}
		    }
		});
	    setLocation((int)(screenSize.getWidth()-d.getWidth())/2, (int)(screenSize.getHeight()-d.getHeight())/2);
	}
	public void showRight() {
	    slider.showRight();
	}
	public void showLeft() {
	    slider.showLeft();
	}
	public int getSelectedPart() {
	    return slider.getSelectedPart();
	}

    }

    public static void main(String[] args) {
	ACProfileRenamerGUI gui = new ACProfileRenamerGUI();

    }

}