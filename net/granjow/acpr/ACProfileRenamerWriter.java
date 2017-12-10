package net.granjow.acpr;

import java.io.*;

/**
 * Provides a method to write the .hdr file for Assassin's Creed PC savegames.
 * A backup will be created automatically.
 */
public class ACProfileRenamerWriter {


    /**
     * Writes the bytecode to the selected file after backing it up.
     */
    public static void writeFile(String hdrFile, byte[] bytes) throws WritingException, BackupNotWritableException {

	System.err.println("Will write to " + hdrFile);

	File bak = new File(hdrFile);
	if (bak.exists() && bak.isFile()) {

	    // Remove existing backup
	    File f = new File(hdrFile + ".orig");
	    if (f.exists()) {
		if (!f.canWrite()) {
		    throw new BackupNotWritableException(f.toString(), hdrFile);
		}
		f.delete();
	    }

	    // Rename original file to backup file
	    bak.renameTo(f);
	    bak.setWritable(true);
	    bak = null;
	    f = null;
	}

	// Write the new file
	File out = new File(hdrFile);
	FileOutputStream fos = null;
	try {
	    fos = new FileOutputStream(out, false);
	    fos.write(bytes);
	} catch (IOException e) {
	    e.printStackTrace();
	    throw new WritingException(hdrFile, e.getMessage());
	}
	finally {
	    try {
		fos.close();
	    } catch (IOException e) {}
	}

    }

    public static void main(String[] args) throws ACProfileRenamerGenerator.NameTooLongException, WritingException, BackupNotWritableException {
	writeFile("test", ACProfileRenamerGenerator.getByteSequence(args[0]));
    }

    public static final class BackupNotWritableException extends Exception {
	public BackupNotWritableException(String filename, String filenameNew) {
	    super("Backup file (" + filename + ") is write protected, aborting!" + System.getProperty("line.separator") 
		  + "Please make it writable. It will be replaced by the current original file (" + filenameNew + ").");
	}
    }

    public static final class WritingException extends Exception {
	public WritingException(String filename, String message) {
	    super("Could not write " + filename + "!" + System.getProperty("line.separator") + message);
	}
    }

}