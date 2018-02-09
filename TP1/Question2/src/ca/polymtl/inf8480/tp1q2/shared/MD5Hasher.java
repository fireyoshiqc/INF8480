package ca.polymtl.inf8480.tp1q2.shared;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class MD5Hasher {

    private MD5Hasher() {

    }

    public static String hashMD5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5 = md.digest(s.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte aMd5 : md5) {
                sb.append(Integer.toHexString((aMd5 & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static String hashMD5(byte[] barr) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5 = md.digest(barr);
            StringBuilder sb = new StringBuilder();
            for (byte aMd5 : md5) {
                sb.append(Integer.toHexString((aMd5 & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
