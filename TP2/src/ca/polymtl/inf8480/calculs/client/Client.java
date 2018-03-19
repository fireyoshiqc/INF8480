package ca.polymtl.inf8480.calculs.client;

import ca.polymtl.inf8480.calculs.nameserver.NameServer;
import ca.polymtl.inf8480.calculs.shared.ComputeServerInterface;
import ca.polymtl.inf8480.calculs.shared.NameServerInterface;
import ca.polymtl.inf8480.calculs.shared.OperationPair;
import ca.polymtl.inf8480.calculs.shared.Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.exit;

public class Client {

    private NameServerInterface nsStub;
    private HashMap<String, ComputeServerInterface> csStubs;
    private HashMap<String, Integer> capacities;
    private String username;
    private String password;
    private List<OperationPair> ops;
    private int result;
    private final Object TOTAL_LOCK = new Object();

    public Client(String inputFile, String username, String password) {

        this.username = username;
        this.password = password;
        List<String> lines = Utils.readFile(inputFile);
        ops = new ArrayList<>();
        if (lines.isEmpty()) {
            System.out.println("Aucune opération spécifiée. Arrêt du répartiteur.");
            exit(1);
        }
        for (String line : lines) {
            String[] lineOps = line.split(" ");
            if (!lineOps[0].toLowerCase().equals("pell") && !lineOps[0].toLowerCase().equals("prime")) {
                System.out.println("Le fichier d'entrée contient une opération invalide (" + lineOps[0] + "). Arrêt du répartiteur.");
                exit(1);
            }
            try {
                ops.add(new OperationPair(lineOps[0], Integer.parseInt(lineOps[1])));
            } catch (NumberFormatException e) {
                System.out.println("Le fichier d'entrée contient un argument numérique invalide (" + lineOps[1] + "). Arrêt du répartiteur.");
                exit(1);
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("Le fichier d'entrée contient une opération sans argument numérique. Arrêt du répartiteur.");
                exit(1);
            }
        }

        // Trouver un serveur de noms, puis obtenir la liste des serveurs de calcul.
        this.queryNameServer();
        this.queryComputeServers();

        // Obtenir la capacité de chacun des serveurs.
        capacities = new HashMap<>();
        List<String> toRemove = new ArrayList<>();
        csStubs.forEach((name, stub) -> {
            try {
                capacities.put(name, stub.getCapacity());
            } catch (RemoteException e) {
                toRemove.add(name);
            }
        });

        // Enlever les serveurs problématiques qui n'ont pas pu retourner leur capacité.
        toRemove.forEach((name) -> csStubs.remove(name));

        // Vérifier s'il reste des serveurs qui fonctionnent.
        if (csStubs.isEmpty()) {
            System.out.println("Aucun serveur de calcul n'est présentement disponible. Arrêt du répartiteur.");
            exit(1);
        }

        // Calculer la capacité totale des serveurs.
        int totalCapacity = 0;
        for (int capacity : capacities.values()) {
            totalCapacity += capacity;
        }

        Object resultLock = new Object();
        int total = 0;
        //HashMap<Thread, ClientTask> tasks = new HashMap<>();

        List<ClientTask> tasks = new ArrayList<>();
        Executor ex = Executors.newCachedThreadPool();
        ExecutorCompletionService<ClientTask.ClientTaskInfo> ecs = new ExecutorCompletionService<>(ex);

        csStubs.forEach((name, stub) -> {
            int qty = (int)(capacities.get(name)*2);
            List<OperationPair> subOps = ops.subList(Math.max(ops.size() - qty, 0), ops.size());
            ClientTask task = new ClientTask(name, stub, new ArrayList<>(subOps), username, password);
            tasks.add(task);
            ops.removeAll(subOps);
        });

        for (ClientTask task : tasks) {
            ecs.submit(task);
        }

        int remainingTasks = tasks.size();

        while (remainingTasks > 0) {
            try {
                ClientTask.ClientTaskInfo res = ecs.take().get();
                switch (res.getStatus()) {
                    case OK:
                        total = (total + res.getResult()) % 4000;
                        if (!ops.isEmpty()) {
                            String name = res.getServerName();
                            int qty = (int)(capacities.get(name)*2);
                            List<OperationPair> subOps = ops.subList(Math.max(ops.size() - qty, 0), ops.size());
                            ClientTask task = new ClientTask(name, csStubs.get(name), new ArrayList<>(subOps), username, password);
                            ecs.submit(task);
                            ops.removeAll(subOps);
                        } else {
                            remainingTasks--;
                        }
                        break;
                    case REFUSED:
                        String name = res.getServerName();
                        System.out.println(String.format("Le serveur '%s' a refusé la tâche. Renvoi.", res.getServerName()));
                        ClientTask task = new ClientTask(name, csStubs.get(name), new ArrayList<>(res.getSublist()), username, password);
                        ecs.submit(task);
                        break;
                    case AUTH_FAILED:
                        csStubs.remove(res.getServerName());
                        ops.addAll(res.getSublist());
                        System.out.println(String.format("Échec de l'authentification sur le serveur '%s'", res.getServerName()));
                        if (csStubs.isEmpty()) {
                            System.out.println("Aucun serveur de calcul n'est présentement disponible. Arrêt du répartiteur.");
                            exit(1);
                        }
                        break;
                    case NO_NAMESERVER:
                        csStubs.remove(res.getServerName());
                        ops.addAll(res.getSublist());
                        System.out.println(String.format("Échec de la communication avec le serveur de noms à partir de '%s'", res.getServerName()));
                        if (csStubs.isEmpty()) {
                            System.out.println("Aucun serveur de noms n'est disponible. Arrêt du répartiteur.");
                            exit(1);
                        }
                        break;
                    case RMI_EXCEPTION:
                        csStubs.remove(res.getServerName());
                        ops.addAll(res.getSublist());
                        System.out.println(String.format("Une erreur RMI est survenue sur le serveur '%s'", res.getServerName()));
                        if (csStubs.isEmpty()) {
                            System.out.println("Aucun serveur de calcul n'est disponible. Arrêt du répartiteur.");
                            exit(1);
                        }
                        break;
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Résultat total : " + total);
        exit(0);
    }

    public static void main(String args[]) {

        ArrayList<String> arguments = new ArrayList<>(Arrays.asList(args));
        int posI = arguments.lastIndexOf("-i");
        int posU = arguments.lastIndexOf("-u");
        int posP = arguments.lastIndexOf("-p");
        if (posI == -1 || posU == -1 || posP == -1 || arguments.size() < 6) {
            System.out.println("Les options -i <fichier>, -u <username> et -p <password> sont obligatoires.");
            System.out.println("Usage :\n-i <String>\t: Fichier de calculs en entrée.\n-u <String>\t: Nom de l'utilisateur." +
                    "\n-p <String>\t: Mot de passe.");
            exit(1);
        }
        String inputFile = arguments.get(posI + 1);
        String username = arguments.get(posU + 1);
        String password = arguments.get(posP + 1);

        // Empêcher l'utilisation de deux options consécutives
        if (inputFile.equals("-u") || inputFile.equals("-p") || username.equals("-i") || username.equals("-p")
                || password.equals("-i") || password.equals("-u")) {
            System.out.println("Les options -i <fichier>, -u <username> et -p <password> sont obligatoires.");
            System.out.println("Usage :\n-i <String>\t: Fichier de calculs en entrée.\n-u <String>\t: Nom de l'utilisateur." +
                    "\n-p <String>\t: Mot de passe.");
            exit(1);
        }

        Client client = new Client(inputFile, username, password);
    }

    private void queryNameServer() {
        if ((nsStub = Utils.findNameServer()) == null) {
            System.out.println("Aucun serveur de noms n'est disponible. Arrêt du répartiteur.");
            exit(1);
        }
    }

    private void queryComputeServers() {
        try {
            csStubs = nsStub.getServers();
            if (csStubs != null && !csStubs.isEmpty()) {
                for (String server : csStubs.keySet()) {
                    System.out.println(server);
                }
            } else {
                System.out.println("Aucun serveur de calcul n'est présentement disponible. Arrêt du répartiteur.");
                exit(1);
            }

        } catch (RemoteException e) {
            System.out.println("Une erreur RMI est survenue : " + e.getMessage());
            exit(1);
        }
    }
}
