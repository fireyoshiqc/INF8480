package ca.polymtl.inf8480.calculs.nameserver;

import ca.polymtl.inf8480.calculs.shared.ComputeServerInterface;
import ca.polymtl.inf8480.calculs.shared.NameServerInterface;
import ca.polymtl.inf8480.calculs.shared.Utils;

import java.io.*;
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
import java.util.Arrays;
import java.util.HashMap;

import static java.lang.System.exit;

public class NameServer implements NameServerInterface {

    private final Object USERSLOCK = new Object();
    private final Object SERVERLISTLOCK = new Object();
    private HashMap<String, UserPass> users;
    private HashMap<String, ComputeServerInterface> info;

    private NameServer() {
        this.users = new HashMap<>();
        this.info = new HashMap<>();
    }

    public static void main(String args[]) {

        ArrayList<String> arguments = new ArrayList<>(Arrays.asList(args));
        int posAdd = arguments.lastIndexOf("-a");
        if (posAdd == -1) {
            // Mode standard d'opération du nameserver
            NameServer ns = new NameServer();
            ns.run();
            ns.buildServerList();
        } else {
            // Mode d'ajout d'utilisateur
            int posU = arguments.lastIndexOf("-u");
            int posP = arguments.lastIndexOf("-p");
            if (posU == -1 || posP == -1 || arguments.size() < 5) {
                System.out.println("Les options -u <username> et -p <password> doivent être spécifiées lors de l'ajout d'un nouvel utilisateur.");
                System.out.println("Usage :\n-a \t: Mode d'ajout d'utilisateur.\n-u <String>\t: Nom de l'utilisateur (obligatoire avec -a)." +
                        "\n-p <String>\t: Mot de passe de l'utilisateur (obligatoire avec -a).");
                exit(1);
            }
            String username = arguments.get(posU + 1);
            String password = arguments.get(posP + 1);

            // Empêcher l'utilisation de deux options consécutives
            if (username.equals("-p") || password.equals("-u")) {
                System.out.println("Les options -u <username> et -p <password> doivent être spécifiées lors de l'ajout d'un nouvel utilisateur.");
                System.out.println("Usage :\n-a \t: Mode d'ajout d'utilisateur.\n-u <String>\t: Nom de l'utilisateur (obligatoire avec -a)." +
                        "\n-p <String>\t: Mot de passe de l'utilisateur (obligatoire avec -a).");
                exit(1);
            }

            addUser(username, password);

        }
    }

    @SuppressWarnings("unchecked")
    private static void addUser(String username, String password) {
        HashMap<String, UserPass> readTable = null;
        File f = new File("users.dat");
        if (f.exists()) {
            try {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
                readTable = (HashMap<String, UserPass>) ois.readObject();
                ois.close();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Le fichier d'utilisateurs est corrompu. Veuillez supprimer 'users.dat'.");
                exit(1);
            }
        } else {
            try {
                readTable = new HashMap<>();
                if (!f.createNewFile()) {
                    System.out.println("Par magie noire, le fichier 'users.dat' semble déjà exister.");
                    exit(1);
                }
            } catch (IOException e) {
                System.out.println("Impossible de créer le fichier 'users.dat'. Vérifiez les permissions du dossier?");
                exit(1);
            }
        }

        String salt = "" + System.nanoTime();
        UserPass pass = new UserPass(salt, hashSHA512(password, salt));
        readTable.put(username, pass);
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f, false));
            oos.writeObject(readTable);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Impossible d'écrire dans le fichier 'users.dat'. L'auriez-vous supprimé entretemps?");
            exit(1);
        }

        System.out.println("L'utilisateur '" + username + "' a été sauvegardé pour lui donner accès au NameServer.\n" +
                "Il peut dorénavant se servir des serveurs de calcul en utilisant son username et mot de passe (voir usage du client).");
    }

    private static String hashSHA512(String toHash, String salt) {
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

    @SuppressWarnings("unchecked")
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

        File f = new File("users.dat");
        if (f.exists()) {
            try {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
                users = (HashMap<String, UserPass>) ois.readObject();
                ois.close();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Le fichier d'utilisateurs est corrompu. Veuillez supprimer 'users.dat'.");
                exit(1);
            }
        } else {
            System.out.println("Le fichier 'users.dat' n'existe pas.\n" +
                    "Veuillez utiliser l'option -a pour ajouter des utilisateurs avant de démarrer le NameServer.");
            exit(1);
        }
    }

    public HashMap<String, ComputeServerInterface> getServers() throws RemoteException {
        this.buildServerList();
        return info;
    }

    public boolean authenticateClient(String username, String pwd) throws RemoteException {
        synchronized (USERSLOCK) {
            UserPass savedPwd = this.users.get(username);
            return savedPwd != null && hashSHA512(pwd, savedPwd.getSalt()).equals(savedPwd.getHashedPass());
        }
    }

    private void buildServerList() {
        synchronized (SERVERLISTLOCK) {
            info = new HashMap<>();
            for (String host : Utils.readFile(("./config/hosts.conf"))) {
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

}
