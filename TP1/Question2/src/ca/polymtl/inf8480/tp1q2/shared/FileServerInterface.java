package ca.polymtl.inf8480.tp1q2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public interface FileServerInterface extends Remote {
    String createClientID() throws RemoteException, NoSuchAlgorithmException;
    String create(String nom) throws RemoteException;
    ArrayList<String> list() throws RemoteException;
    ArrayList<byte[]> syncLocalDirectory() throws RemoteException;
    byte[] get(String nom, String checksum) throws RemoteException;
    byte[] lock(String nom, String clientid, String checksum) throws RemoteException;
    void push(String nom, /*File*/ String contenu, String clientid) throws RemoteException;
}