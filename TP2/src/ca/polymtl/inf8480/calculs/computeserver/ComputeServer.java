package ca.polymtl.inf8480.calculs.computeserver;

import ca.polymtl.inf8480.calculs.shared.ComputeServerInterface;
import ca.polymtl.inf8480.calculs.shared.OperationPair;
import org.apache.commons.cli.*;

import java.rmi.ConnectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Random;

import static java.lang.System.exit;


public class ComputeServer implements ComputeServerInterface {

    private int capacity;
    private float maliciousness;

    public static void main(String args[]) {

        Options options = new Options();

        Option capacity = new Option("q", "capacity", true, "compute server capacity");
        capacity.setRequired(true);
        options.addOption(capacity);

        Option maliciousness = new Option("m", "maliciousness", true, "server maliciousness");
        maliciousness.setRequired(false);
        options.addOption(maliciousness);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            exit(1);
        }

        String capacityArg = cmd.getOptionValue("capacity");
        String maliciousnessArg = cmd.getOptionValue("maliciousness", "0");

        try {
            int capacityNum = Integer.parseInt(capacityArg);
            float maliciousnessNum = Float.parseFloat(maliciousnessArg);
            if (capacityNum < 1) {
                throw new NumberFormatException("La capacité doit être plus grande que 0");
            }
            if (maliciousnessNum < 0.0f || maliciousnessNum > 1.0f) {
                throw new NumberFormatException("Le niveau de maliciosité du serveur doit être au moins 0.0, et pas plus grand que 1.0.");
            }
            ComputeServer cs = new ComputeServer(capacityNum, maliciousnessNum);
            cs.run();
        } catch (NumberFormatException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            exit(1);
        }
    }

    public ComputeServer(int capacity, float maliciousness) {
        super();
        this.capacity = capacity;
        this.maliciousness = maliciousness;
    }

    private void run() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        try {
            ComputeServerInterface stub = (ComputeServerInterface) UnicastRemoteObject.exportObject(this, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("cs", stub);
            System.out.println("Compute server ready.");
        } catch (ConnectException e) {
            System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé?\n");
            System.err.println("Erreur" + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }

    public int calculate(ArrayList<OperationPair> ops) throws RemoteException {
        // Capacity refusal
        float refusalRate = (ops.size() - capacity)/(5*capacity);
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
                sum += Operations.pell(op.arg) % 4000;
                break;
            case "prime":
                sum += Operations.prime(op.arg) % 4000;
                break;
        }
        return sum;
    }

    public int getCapacity() throws RemoteException {
        return this.capacity;
    }

}
