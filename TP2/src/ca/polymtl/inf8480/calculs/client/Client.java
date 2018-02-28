package ca.polymtl.inf8480.calculs.client;

import ca.polymtl.inf8480.calculs.shared.NameServerInterface;
import ca.polymtl.inf8480.calculs.shared.Utils;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    private NameServerInterface nsStub;

    public static void main(String args[]) {
        Client client = new Client();

    }

    public Client() {
        nsStub = Utils.findNameServer();
        try {
            for (String server : nsStub.getServers()) {
                System.out.println(server);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private NameServerInterface loadNameServerStub(String hostname) {
        NameServerInterface stub = null;

        try {
            Registry registry = LocateRegistry.getRegistry(hostname, 5050);
            stub = (NameServerInterface) registry.lookup("ns");
        } catch (NotBoundException e) {
            System.err.println("Erreur: Le nom '" + e.getMessage()
                    + "' n'est pas défini dans le registre.");
        } catch (RemoteException e) {
            System.err.println("Erreur: " + e.getMessage());
        }

        return stub;
    }

}
