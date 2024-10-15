package tfc.jlluavm.exec;

import org.lwjgl.system.MemoryUtil;
import tfc.jlluavm.exec.binding.LUABound;

@LUABound(name = "std", scope="none")
public class StandardFunctions {
    // types:
    // 0: int/long
    // 1: float/double
    // 2: boolean
    // 3: string
    // 4: table
    // 5: function
    // 6: jni function

    @LUABound(name = "print", scope = "global")
    public static void print(long dOut, byte type, long data) {
        switch (type) {
            case 0 -> System.out.println(data);
            case 1 -> System.out.println(Double.longBitsToDouble(data));
            default -> throw new RuntimeException("NYI: type " + type);
        }
        MemoryUtil.memPutByte(dOut, (byte) 3);
        MemoryUtil.memPutLong(dOut + 1, 0);
    }

    @LUABound(name = "math", scope = "global")
    public static class LUAMath {
        @LUABound(name = "pi")
        public static double PI = Math.PI;

        @LUABound(name = "cos")
        public static void cos(long dOut, byte type, long value) {
            double out = switch (type) {
                case 0 -> Math.cos(value);
                case 1 -> Math.cos(Double.longBitsToDouble(value));
                default -> throw new RuntimeException("Cos not applicable to type: " + type);
            };

            MemoryUtil.memPutByte(dOut, (byte) 1);
            MemoryUtil.memPutDouble(dOut + 1, out);
        }

        @LUABound(name = "sin")
        public static void sin(long dOut, byte type, long value) {
            double out = switch (type) {
                case 0 -> Math.sin(value);
                case 1 -> Math.sin(Double.longBitsToDouble(value));
                default -> throw new RuntimeException("Sin not applicable to type: " + type);
            };

            MemoryUtil.memPutByte(dOut, (byte) 1);
            MemoryUtil.memPutDouble(dOut + 1, out);
        }

        @LUABound(name = "exp")
        public static void exp(long dOut, byte type, long value) {
            double out = switch (type) {
                case 0 -> Math.exp(value);
                case 1 -> Math.exp(Double.longBitsToDouble(value));
                default -> throw new RuntimeException("Exp not applicable to type: " + type);
            };

            MemoryUtil.memPutByte(dOut, (byte) 1);
            MemoryUtil.memPutDouble(dOut + 1, out);
        }

        @LUABound(name = "sqrt")
        public static void sqrt(long dOut, byte type, long value) {
            double out = switch (type) {
                case 0 -> Math.sqrt(value);
                case 1 -> Math.sqrt(Double.longBitsToDouble(value));
                default -> throw new RuntimeException("Sqrt not applicable to type: " + type);
            };

            MemoryUtil.memPutByte(dOut, (byte) 1);
            MemoryUtil.memPutDouble(dOut + 1, out);
        }

        @LUABound(name = "floor")
        public static void floor(long dOut, byte type, long value) {
            double out = switch (type) {
                case 0 -> Math.floor(value); // TODO: should output a long
                case 1 -> Math.floor(Double.longBitsToDouble(value));
                default -> throw new RuntimeException("Floor not applicable to type: " + type);
            };

            MemoryUtil.memPutByte(dOut, (byte) 1);
            MemoryUtil.memPutDouble(dOut + 1, out);
        }

        @LUABound(name = "ceil")
        public static void ceil(long dOut, byte type, long value) {
            double out = switch (type) {
                case 0 -> Math.ceil(value); // TODO: should output a long
                case 1 -> Math.ceil(Double.longBitsToDouble(value));
                default -> throw new RuntimeException("Ceil not applicable to type: " + type);
            };

            MemoryUtil.memPutByte(dOut, (byte) 1);
            MemoryUtil.memPutDouble(dOut + 1, out);
        }

        @LUABound(name = "abs")
        public static void abs(long dOut, byte type, long value) {
            double out = switch (type) {
                case 0 -> Math.abs(value); // TODO: should output a long
                case 1 -> Math.abs(Double.longBitsToDouble(value));
                default -> throw new RuntimeException("Abs not applicable to type: " + type);
            };

            MemoryUtil.memPutByte(dOut, (byte) 1);
            MemoryUtil.memPutDouble(dOut + 1, out);
        }

        @LUABound(name = "pow")
        public static void pow(long dOut, byte type, long value, byte type1, long value1) {
            double lh = switch (type) {
                case 0 -> value;
                case 1 -> Double.longBitsToDouble(value);
                default -> throw new RuntimeException("Pow not applicable to type: " + type);
            };
            double rh = switch (type1) {
                case 0 -> value1;
                case 1 -> Double.longBitsToDouble(value1);
                default -> throw new RuntimeException("Pow not applicable to type: " + type1);
            };

            MemoryUtil.memPutByte(dOut, (byte) 1);
            MemoryUtil.memPutDouble(dOut + 1, Math.pow(lh, rh));
        }
    }
}
