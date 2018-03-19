package ca.polymtl.inf8480.calculs.client;

import ca.polymtl.inf8480.calculs.shared.ComputeServerInterface;
import ca.polymtl.inf8480.calculs.shared.OperationPair;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ClientTask implements Callable<ClientTask.ClientTaskInfo> {

    private final String username;
    private final String password;
    private String name;
    private ComputeServerInterface stub;
    private List<OperationPair> ops;
    private int qty;
    private ClientTaskInfo status;

    public ClientTask(String name, ComputeServerInterface stub, List<OperationPair> ops, String username, String password) {
        this.name = name;
        this.stub = stub;
        this.ops = ops;
        this.username = username;
        this.password = password;
    }

    @Override
    public ClientTaskInfo call() throws Exception {
        try {
            int res = stub.calculate(ops, username, password);
            if (res >= 0) {
                status = new ClientTaskInfo(res, TaskResult.OK, ops, name);
            } else if (res == -1) {
                status = new ClientTaskInfo(res, TaskResult.REFUSED, ops, name);
            } else if (res == -2) {
                status = new ClientTaskInfo(res, TaskResult.AUTH_FAILED, ops, name);
            } else if (res == -3) {
                status = new ClientTaskInfo(res, TaskResult.NO_NAMESERVER, ops, name);
            }

        } catch (RemoteException e) {
            status = new ClientTaskInfo(-4, TaskResult.RMI_EXCEPTION, ops, name);
        }
        return status;
    }

    enum TaskResult {
        OK, REFUSED, AUTH_FAILED, NO_NAMESERVER, RMI_EXCEPTION
    }

    class ClientTaskInfo {

        private int result;
        private TaskResult status;
        private List<OperationPair> sublist;
        private String serverName;

        ClientTaskInfo(int result, TaskResult status, List<OperationPair> sublist, String serverName) {
            this.result = result;
            this.status = status;
            this.sublist = sublist;
            this.serverName = serverName;
        }

        public int getResult() {
            return result;
        }

        public TaskResult getStatus() {
            return status;
        }

        public List<OperationPair> getSublist() {
            return sublist;
        }

        public String getServerName() {
            return serverName;
        }
    }
}
