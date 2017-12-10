package net.granjow.acpr;

import java.io.File;
import java.net.URL;

import javax.swing.ImageIcon;

public final class ACPR_Resources {

    /** Singleton */
    private ACPR_Resources() {
	acDir = getACDir();
    }
    private static ACPR_Resources singleton = new ACPR_Resources();


    public static ACPR_Resources i() { return singleton; }


    public static final String openHdrFileTitle = "Open Assassin's Creed Savegame";
    public static final String openHdrBackupTitle = "Open Assassin's Creed Backup File";
    public static final String saveHdrClone = "New File: Assassin's Creed Savegame Clone";


    private static final String resDir = "/pictures/";

    public final ImageIcon iiLoad = new ImageIcon(getClass().getResource(resDir + "ACPR-Load.jpg"));
    public final ImageIcon iiName = new ImageIcon(getClass().getResource(resDir + "ACPR-Name.jpg"));
    public final ImageIcon iiDone = new ImageIcon(getClass().getResource(resDir + "ACPR-Done.jpg"));
    public final ImageIcon iiDoneRestore = new ImageIcon(getClass().getResource(resDir + "ACPR-Done-Restore.jpg"));
    public final ImageIcon iiFailed = new ImageIcon(getClass().getResource(resDir + "ACPR-Failed.jpg"));
    public final ImageIcon iiBackground = new ImageIcon(getClass().getResource(resDir + "ACPR-Accessory-Background.jpg"));
    public final ImageIcon iiHelp = new ImageIcon(getClass().getResource(resDir + "ACPR-Help.jpg"));
    public final ImageIcon iiSlider = new ImageIcon(getClass().getResource(resDir + "ACPR-Select-Task.jpg"));

    public static final URL urlBtnSave = ACPR_Resources.class.getResource(resDir + "ACPR-Button-Save-Active.png");
    public static final URL urlBtnSaveInactive = ACPR_Resources.class.getResource(resDir + "ACPR-Button-Save-Inactive.png");

    /**
     * Tries to find the location of the Assassin's Creed Savegames.
     * As this will work on Windows systems only, we can use \\ instead 
     * of the OS-dependant pathSeparator.
     */
    private static File getACDir() {
	File acDir = null;

	String userHome = System.getProperty("user.home") + "\\";
	String ubiDir = "Ubisoft\\Assassin's Creed\\Saved Games\\";
	String[] possibleDirectories = {
	    System.getenv("APPDATA") + "\\",
	    userHome + "AppData\\Roaming\\",
	    userHome + "Application Data\\",
	    userHome + "Anwendungsdaten\\"
	};

	for (String s : possibleDirectories) {
	    File f = new File(s + ubiDir);
	    System.err.printf("\"%s\" exists? \t%s\n", f.getAbsolutePath(), f.exists()? "Yes" : "No");
	    if (f.exists()) {
		acDir = f;
		break;
	    }
	}
	System.err.printf("%%APPDATA%% is >%s<\n", System.getenv("APPDATA"));

	if (acDir == null) {
	    acDir = new File(System.getProperty("user.dir"));
	}

	return acDir;
    }

    public final File savesDir = new File(getACDir().getAbsolutePath() + File.separator + "Saves");
    public final File acDir;

    public static void main(String[] args) {
	System.out.println(i().iiName);
	System.out.println(i().urlBtnSaveInactive);
	ImageIcon ii = new ImageIcon(i().urlBtnSaveInactive);
    }

}