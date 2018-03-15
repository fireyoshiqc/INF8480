package ca.polymtl.inf8480.calculs.client;

import ca.polymtl.inf8480.calculs.computeserver.ComputeServer;
import ca.polymtl.inf8480.calculs.shared.ComputeServerInterface;
import ca.polymtl.inf8480.calculs.shared.OperationPair;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.exit;

public class ClientTask implements Runnable {

    private ComputeServerInterface stub;
    private List<OperationPair> ops;
    private int qty;
    private final String username;
    private final String password;
    private int[] total;
    private final int index;
    private final Object resultLock;

    public ClientTask(ComputeServerInterface stub, List<OperationPair> ops, int qty, String username, String password, int[] total, int index, Object resultLock) {
        this.stub = stub;
        this.ops = ops;
        this.qty = qty;
        this.username = username;
        this.password = password;
        this.total = total;
        this.index = index;
        this.resultLock = resultLock;
    }

    @Override
    public void run() {
        try {
            int res = stub.calculate(new ArrayList<>(ops.subList(Math.max(index-qty, 0), index)), username, password);
            synchronized(resultLock){
                total[0] += res;
            }
        } catch (RemoteException e) {
            System.out.println("Une erreur RMI est survenue : " + e.getMessage());
            exit(1);
        }

    }
}
