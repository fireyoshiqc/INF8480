package ca.polymtl.inf8480.calculs.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface ComputeServerInterface extends Remote {
    int calculate(ArrayList<OperationPair> ops) throws RemoteException;
    int getCapacity() throws RemoteException;
}
