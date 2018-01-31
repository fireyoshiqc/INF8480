package ca.polymtl.inf8480.tp1q2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FileServerInterface extends Remote {
    String createClientID() throws RemoteException;
    void create(String nom) throws RemoteException;
    /*FileList or something*/ void list() throws RemoteException;
    /*Files or something*/ void syncLocalDirectory() throws RemoteException;
    /* File */ void get(String nom, String checksum) throws RemoteException;
    /* File */ void lock(String nom, String clientid, String checksum) throws RemoteException;
    void push(String nom, /*File*/ String contenu, String clientid) throws RemoteException;
}