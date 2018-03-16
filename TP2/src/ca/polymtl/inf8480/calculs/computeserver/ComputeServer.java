package ca.polymtl.inf8480.calculs.computeserver;

import ca.polymtl.inf8480.calculs.shared.ComputeServerInterface;
import ca.polymtl.inf8480.calculs.shared.NameServerInterface;
import ca.polymtl.inf8480.calculs.shared.OperationPair;
import ca.polymtl.inf8480.calculs.shared.Utils;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static java.lang.System.exit;


public class ComputeServer implements ComputeServerInterface {

    private int capacity;
    private float maliciousness;
    private NameServerInterface nsStub;

    public ComputeServer(int capacity, float maliciousness) {
        super();
        this.capacity = capacity;
        this.maliciousness = maliciousness;
    }

    public static void main(String args[]) {

        ArrayList<String> arguments = new ArrayList<>(Arrays.asList(args));
        int posQ = arguments.lastIndexOf("-q");
        if (posQ == -1) {
            System.out.println("L'option -q est requise afin de spécifier la capacité du serveur.");
            System.out.println("Usage :\n-q <int>\t: Capacité du serveur.\n-m <float>\t: Niveau de maliciosité du serveur, 0.0-1.0 (facultatif).");
            exit(1);
        }
        String capacityArg = arguments.get(posQ + 1);
        String maliciousnessArg = "";

        int posM = arguments.lastIndexOf("-m");
        if (posM != -1) {
            maliciousnessArg = arguments.get(posM + 1);
            try {
                int capacityNum = Integer.parseInt(capacityArg);
                float maliciousnessNum = Float.parseFloat(maliciousnessArg);
                if (capacityNum < 1) {
                    throw new NumberFormatException("La capacité doit être plus grande que 0.");
                }
                if (maliciousnessNum < 0.0f || maliciousnessNum > 1.0f) {
                    throw new NumberFormatException("Le niveau de maliciosité du serveur doit être au moins 0.0, et pas plus grand que 1.0.");
                }
                ComputeServer cs = new ComputeServer(capacityNum, maliciousnessNum);
                cs.run();
            } catch (NumberFormatException e) {
                System.out.println(e.getMessage());
                System.out.println("Usage :\n-q <int>\t: Capacité du serveur.\n-m <float>\t: Niveau de maliciosité du serveur 0.0-1.0 (facultatif).");
                exit(1);
            }
        } else {
            try {
                int capacityNum = Integer.parseInt(capacityArg);
                if (capacityNum < 1) {
                    throw new NumberFormatException("La capacité doit être plus grande que 0.");
                }
                ComputeServer cs = new ComputeServer(capacityNum, 0.0f);
                cs.run();
            } catch (NumberFormatException e) {
                System.out.println(e.getMessage());
                System.out.println("Usage :\n-q <int>\t: Capacité du serveur.\n-m <float>\t: Niveau de maliciosité du serveur 0.0-1.0 (facultatif).");
                exit(1);
            }
        }


    }

    private void run() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        try {
            ComputeServerInterface stub = (ComputeServerInterface) UnicastRemoteObject.exportObject(this, 5049);
            Registry registry = LocateRegistry.getRegistry(5050);
            registry.rebind("cs", stub);
            System.out.println("Compute server ready.");
        } catch (ConnectException e) {
            System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé?\n");
            System.err.println("Erreur" + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
        }

        this.nsStub = Utils.findNameServer();
    }

    @Override
    public int calculate(List<OperationPair> ops, String username, String pwd) throws RemoteException {
        if (nsStub == null) {
            nsStub = Utils.findNameServer();
        }
        if (nsStub != null) {
            if (nsStub.authenticateClient(username, pwd)) {
                // Capacity refusal
                float refusalRate = (ops.size() - capacity) / (5 * capacity);
                Random rand = new Random(System.nanoTime());
                if (refusalRate > 0.0f && rand.nextFloat() < refusalRate) {
                    return -1;
                }

                // Malicious result
                if (maliciousness > 0.0f && rand.nextFloat() < maliciousness) {
                    return rand.nextInt(4000);
                }

                // Valid result
                int sum = 0;
                for (OperationPair op : ops)
                    switch (op.operation.toLowerCase()) {
                        case "pell":
                            sum = (sum + Operations.pell(op.arg)) % 4000;
                            break;
                        case "prime":
                            sum = (sum + Operations.prime(op.arg)) % 4000;
                            break;
                    }
                return sum;
            } else {
                // Authentication invalid
                return -2;
            }
        } else {
            return -3;
        }


    }

    @Override
    public int getCapacity() throws RemoteException {
        return this.capacity;
    }

}
