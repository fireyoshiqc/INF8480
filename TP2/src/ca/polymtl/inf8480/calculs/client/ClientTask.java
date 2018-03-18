package ca.polymtl.inf8480.calculs.client;

import ca.polymtl.inf8480.calculs.shared.ComputeServerInterface;
import ca.polymtl.inf8480.calculs.shared.OperationPair;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ClientTask implements Runnable {

    enum TaskResult {
        OK, REFUSED, AUTH_FAILED, NO_NAMESERVER, RMI_EXCEPTION
    }

    private String name;
    private ComputeServerInterface stub;
    private List<OperationPair> ops;
    private int qty;
    private final String username;
    private final String password;
    private int[] total;
    private final int index;
    private final Object resultLock;
    private ClientTaskInfo status;

    public ClientTask(String name, ComputeServerInterface stub, List<OperationPair> ops, int qty, String username, String password, int[] total, int index, Object resultLock) {
        this.name = name;
        this.stub = stub;
        this.ops = ops;
        this.qty = qty;
        this.username = username;
        this.password = password;
        this.total = total;
        this.index = index;
        this.resultLock = resultLock;
    }

    public List<OperationPair> getOps() {
        return new ArrayList<>(ops.subList(Math.max(index-qty, 0), index));
    }

    public String getName() {
        return name;
    }

    @Override
    public void run() {
        List<OperationPair> subOps = ops.subList(Math.max(index-qty, 0), index);
        try {
            int res = stub.calculate(new ArrayList<>(subOps), username, password);
            if (res >= 0) {
                synchronized(resultLock){
                    total[0] = (total[0] + res) % 4000;
                }
                status = new ClientTaskInfo(TaskResult.OK, subOps);
            }
            else if (res == -1) {
                System.out.println("La tâche a été refusée sur le serveur de calcul '" + name + "'.");
                status = new ClientTaskInfo(TaskResult.REFUSED, subOps);
            }
            else if (res == -2) {
                System.out.println("L'authentification est incorrecte sur le serveur de calcul '" + name + "'.");
                status = new ClientTaskInfo(TaskResult.AUTH_FAILED, subOps);
            }
            else if (res == -3) {
                System.out.println("Le serveur de noms n'a pas été trouvé à partir du serveur de calcul '" + name + "'.");
                status = new ClientTaskInfo(TaskResult.NO_NAMESERVER, subOps);
            }

        } catch (RemoteException e) {
            System.out.println("Une erreur RMI est survenue : " + e.getMessage());
            status = new ClientTaskInfo(TaskResult.RMI_EXCEPTION, subOps);
        }

    }

    public ClientTaskInfo getStatus() {
        return status;
    }

    class ClientTaskInfo {
        private TaskResult result;
        private List<OperationPair> sublist;
        ClientTaskInfo(TaskResult result, List<OperationPair> sublist) {
            this.result = result;
            this.sublist = sublist;
        }

        public TaskResult getResult() {
            return result;
        }

        public List<OperationPair> getSublist() {
            return sublist;
        }
    }
}
