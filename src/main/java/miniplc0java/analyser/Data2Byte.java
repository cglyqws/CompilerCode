package miniplc0java.analyser;

import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class Data2Byte {

    public static byte[] getBytes(Boolean data)
    {
        byte[] bytes = new byte[1];
        bytes[0] = data ? (byte) 0x01:0x00;
        return bytes;
    }

    public static byte[] getBytes(short data) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) (data & 0xff);
        bytes[1] = (byte) ((data & 0xff00) >> 8);
        return bytes;
    }

    public static byte[] getBytes(char data) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) (data);
        bytes[1] = (byte) (data >> 8);
        return bytes;
    }

    public static byte[] getBytes(int data) {
        byte[] bytes = new byte[4];
        bytes[3] = (byte) (data & 0xff);
        bytes[2] = (byte) ((data & 0xff00) >> 8);
        bytes[1] = (byte) ((data & 0xff0000) >> 16);
        bytes[0] = (byte) ((data & 0xff000000) >> 24);
        return bytes;
    }
    public static byte[] getBytes2(int data) {
        byte[] bytes = new byte[8];
        bytes[7] = (byte) (data & 0xff);
        bytes[6] = (byte) ((data & 0xff00) >> 8);
        bytes[5] = (byte) ((data & 0xff0000) >> 16);
        bytes[4] = (byte) ((data & 0xff000000) >> 24);
        return bytes;
    }
    public static byte[] getBytes3(int data) {
        byte[] bytes = new byte[1];
        bytes[0] = (byte) (data & 0xff);
        return bytes;
    }
    public static byte[] getBytes(long data) {
        byte[] bytes = new byte[8];
        bytes[0] = (byte) (data & 0xff);
        bytes[1] = (byte) ((data >> 8) & 0xff);
        bytes[2] = (byte) ((data >> 16) & 0xff);
        bytes[3] = (byte) ((data >> 24) & 0xff);
        bytes[4] = (byte) ((data >> 32) & 0xff);
        bytes[5] = (byte) ((data >> 40) & 0xff);
        bytes[6] = (byte) ((data >> 48) & 0xff);
        bytes[7] = (byte) ((data >> 56) & 0xff);
        return bytes;
    }

    public static byte[] getBytes(float data) {
        int intBits = Float.floatToIntBits(data);
        return getBytes(intBits);
    }

    public static byte[] getBytes(double data) {
        long intBits = Double.doubleToLongBits(data);
        return getBytes(intBits);
    }

    public static byte[] getBytes(String data, String charsetName) {
        Charset charset = Charset.forName(charsetName);
        return data.getBytes(charset);
    }

//    public static byte[] getBytes(String data) {
//        return getBytes(data, "GBK");
//    }

    public static byte[] getBytes(String valueString) {
        byte temp[] = new byte[valueString.length()];
        int len = valueString.length();
        for (int i = 0; i < valueString.length(); i++) {
            char c = valueString.charAt(i);
            temp[i] = (byte) (c & 0xff);
        }
        return temp;
    }


}