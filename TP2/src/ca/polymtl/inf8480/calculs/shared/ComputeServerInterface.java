package ca.polymtl.inf8480.calculs.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ComputeServerInterface extends Remote {
    int calculate(List<OperationPair> ops, String username, String pwd) throws RemoteException;

    int getCapacity() throws RemoteException;
}
