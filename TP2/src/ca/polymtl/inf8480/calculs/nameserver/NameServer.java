package ca.polymtl.inf8480.calculs.nameserver;

import ca.polymtl.inf8480.calculs.shared.ComputeServerInterface;
import ca.polymtl.inf8480.calculs.shared.NameServerInterface;

import java.nio.charset.StandardCharsets;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.security.MessageDigest;

public class NameServer implements NameServerInterface {

    HashMap<String, UserPass> users;
    HashMap<String, String> info;
    private final String DOMAIN = "polymtl.ca";
    private final String SUBDOMAIN = "info";
    private final Object USERSLOCK = new Object();

    public static void main(String args[]) {
        NameServer ns = new NameServer();

    }

    private NameServer() {
        this.users = new HashMap<>();
        this.info = new HashMap<>();
    }

    private void run() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        try {
            NameServerInterface stub = (NameServerInterface) UnicastRemoteObject.exportObject(this, 0);
            Registry registry = LocateRegistry.getRegistry();
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
        ArrayList<String> serverList = new ArrayList<>();
        for (String key : info.keySet()) {
            serverList.add(key+"."+SUBDOMAIN+"."+DOMAIN);
        }
        return serverList;
    }

    public boolean authenticateClient(String username, String pwd) throws RemoteException {
        synchronized(USERSLOCK) {
            UserPass savedPwd = this.users.get(username);
            return savedPwd != null && this.hashSHA512(pwd, savedPwd.getSalt()).equals(savedPwd.getHashedPass());
        }
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
        }
        catch (NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        return hashed;
    }

}
