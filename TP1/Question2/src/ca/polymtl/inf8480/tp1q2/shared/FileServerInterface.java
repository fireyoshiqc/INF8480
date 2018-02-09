package ca.polymtl.inf8480.tp1q2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

public interface FileServerInterface extends Remote {
    String createClientID() throws RemoteException, NoSuchAlgorithmException;
    String create(String nom) throws RemoteException;
    ArrayList<String> list() throws RemoteException;
    HashMap<String, byte[]> syncLocalDirectory() throws RemoteException;
    byte[] get(String nom, String checksum) throws RemoteException;
    byte[] lock(String nom, String clientid, String checksum) throws RemoteException;
    String push(String nom, byte[] contenu, String clientid) throws RemoteException;
}