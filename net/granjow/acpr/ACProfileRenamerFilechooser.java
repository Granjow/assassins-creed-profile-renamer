package net.granjow.acpr;

import java.io.File;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import java.text.DateFormat;
import java.util.Date;

import javax.swing.JFileChooser;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;


public class ACProfileRenamerFilechooser extends JFileChooser {
	

	public ACProfileRenamerFilechooser() {

	super(ACPR_Resources.i().acDir);

	setFileFilter( new FileFilter() {
		public String getDescription() {
		    return "*.hdr (AC Profile Name files)";
		}
		public boolean accept(File f) {
		    return f.getName().endsWith(".hdr") || f.isDirectory();
		}
	    });
	setFileSelectionMode(JFileChooser.FILES_ONLY);
	setMultiSelectionEnabled(false);

	setAccessory(new NamePreview(this));

    }

    public ACProfileFile getHdrFile() throws NoFileChosenException, ACProfileFile.NoHdrExtensionException {
	setDialogTitle(ACPR_Resources.openHdrFileTitle);
	setCurrentDirectory(ACPR_Resources.i().acDir);
	int val = showOpenDialog(null);
	if (val != JFileChooser.APPROVE_OPTION) {
	    throw new NoFileChosenException("Source .hdr file");
	}
	return new ACProfileFile (getSelectedFile().getAbsolutePath());

    }

    public ACProfileFile getHdrBackupFile() throws NoFileChosenException, FileNotExistingException, ACProfileFile.NoHdrExtensionException {
	setDialogTitle(ACPR_Resources.openHdrBackupTitle);
	setCurrentDirectory(ACPR_Resources.i().savesDir);
	int val = showOpenDialog(null);
	if (val != JFileChooser.APPROVE_OPTION) {
	    throw new NoFileChosenException(".hdr backup file");
	}

	String filename = getSelectedFile().getAbsolutePath();
	if (!filename.endsWith(".hdr")) {
	    filename += ".hdr";
	}

	// Test whether file exists
	File f = new File(filename);
	if (!f.exists()) {
	    throw new FileNotExistingException(filename);
	}

	return new ACProfileFile(filename);
    }

    public ACProfileFile getHdrTargetFile() throws NoFileChosenException, ACProfileFile.NoHdrExtensionException {
	String sNFC = "Target .hdr file";

	setDialogTitle(ACPR_Resources.saveHdrClone);
	setCurrentDirectory(ACPR_Resources.i().acDir);
	setSelectedFile(new File("assassination-of-whom.hdr"));
	int val = showSaveDialog(null);
	if (val != JFileChooser.APPROVE_OPTION) {
	    throw new NoFileChosenException(sNFC);
	}

	// Append .hdr extension, if necessary
	String filename = getSelectedFile().getAbsolutePath();
	if (!filename.endsWith(".hdr")) {
	    filename += ".hdr";
	}

	// Test whether file exists
	File f = new File(filename);
	if (f.exists()) {
	    int answer = JOptionPane.showConfirmDialog(null, filename + " will be overwritten. Okay?");
	    if (answer == JOptionPane.CANCEL_OPTION) throw new NoFileChosenException(sNFC);
	    if (answer == JOptionPane.NO_OPTION) return getHdrTargetFile();
	}
	return new ACProfileFile(filename);
    }


    public class NoFileChosenException extends Exception {
	public NoFileChosenException(String expectedFileType) {
	    super("No file chosen. Expected: " + expectedFileType);
	}
    }
    public class FileNotExistingException extends Exception {
	public FileNotExistingException(String filename) {
	    super(filename + " does not exist!");
	}
    }



