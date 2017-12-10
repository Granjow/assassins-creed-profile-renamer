package net.granjow.acpr;

/**
 * A savegame in Assassin's Creed for PC consists of four files:
 * <ul><li>The .sav file which contains the savegame itself</li>
 * <li>The .map file containing the keyboard layout</li>
 * <li>The .opt file containing some unknown information</li>
 * <li>The .hdr file where the profile name is stored in.</li></ul>
 * This class generates bytecode for the .hdr file. 

 * In comparisation to the «official» process (creating a new game in AC
 * and entering the name there) this method supports 25 instead of only 20 
 * characters and the whole printable ASCII alphabet. Additionally some
 * special AC «ingame» symbols like the hidden blade symbol are supported. 
 */
public class ACProfileRenamerGenerator {

    public static final int MAXLENGTH = 25;

    private static class ACSymbol {
	final String symbolName;
	final String symbolTag;
	final byte bytecode;
	public ACSymbol(String symbolName, String symbolTag, byte bytecode) {
	    this.symbolName = symbolName;
	    this.symbolTag = symbolTag;
	    this.bytecode = bytecode;
	}
    }


    enum Symbols {
	PLACEHOLDER;
	static ACSymbol Feet = new ACSymbol("Feet", ":foot:", (byte)0xa2);
	static ACSymbol Hand = new ACSymbol("Hand", ":hand:", (byte)0xa3);
	static ACSymbol Hand2 = new ACSymbol("Hand 2", ":hand2:", (byte)0xa4);
	static ACSymbol ArrowDown = new ACSymbol("Arrow down", ":down:", (byte)0xa5);
	static ACSymbol ArrowLeft = new ACSymbol("Arrow left", ":left:", (byte)0xa6);
	static ACSymbol ArrowRight = new ACSymbol("Arrow right", ":right:", (byte)0xa7);
	static ACSymbol HiddenBlade = new ACSymbol("Hidden Blade", ":blade:", (byte)0xaf);
	static ACSymbol Fist = new ACSymbol("Fist", ":fist:",(byte) 0xb1);
	static ACSymbol EagleVision = new ACSymbol("Eagle Vision", ":eagle:", (byte)0xbc);
	static ACSymbol ShortSword = new ACSymbol("Short Sword", ":shortsword:", (byte)0xbd);
	static ACSymbol Sword = new ACSymbol("Sword", ":sword:", (byte)0xbe);
	static ACSymbol Lock = new ACSymbol("Lock", ":lock:", (byte)0xb2);
	static ACSymbol Hood = new ACSymbol("Hood", ":hood:", (byte)0xb3);
	static ACSymbol Camera = new ACSymbol("Camera", ":cam:", (byte)0xb5);
    }

    /**
     * The bytecode for different symbols in Assassin's Creed.
     */
    public static final ACSymbol[] symbols = {
	new ACSymbol("Feet", ":foot:", (byte)0xa2),
	new ACSymbol("Hand", ":hand:", (byte)0xa3),
	new ACSymbol("Hand 2", ":hand2:", (byte)0xa4),
	new ACSymbol("Arrow down", ":down:", (byte)0xa5),
	new ACSymbol("Arrow left", ":left:", (byte)0xa6),
	new ACSymbol("Arrow right", ":right:", (byte)0xa7),
	new ACSymbol("Hidden Blade", ":blade:", (byte)0xaf),
	new ACSymbol("Fist", ":fist:",(byte) 0xb1),
	new ACSymbol("Eagle Vision", ":eagle:", (byte)0xbc),
	new ACSymbol("Short Sword", ":shortsword:", (byte)0xbd),
	new ACSymbol("Sword", ":sword:", (byte)0xbe),
	new ACSymbol("Lock", ":lock:", (byte)0xb2),
	new ACSymbol("Hood", ":hood:", (byte)0xb3),
	new ACSymbol("Camera", ":cam:", (byte)0xb5)
    };


    /**
     * Replaces all symbol tags by their hex value.
     */
    public static final String getUntaggedName(String profileName) {
	String untaggedName = profileName;

	for (ACSymbol sym : symbols) {
	    if (untaggedName.contains(sym.symbolTag)) System.out.printf("Replacing %s by 0x%02x\n", sym.symbolTag, sym.bytecode);
	    untaggedName = untaggedName.replace(sym.symbolTag, ""+(char)sym.bytecode);
	}

	return untaggedName;
    }

    /**
     * Checks whether the profile name does not exceed the maximum length.
     */
    public static final boolean isInBounds(String profileName) {
	System.err.println("Maxlength is " + MAXLENGTH + ", untagged Name is " + getUntaggedName(profileName)
			   + ", l = " + getUntaggedName(profileName).length());
	return getUntaggedName(profileName).length() <= MAXLENGTH && getUntaggedName(profileName).length() > 0;
    }

    /**
     * Builds the byte sequence from the given name.
     * The byte sequence can be written to a .hdr file
     * and read by Assassin's Creed.
     */
    public static final byte[] getByteSequence(String profileName) throws NameTooLongException {

	String untaggedName = getUntaggedName(profileName);

	byte[] bytes = new byte[untaggedName.length()*2 + 4];
	int offset = 4;

	// Check that the maximum length is not exceeded
	if (!isInBounds(profileName)) throw new NameTooLongException(untaggedName.length());


	// The first byte describes the length of the profile name,
	// the next three are zero bytes.
	bytes[0] = (byte)untaggedName.length();
	bytes[1] = (byte)0x00;
	bytes[2] = (byte)0x00;
	bytes[3] = (byte)0x00;


	// Writes the name. Each character byte is followed by a zero byte.
	for (int i = 0; i < untaggedName.length(); i++) {
	    bytes[2*i+offset] = (byte)untaggedName.charAt(i);
	    bytes[2*i+offset+1] = (byte)0x00;
	}

	return bytes;
    }

    /**
     * Replaces special bytecodes with the corresponding tag.
     */
    public static final String getTaggedName(String untaggedName) {
	String taggedName = untaggedName;

	for (ACSymbol sym : symbols) {
	    if (taggedName.contains("" + (char)sym.bytecode)) {
		System.err.printf("Replacing %02x with tag %s\n", sym.bytecode, sym.symbolTag);
	    }
	    taggedName = taggedName.replace("" + (char)sym.bytecode, sym.symbolTag);
	}

	return taggedName;
    }

    /**
     * Reads a .hdr file byte sequence and returns the (unformatted) string
     * still containing hex values.
     */
    public static final String loadByteSequence(byte[] bytes) {

	String plainName = "";


	// Read the name length from the first byte
	byte length = bytes[0];
	int fileLength = length*2 + 4;
	System.err.printf("Loaded file with length %d\n", length);

	/*
	 * Read the name (offset: 4).
	 * As every second byte is a zero byte, select a step size of 2.
	 */
	for (int i = 4; i < fileLength; i += 2) {
	    plainName += (char) bytes[i];
	}

	return plainName;
    }

    public static void main(String[] args) throws NameTooLongException {
	if (args.length == 0) return;
	ACProfileRenamerGenerator png = new ACProfileRenamerGenerator();

	byte[] bytes = png.getByteSequence(args[0]);
	for (int i = 0; i < bytes.length; i++) {
	    System.out.print((char)bytes[i]);
	}
	System.out.println();
	for (int i = 0; i < bytes.length; i++) {
	    System.out.printf("%02x ", bytes[i]);
	}
	System.out.println();
    }

    public static class NameTooLongException extends Exception {
	NameTooLongException(int size) {
	    super("Name is too long (" + size + "), maximum length is " + MAXLENGTH + ".");
	}
    }

}