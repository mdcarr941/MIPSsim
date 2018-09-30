package cda5155.proj1;

import java.util.List;
import java.io.IOException;
import java.nio.file.*;

public class App {
    public static byte string2Byte(String byteString) {
        byte B = 0;
        for (int k = 0; k < 8; ++k) {
            if (byteString.charAt(k) == '1') B |= 1 << (7-k);
        }
        return B;
    }

    public static String byte2String(byte B) {
        char[] chars = new char[] {'0', '0', '0', '0', '0', '0', '0', '0'};
        for (int k = 0; k < 8; ++k) {
            if ((B & (1 << (7-k))) > 0) chars[k] = '1';
        }
        return new String(chars);
    }

    public static byte[] createMemory(List<String> lines) {
        int numLines = lines.size();
        byte[] memory = new byte[256 + 4 * numLines];
        String line;
        for (int k = 0; k < numLines; ++k) {
            line = lines.get(k);
            for (int l = 0; l < 4; ++l) {
                memory[256 + 4*k + l] = string2Byte(line.substring(8 * l, 8 * (l + 1)));
            }
        }
        return memory;
    }

    public static void main(String[] args) throws IOException {
        Path path;
        if (args.length >= 2) {
            path = Paths.get(args[1]);
        }
        else {
            path = Paths.get("sample.txt");
        }
        byte[] memory = createMemory(Files.readAllLines(path));
    }
}
