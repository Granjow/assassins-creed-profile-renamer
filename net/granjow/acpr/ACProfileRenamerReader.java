package net.granjow.acpr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


/**
 * Reads the bytes of a .hdr file.
 */
public class ACProfileRenamerReader {
    
    public static byte[] readFile(File f) throws ReadingException {
	byte[] bytes = {-1};
	FileInputStream fis = null;

	try {
	    fis = new FileInputStream(f);
	    byte b;

	    // Read the length from the first byte
	    b = (byte) fis.read();
	    int length = 2*b + 4;

	    bytes = new byte[length];
	    bytes[0] = b;

	    int bytesRead = fis.read(bytes, 1, length-1);
	    if (bytesRead < 4) {
		bytes[0] = 0;
		System.err.println("File is empty!");
	    } else if (bytesRead+1 < bytes[0]) {
		bytes[0] = (byte)((bytesRead-4)/2);
		System.err.println("Corrected file length: " + bytes[0]);
	    }


	} catch (IOException e) {
	    e.printStackTrace();
	    throw new ReadingException(f);
	}
	finally {
	    try {
		if (fis != null) fis.close();
	    } catch (IOException e) {}
	}

	return bytes;
    }

    public static void main(String args[]) throws ReadingException {
	byte[] bytes;
	bytes = readFile(new File("MB2_Tamir.hdr"));
	for (byte b : bytes) {
	    System.out.printf("%02x ", b);
	}
	System.out.println();
	String s = ACProfileRenamerGenerator.loadByteSequence(bytes);
	System.out.println(s);
	s = ACProfileRenamerGenerator.getTaggedName(s);
	System.out.println(s);
	readFile(new File("(I don't exist)"));
    }

    public static class ReadingException extends Exception {
	public ReadingException(File f) {
	    super("There was a problem reading file " + f.getAbsoluteFile() + ".");
	}
    }

}