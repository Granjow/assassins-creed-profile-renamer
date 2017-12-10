package net.granjow.acpr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Represents a .hdr file that can be read/written directly.
 */
public class ACProfileFile extends File {
	private static final long serialVersionUID = 1L;
	
	public ACProfileFile(String s) throws NoHdrExtensionException {
	super(s);
	if (!s.endsWith(".hdr")) {
	    throw new NoHdrExtensionException(s);
	}
    }

    public String getProfileName() throws ACProfileRenamerReader.ReadingException {
	String profileName = new String();

	try {
	    byte[] bytes = ACProfileRenamerReader.readFile(this);
	    profileName = ACProfileRenamerGenerator.loadByteSequence(bytes);
	    profileName = ACProfileRenamerGenerator.getTaggedName(profileName);
	}
	finally {
	}

	return profileName;
    }

    /**
     * Changes the profile name of the savegame to profileName
     */
    public void writeProfileName(String profileName) throws ACProfileRenamerWriter.WritingException, 
							     ACProfileRenamerWriter.BackupNotWritableException,
							     ACProfileRenamerGenerator.NameTooLongException {
	String untaggedName = ACProfileRenamerGenerator.getUntaggedName(profileName);
	byte[] bytes = ACProfileRenamerGenerator.getByteSequence(untaggedName);
	ACProfileRenamerWriter.writeFile(getAbsolutePath(), bytes);
    }

    /**
     * Returns the name of the file, without the file extension.
     */
    public String getBasename(boolean absolute) {
	if (absolute) return getAbsolutePath().substring(0, getAbsolutePath().length() - 4);
	else return getName().substring(0, getName().length() - 4);
    }

    /**
     * @return Array of existing files belonging to the savegame (.sav, .opt, .hdr, .map)
     */
    private File[] getFileGroup() throws MissingFileException {
	String basename = getBasename(true);
	File[] files = new File[4];
	files[0] = this;
	files[1] = new File(basename + ".sav");
	files[2] = new File(basename + ".opt"); // optional
	files[3] = new File(basename + ".map"); // optional
	for (int i = 0; i < 2; i++) {
	    if (!files[i].exists()) {
		throw new MissingFileException(files[i].getAbsolutePath());
	    }
	}
	if (!files[3].exists()) { files[3] = null; }
	if (!files[2].exists()) { files[2] = null; }
	return files;
    }
    public boolean isCompleteFilegroup() {
	try {
	    getFileGroup();
	    return true;
	} catch (MissingFileException e) {}
	return false;
    }

    /**
     * Copies all files belonging to this savegame into a new directory. 
     * If {@code newName} is {@code null}, the same name will be taken.
     */
    public void copyFileGroup(File dir, String newName) throws FileNotFoundException, IOException, MissingFileException, IsNoDirectoryException {
	File[] files = getFileGroup();
	for (File f : files) {
	    if (f != null) {
		if (newName == null) {
		    copyTo(f, dir);
		} else {
		    System.err.println("New name will be " + newName + f.getName().substring(f.getName().length()-4));
		    copyTo(f, dir, newName + f.getName().substring(f.getName().length()-4));
		}
	    }
	}
    }
    private void copyTo(File file, File dir) throws FileNotFoundException, IOException, IsNoDirectoryException {
	copyTo(file, dir, file.getName());
    }
    private void copyTo(File file, File dir, String newName) throws FileNotFoundException, IOException, IsNoDirectoryException {
	if (!dir.exists()) dir.mkdir();
	if (dir.isDirectory()) {
	    File newFile = new File(dir.getAbsolutePath() + File.separator + newName);
	    System.err.println("newName: " + newName);

	    FileInputStream fis = null;
	    FileOutputStream fos = null;
	    
	    try {
		System.err.print("Can write " + newFile.getAbsolutePath() + "? \t1 " + newFile.canWrite());
		newFile.getParentFile().mkdirs();
		newFile.createNewFile();
		System.err.println("\t2 " + newFile.canWrite());
		fis = new FileInputStream(file);
		fos = new FileOutputStream(newFile, false);
		byte[] bytes = new byte[4096];
		int len;
		
		while ( (len = fis.read(bytes)) > 0) {
		    fos.write(bytes, 0, len);
		}
	    }
	    finally {
		try {
		    if (fis != null) fis.close();
		    if (fos != null) fos.close();
		} catch (Exception e) {}
	    }
	    return;
	}
	throw new IsNoDirectoryException(dir.getAbsolutePath());
    }
    /**
     * Renames all files belonging to this .hdr file. 
     * <strong>Attention:</strong> This file's name will not change,
     * therefore the new ACProfileFile is returned.
     * @returns The renamed ACProfileFile
     */
    public ACProfileFile renameFileGroup(String newName) throws MissingFileException, RenamingFileException {

	String basepath = (getParent() == null)? "" : getParent() + File.separator;
	File[] files = getFileGroup();
	ACProfileFile f = null;
	try { f = new ACProfileFile(basepath + newName + ".hdr"); }
	catch (NoHdrExtensionException e) {e.printStackTrace();}

	if (!files[0].renameTo(f)) throw new RenamingFileException(files[0].getAbsolutePath(), f.getAbsolutePath());
	files[1].renameTo(new File(basepath + newName + ".sav"));
	if (files[2] != null) files[2].renameTo(new File(basepath + newName + ".opt"));
	if (files[3] != null) files[3].renameTo(new File(basepath + newName + ".map"));

	System.err.println("Renamed files to " + newName);
	return f;
    }

    public static void main(String[] args) 
	throws ACProfileRenamerReader.ReadingException, ACProfileRenamerWriter.WritingException, 
	       ACProfileRenamerWriter.BackupNotWritableException, ACProfileRenamerGenerator.NameTooLongException,
	       NoHdrExtensionException, MissingFileException,
	       FileNotFoundException, IOException, IsNoDirectoryException,
	       RenamingFileException {
	ACProfileFile f = new ACProfileFile("newtest.hdr");
	System.err.println(f.getBasename(true));
	f.writeProfileName(":sword: evil");
	f.getFileGroup();
	f = f.renameFileGroup("secondtest");
	f.copyFileGroup(ACPR_Resources.i().savesDir, null);
	System.err.println(new java.util.Date(f.lastModified()).toString());
    }

    public class NoHdrExtensionException extends Exception {
	public NoHdrExtensionException(String filename) {
	    super(String.format("File %s does not end with .hdr!", filename));
	}
    }
    public class MissingFileException extends Exception {
	public MissingFileException(String filename) {
	    super(String.format("Missing file (%s)! Mandatory for AC savegames!", filename));
	}
    }
    public class IsNoDirectoryException extends Exception {
	public IsNoDirectoryException(String filename) {
	    super(String.format("Cannot copy files to %s, is not a directory!", filename));
	}
    }
    public class RenamingFileException extends Exception {
	public RenamingFileException(String filenameFrom, String filenameTo) {
	    super(String.format("File %s could not be renamed to %s!", filenameFrom, filenameTo));
	}
    }

}

