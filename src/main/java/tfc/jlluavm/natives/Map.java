package tfc.jlluavm.natives;

import org.lwjgl.system.MemoryUtil;

public class Map {
    private static final long[] functionAddrs;

    static {
        System.loadLibrary("bin/hash_map");

        long ptr = getFunctions();
        int funcCount = 4;
        functionAddrs = new long[funcCount];
        for (int i = 0; i < functionAddrs.length; i++) {
            functionAddrs[i] = MemoryUtil.memGetLong(ptr + i * 8);
        }
        MemoryUtil.nmemFree(ptr);
    }

    private static native long getFunctions();

    public static void init() {
    }

    public static long getFunctionAddr(int functionIndex) {
        return functionAddrs[functionIndex];
    }

    public static long getFunctionAddr(String name) {
        return switch (name) {
            case "createMap" -> functionAddrs[0];
            case "freeMap" -> functionAddrs[3];
            case "mapAdd" -> functionAddrs[1];
            case "mapGet" -> functionAddrs[2];
            default -> throw new RuntimeException("Function " + name + " does not exist.");
        };
    }
}
