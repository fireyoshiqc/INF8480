package ca.polymtl.inf8480.calculs.shared;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

public class Utils {

    public static NameServerInterface findNameServer() {
        for (String host : readConfigFile(("./config/hosts.conf"))) {
            if (System.getSecurityManager() == null) {
                System.setSecurityManager(new SecurityManager());
            }
            NameServerInterface stub = loadNameServerStub(host);
            if (stub != null) {
                System.out.println("Found NameServer on host : " + host);
                return stub;
            }
        }
        return null;
    }

    public static ArrayList<String> readConfigFile(String filename) {
        ArrayList<String> lines = new ArrayList<>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String str;
            while ((str = in.readLine()) != null) {
                lines.add(str);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private static NameServerInterface loadNameServerStub(String hostname) {
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
