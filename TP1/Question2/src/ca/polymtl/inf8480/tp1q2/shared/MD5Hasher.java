package ca.polymtl.inf8480.tp1q2.shared;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Classe utilitaire permettant de générer des sommes de hachages MD5 à l'aide de String ou de tableaux de bytes.
 */
public final class MD5Hasher {

    /**
     * Constructeur privé afin d'empêcher l'instanciation de la classe.
     */
    private MD5Hasher() {

    }

    /**
     * Fonction produisant le hachage MD5 d'une chaîne de caractères.
     * @param s La chaîne à hacher.
     * @return La somme de hachage MD5, sous forme de String.
     */
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

    /**
     * Fonction produisant le hachage MD5 d'un tableau de bytes.
     * @param barr Le tableau de bytes à hacher.
     * @return La somme de hachage MD5, sous forme de String.
     */
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
