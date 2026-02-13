import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HWID {

    // Ye array Hex conversion ke liye use hota hai (0-9 + A-F)
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    // Ye method final HWID ko String form me return karta hai (A1B2C3D4...)
    public static String getHWID() {
        return bytesToHex(generateHWID());
    }

    // Ye method actual HWID generate karta hai by using PC hardware / OS info
    public static byte[] generateHWID() {
        try {
            // MD5 hashing algorithm use hota hai
            MessageDigest hash = MessageDigest.getInstance("MD5");

            // Yaha system ki information collect ho rahi hai
            String s =
                    System.getProperty("os.name") +             // Operating System Name
                            System.getProperty("os.arch") +             // CPU Architecture (x64/x86)
                            System.getProperty("os.version") +          // OS Version
                            Runtime.getRuntime().availableProcessors() + // CPU Cores Count
                            System.getenv("PROCESSOR_IDENTIFIER") +      // Processor Model Name
                            System.getenv("PROCESSOR_ARCHITECTURE") +    // Processor Architecture
                            System.getenv("PROCESSOR_ARCHITEW6432") +    // 64-bit architecture info
                            System.getenv("NUMBER_OF_PROCESSORS");       // Total Processors again

            // Sab combined string ko MD5 hash me convert kar diya
            return hash.digest(s.getBytes());

        } catch (NoSuchAlgorithmException e) {
            throw new Error("Algorithm wasn't found.", e);
        }
    }

    // Hex String se Byte Array banane ke liye (rarely used)
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] =
                    (byte) ((Character.digit(s.charAt(i), 16) << 4)
                            + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    // Byte Array ko Hex String me convert karta hai (final readable format)
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];     // Pehla hex character (high nibble)
            hexChars[j * 2 + 1] = hexArray[v & 0x0F]; // Doosra hex character (low nibble)
        }

        return new String(hexChars);
    }
}
