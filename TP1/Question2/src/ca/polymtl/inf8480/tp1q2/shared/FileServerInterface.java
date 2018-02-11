package ca.polymtl.inf8480.tp1q2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Interface devant être implémentée par le serveur. Servira de "stub" pour le client.
 * Voir le détail des fonctions et méthodes dans la classe FileServer.java
 */
public interface FileServerInterface extends Remote {
    String createClientID() throws RemoteException, NoSuchAlgorithmException;
    String create(String nom) throws RemoteException;
    ArrayList<String> list() throws RemoteException;
    HashMap<String, byte[]> syncLocalDirectory() throws RemoteException;
    byte[] get(String nom, String checksum) throws RemoteException;
    byte[] lock(String nom, String clientid, String checksum) throws RemoteException;
    String push(String nom, byte[] contenu, String clientid) throws RemoteException;
}