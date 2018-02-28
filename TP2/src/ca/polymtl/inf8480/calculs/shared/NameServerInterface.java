package ca.polymtl.inf8480.calculs.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface NameServerInterface extends Remote {
    ArrayList<String> getServers() throws RemoteException;
    boolean authenticateClient(String username, String pwd) throws RemoteException;
}