    /**
     * Directly shows the profile name in the file selection dialogue.
     * @see <a href="http://java.sun.com/docs/books/tutorial/uiswing/components/filechooser.html">Sun Tutorial</a>
     */
    public class NamePreview 
	extends JComponent 
	implements PropertyChangeListener {

	private ACProfileFile currentFile = null;
	private ACProfileRenamerGUI.JTextAreaT jtaProfilename = null;
	private ACProfileRenamerGUI.JTextAreaT jtaLastModified = null;
	private Dimension size = new Dimension(400, 320);

	private final Color textColor = new Color(25, 25, 25);

	public NamePreview(JFileChooser jfc) {

	    super();
	    jfc.addPropertyChangeListener(this);

	    add(getJtaProfilename());
	    add(getLastModified());

	    setPreferredSize(size);
	    setVisible(true);

	}

	public void paintComponent(Graphics g) {
	    g.drawImage(ACPR_Resources.i().iiBackground.getImage(), 0, 0, null);
	}
	public void propertyChange(PropertyChangeEvent e) {

	    boolean update = false;

	    if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(e.getPropertyName())) {
		update = true;
		currentFile = null;
	    } else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(e.getPropertyName())) {
		update = true;
		if (e.getNewValue() != null) {
		    try {
			currentFile = new ACProfileFile(((File) e.getNewValue()).getAbsolutePath());
		    } catch (ACProfileFile.NoHdrExtensionException ex) {}
		    if (currentFile != null && (!currentFile.exists() || !currentFile.isFile())) currentFile = null;
		} else {
		    currentFile = null;
		}
	    }

	    if (update) {
		update();
	    }

	}

	// Update profile name and last modified date for the current file
	private void update() {
	    getJtaProfilename().setText("");


	    // Get the last modified date
	    long lastModified = 0;
	    if (currentFile != null && currentFile.exists() && currentFile.isFile() && currentFile.canRead()) {
		File f = new File(currentFile.getAbsolutePath().substring(0, currentFile.getAbsolutePath().length()-4) + ".sav");
		if (f.exists()) lastModified = f.lastModified();
	    }


	    // Get the profile name, set text
	    if (currentFile != null) {
		try {
		    getJtaProfilename().setText(currentFile.getProfileName());
		} catch (ACProfileRenamerReader.ReadingException e) {
		    e.printStackTrace();
		}
	    } else {
		getJtaProfilename().setDefaultText();
	    }


	    // Set last modified text
	    if (lastModified != 0) {
		Date modifiedDate = new Date(lastModified);
		String sDate = DateFormat.getDateInstance(DateFormat.LONG).format(modifiedDate);
		sDate += System.getProperty("line.separator");
		sDate += DateFormat.getTimeInstance(DateFormat.LONG).format(modifiedDate);
		getLastModified().setText(sDate);
	    } else {
		getLastModified().setDefaultText();
	    }

	}
	private ACProfileRenamerGUI.JTextAreaT getJtaProfilename() {
	    if (jtaProfilename == null) {
		int x = 20, y = 50;
		jtaProfilename = new ACProfileRenamerGUI.JTextAreaT(Color.WHITE, textColor, "The profile name will be displayed here.");
		jtaProfilename.setBounds(x, y, (int)size.getWidth() - 2*x, 100);
		jtaProfilename.setForeground(new Color(255,255,255));
		jtaProfilename.setDefaultText();
	    }
	    return jtaProfilename;
	}
	private ACProfileRenamerGUI.JTextAreaT getLastModified() {
	    if (jtaLastModified == null) {
		int x = 20, y = 200;
		jtaLastModified = new ACProfileRenamerGUI.JTextAreaT(Color.WHITE, textColor, "The date you have last played this savegame will be displayed here.");
		jtaLastModified.setBounds(x, y, (int)size.getWidth() - 2*x, (int)size.getHeight() - y - (int)(1.615*x));
		jtaLastModified.setForeground(new Color(255,255,255));
		jtaLastModified.setDefaultText();
	    }
	    return jtaLastModified;
	}


    }

    public static void main(String[] args) throws NoFileChosenException, ACProfileFile.NoHdrExtensionException {
	ACProfileRenamerFilechooser acfc = new ACProfileRenamerFilechooser();
	acfc.getHdrFile();
    }

}
