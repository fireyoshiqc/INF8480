package ca.polymtl.inf8480.calculs.nameserver;

import ca.polymtl.inf8480.calculs.shared.ComputeServerInterface;
import ca.polymtl.inf8480.calculs.shared.NameServerInterface;
import ca.polymtl.inf8480.calculs.shared.Utils;

import java.nio.charset.StandardCharsets;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

public class NameServer implements NameServerInterface {

    private final Object USERSLOCK = new Object();
    HashMap<String, UserPass> users;
    HashMap<String, ComputeServerInterface> info;

    private NameServer() {
        this.users = new HashMap<>();
        this.info = new HashMap<>();
    }

    public static void main(String args[]) {
        NameServer ns = new NameServer();
        ns.run();
        ns.buildServerList();
    }

    private void run() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        try {
            NameServerInterface stub = (NameServerInterface) UnicastRemoteObject.exportObject(this, 5048);
            Registry registry = LocateRegistry.getRegistry(5050);
            registry.rebind("ns", stub);
            System.out.println("Name server ready.");
        } catch (ConnectException e) {
            System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé?\n");
            System.err.println("Erreur" + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }

    public ArrayList<String> getServers() throws RemoteException {
        return new ArrayList<>(info.keySet());
    }

    public boolean authenticateClient(String username, String pwd) throws RemoteException {
        synchronized (USERSLOCK) {
            UserPass savedPwd = this.users.get(username);
            return savedPwd != null && this.hashSHA512(pwd, savedPwd.getSalt()).equals(savedPwd.getHashedPass());
        }
    }

    private void buildServerList() {
        info = new HashMap<>();
        for (String host : Utils.readConfigFile(("./config/hosts.conf"))) {
            if (System.getSecurityManager() == null) {
                System.setSecurityManager(new SecurityManager());
            }
            ComputeServerInterface distantServerStub = loadServerStub(host);
            if (distantServerStub != null) {
                System.out.println("Found ComputeServer on host : " + host);
                info.put(host, distantServerStub);
            }
        }
    }

    private ComputeServerInterface loadServerStub(String hostname) {
        ComputeServerInterface stub = null;

        try {
            Registry registry = LocateRegistry.getRegistry(hostname, 5050);
            stub = (ComputeServerInterface) registry.lookup("cs");
        } catch (NotBoundException e) {
            System.err.println("Erreur: Le nom '" + e.getMessage()
                    + "' n'est pas défini dans le registre.");
        } catch (RemoteException e) {
            System.err.println("Erreur: " + e.getMessage());
        }

        return stub;
    }

    private String hashSHA512(String toHash, String salt) {
        String hashed = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] bytes = md.digest(toHash.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            hashed = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hashed;
    }

}
