package ca.polymtl.inf8480.calculs.client;

import ca.polymtl.inf8480.calculs.shared.ComputeServerInterface;
import ca.polymtl.inf8480.calculs.shared.NameServerInterface;
import ca.polymtl.inf8480.calculs.shared.OperationPair;
import ca.polymtl.inf8480.calculs.shared.Utils;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import static java.lang.System.exit;

public class Client {

    private final int[] fChunk = {0};
    private NameServerInterface nsStub;
    private HashMap<String, ComputeServerInterface> csStubs;
    private HashMap<String, Integer> capacities;
    private List<OperationPair> ops;

    public Client(String inputFile, String username, String password, boolean secure) {

        // Lire le fichier contenant la liste d'opérations à effectuer.
        List<String> lines = Utils.readFile(inputFile);
        ops = new ArrayList<>();
        if (lines.isEmpty()) {
            System.out.println("Aucune opération spécifiée. Arrêt du répartiteur.");
            exit(1);
        }
        for (String line : lines) {
            String[] lineOps = line.split(" ");
            if (!lineOps[0].toLowerCase().equals("pell") && !lineOps[0].toLowerCase().equals("prime")) {
                System.out.printf("Le fichier d'entrée contient une opération invalide (%s). Arrêt du répartiteur.%n", lineOps[0]);
                exit(1);
            }
            try {
                ops.add(new OperationPair(lineOps[0], Integer.parseInt(lineOps[1])));
            } catch (NumberFormatException e) {
                System.out.printf("Le fichier d'entrée contient un argument numérique invalide (%s). Arrêt du répartiteur.%n", lineOps[1]);
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

        System.out.printf("Démarrage du répartiteur en mode %s.%n", secure ? "sécurisé" : "non-sécurisé");

        // Lancer le calcul et compter son temps d'exécution.
        long start = System.currentTimeMillis();
        int total = secure ? secureCalculation(username, password) : insecureCalculation(username, password);
        long end = System.currentTimeMillis();

        System.out.printf("Résultat total : %d%n", total);
        System.out.println(String.format("Temps d'exécution : %d millisecondes.", end - start));
        exit(0);
    }

    public static void main(String args[]) {

        // Prendre les arguments de ligne de commande.
        ArrayList<String> arguments = new ArrayList<>(Arrays.asList(args));
        int posI = arguments.lastIndexOf("-i");
        int posU = arguments.lastIndexOf("-u");
        int posP = arguments.lastIndexOf("-p");
        int posM = arguments.lastIndexOf("-m");
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
        if (inputFile.equals("-u") || inputFile.equals("-p") || inputFile.equals("-m")
                || username.equals("-i") || username.equals("-p") || username.equals("-m")
                || password.equals("-i") || password.equals("-u") || password.equals("-m")) {
            System.out.println("Les options -i <fichier>, -u <username> et -p <password> sont obligatoires.");
            System.out.println("Usage :\n-i <String>\t: Fichier de calculs en entrée.\n-u <String>\t: Nom de l'utilisateur." +
                    "\n-p <String>\t: Mot de passe.\n-m\t: Mode malicieux (non-sécurisé).");
            exit(1);
        }

        // Créer un client, dont la majorité de l'exécution se fait dans le constructeur.
        Client client = new Client(inputFile, username, password, posM == -1);
    }

    // Trouver le serveur de noms à partir du fichier de config (voir Utils).
    private void queryNameServer() {
        if ((nsStub = Utils.findNameServer()) == null) {
            System.out.println("Aucun serveur de noms n'est disponible. Arrêt du répartiteur.");
            exit(1);
        }
    }

    // Établir la liste des serveurs de calcul à l'aide du serveur de noms.
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
            System.out.printf("Une erreur RMI est survenue : %s%n", e.getMessage());
            exit(1);
        }
    }

    // Calcul en mode sécurisé (aucun serveur malicieux).
    private int secureCalculation(String username, String password) {

        // Résultat total
        int total = 0;

        // Établir une liste de tâches et un exécuteur pour les lancer.
        List<ClientTask> tasks = new ArrayList<>();
        Executor ex = Executors.newCachedThreadPool();
        ExecutorCompletionService<ClientTask.ClientTaskInfo> ecs = new ExecutorCompletionService<>(ex);

        // Pour chaque serveur disponible, lancer une tâche de calcul correspondant à 2 fois sa capacité.
        // Cela équivaut à ~15-20% de taux de refus en moyenne.
        csStubs.forEach((name, stub) -> {
            int qty = capacities.get(name) * 2;

            // Prendre les 'qty' derniers éléments de la liste d'opérations.
            List<OperationPair> subOps = ops.subList(Math.max(ops.size() - qty, 0), ops.size());

            // Lancer la tâche avec cette sous-liste.
            ClientTask task = new ClientTask(name, stub, new ArrayList<>(subOps), username, password, -1);
            tasks.add(task);

            // Enlever la sous-liste des opérations restantes.
            ops.removeAll(subOps);
        });

        // Soumettre les tâches à l'exécuteur.
        for (ClientTask task : tasks) {
            ecs.submit(task);
        }

        int remainingTasks = tasks.size();

        // Tant qu'il reste des tâches actives, tenter de récupérer les résultats de leur exécution.
        while (remainingTasks > 0) {
            try {
                // Appel bloquant pour récupérer une tâche terminée de la file de l'exécuteur.
                ClientTask.ClientTaskInfo res = ecs.take().get();

                // En fonction du résultat fourni par la tâche...
                switch (res.getStatus()) {

                    // Si OK, lancer une nouvelle tâche à partir de la fin de la liste d'opérations (comme plus haut).
                    case OK:

                        // Ajouter le résultat de l'exécution au total modulo 4000.
                        total = (total + res.getResult()) % 4000;

                        // S'il reste des calculs à faire, lancer une nouvelle tâche sur le serveur dorénavant disponible.
                        if (!ops.isEmpty()) {
                            String name = res.getServerName();
                            int qty = capacities.get(name) * 2;
                            List<OperationPair> subOps = ops.subList(Math.max(ops.size() - qty, 0), ops.size());
                            ClientTask task = new ClientTask(name, csStubs.get(name), new ArrayList<>(subOps), username, password, -1);
                            ecs.submit(task);
                            ops.removeAll(subOps);

                            // S'il ne reste plus de calculs à faire, décrémenter le nombre de tâches actives.
                        } else {
                            remainingTasks--;
                        }
                        break;

                    // Si le calcul est refusé, relancer le calcul sur le même serveur (il devrait finir par se compléter puisqu'on n'atteint pas 6 fois la capacité du serveur).
                    case REFUSED:
                        String name = res.getServerName();
                        System.out.println(String.format("Le serveur '%s' a refusé la tâche. Renvoi.", res.getServerName()));
                        ClientTask task = new ClientTask(name, csStubs.get(name), new ArrayList<>(res.getSublist()), username, password, -1);
                        ecs.submit(task);
                        break;

                    // Si l'authentification a échoué, retirer le serveur de la liste des serveurs disponibles.
                    // En temps normal, un échec d'authentification pour mauvais username/password devrait se réfléter
                    // sur tous les serveurs et provoquer l'arrêt du répartiteur.
                    case AUTH_FAILED:

                        // Retirer le serveur fautif et remettre ses opérations en fin de liste.
                        csStubs.remove(res.getServerName());
                        ops.addAll(res.getSublist());
                        System.out.println(String.format("Échec de l'authentification sur le serveur '%s'", res.getServerName()));

                        // S'il ne reste plus de serveurs disponibles (ce qui est normalement le cas lors d'un échec
                        // d'authentification), arrêter le répartiteur.
                        if (csStubs.isEmpty()) {
                            System.out.println("Aucun serveur de calcul n'est présentement disponible. Arrêt du répartiteur.");
                            exit(1);
                        }
                        break;

                    // Si le serveur de calcul n'arrive pas à trouver le serveur de noms, faire la même chose que pour AUTH_FAILED.
                    // On assume ici que le serveur de calcul est fautif.
                    case NO_NAMESERVER:
                        csStubs.remove(res.getServerName());
                        ops.addAll(res.getSublist());
                        System.out.println(String.format("Échec de la communication avec le serveur de noms à partir de '%s'", res.getServerName()));
                        if (csStubs.isEmpty()) {
                            System.out.println("Aucun serveur de noms n'est disponible. Arrêt du répartiteur.");
                            exit(1);
                        }
                        break;

                    // Idem s'il y a une erreur RMI, donc que le serveur est mort ou refuse la connexion.
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
                System.out.println("Erreur : Un thread a été interrompu ou n'a pas pu s'exécuter correctement.");
            }
        }

        // On retourne le résultat total.
        return total;
    }

    // Calcul en mode non-sécurisé (possibilité de serveurs malicieux).
    private int insecureCalculation(String username, String password) {

        // Le fonctionnement est sensiblement le même que pour secureCalculation, initialement.
        int total = 0;
        List<ClientTask> tasks = new ArrayList<>();
        Executor ex = Executors.newCachedThreadPool();
        ExecutorCompletionService<ClientTask.ClientTaskInfo> ecs = new ExecutorCompletionService<>(ex);

        csStubs.forEach((name, stub) -> {
            int qty = capacities.get(name) * 2;
            List<OperationPair> subOps = ops.subList(Math.max(ops.size() - qty, 0), ops.size());

            // Petite différence ici, chaque calcul est associé à un identifiant de 'chunk', qui indique quel morceau
            // de la liste d'opérations est traité. C'est utile pour la validation par la suite.
            int chunk = fChunk[0];
            ClientTask task = new ClientTask(name, stub, new ArrayList<>(subOps), username, password, chunk);
            fChunk[0]++;
            tasks.add(task);
            ops.removeAll(subOps);
        });

        for (ClientTask task : tasks) {
            ecs.submit(task);
        }

        int remainingTasks = tasks.size();

        // Hashmap des résultats à valider (voir plus bas).
        HashMap<Integer, List<Integer>> resultsToValidate = new HashMap<>();

        // Pareil que pour secureCalculation.
        while (remainingTasks > 0) {
            try {
                ClientTask.ClientTaskInfo res = ecs.take().get();
                switch (res.getStatus()) {
                    // Si le résultat est OK (supposément), le traiter par rapport à resultsToValidate (détaillé plus bas).
                    case OK:
                        String serverName = res.getServerName();
                        int result = res.getResult();
                        int resChunk = res.getChunk();

                        // Ajouter le résultat à la HashMap si celui-ci est absent. Sinon, procéder à une vérification des résultats précédents.
                        List<Integer> oldList = resultsToValidate.putIfAbsent(resChunk, Arrays.asList(result));
                        if (oldList != null) {
                            ArrayList<Integer> resList = new ArrayList<>(oldList);

                            // Si le résultat existe déjà dans la liste associée au chunk traité par le serveur, alors
                            // on a un bon résultat car validé par deux serveurs.
                            if (resList.indexOf(result) != -1) {
                                // On enlève le chunk du Hashmap car il est traité, et on ajoute au total comme pour
                                // secureCalculation.
                                resultsToValidate.remove(resChunk);
                                total = (total + result) % 4000;

                                // Même chose que secureCalculation, on démarre une nouvelle tâche avec un nouvel
                                // identifiant de chunk, et on enlève les calculs alloués de la liste d'opérations
                                // restantes.
                                if (!ops.isEmpty()) {
                                    int qty = capacities.get(serverName) * 2;
                                    List<OperationPair> subOps = ops.subList(Math.max(ops.size() - qty, 0), ops.size());
                                    int chunk = fChunk[0];
                                    ClientTask task = new ClientTask(serverName, csStubs.get(serverName), new ArrayList<>(subOps), username, password, chunk);
                                    fChunk[0]++;
                                    ecs.submit(task);
                                    ops.removeAll(subOps);
                                } else {
                                    remainingTasks--;
                                }

                            // Si le résultat n'existe pas déjà dans la liste de résultats associés au chunk, il
                            // faut le faire valider par un autre serveur au moins.
                            } else {
                                resList.add(result);
                                resultsToValidate.replace(resChunk, resList);

                                // Trouver un serveur pouvant accueillir le chunk de calculs. On peut accorder le calcul
                                // à un serveur ayant jusqu'à la moitié de la capacité du serveur original, au besoin.
                                // Dans nos expériences, cela n'arrive pas.
                                Optional<Map.Entry<String, ComputeServerInterface>> server = csStubs.entrySet().stream()
                                        .filter((entry) -> capacities.get(entry.getKey()) * 2 >= capacities.get(serverName) && !entry.getKey().equals(serverName)).findAny();

                                // Si on trouve un serveur pour la validation, procéder en lui envoyant la même tâche
                                // qu'au serveur original.
                                if (server.isPresent()) {
                                    Map.Entry<String, ComputeServerInterface> serverEntry = server.get();
                                    ClientTask task = new ClientTask(serverEntry.getKey(), serverEntry.getValue(), new ArrayList<>(res.getSublist()), username, password, res.getChunk());
                                    System.out.println(String.format("Tâche #%d transférée du serveur '%s' au serveur '%s' pour validation.", res.getChunk(), serverName, serverEntry.getKey()));
                                    ecs.submit(task);

                                // Sinon, arrêter le répartiteur car on a une configuration invalide pour l'utilisation
                                // en mode non-sécurisé.
                                } else {
                                    System.out.println("Aucun serveur de calcul disponible pour la validation du calcul. Arrêt du répartiteur.");
                                    exit(1);
                                }

                            }
                        } else {
                            // On fait la même chose si jamais le chunk n'a jamais été traité auparavant.
                            Optional<Map.Entry<String, ComputeServerInterface>> server = csStubs.entrySet().stream()
                                    .filter((entry) -> capacities.get(entry.getKey()) * 2 >= capacities.get(serverName) && !entry.getKey().equals(serverName)).findAny();
                            if (server.isPresent()) {
                                Map.Entry<String, ComputeServerInterface> serverEntry = server.get();
                                ClientTask task = new ClientTask(serverEntry.getKey(), serverEntry.getValue(), new ArrayList<>(res.getSublist()), username, password, res.getChunk());
                                ecs.submit(task);
                                System.out.println(String.format("Tâche #%d transférée du serveur '%s' au serveur '%s' pour validation.", res.getChunk(), serverName, serverEntry.getKey()));
                            } else {
                                System.out.println("Aucun serveur de calcul disponible pour la validation du calcul. Arrêt du répartiteur.");
                                exit(1);
                            }
                        }


                        break;

                    // Les autres cas se font exactement comme pour le mode sécurisé, à la différence qu'on enlève le chunk
                    // qui avait été alloué au serveur puisqu'on remet ses opérations dans la liste d'ops restantes.
                    // Si un serveur était en train de valider un résultat d'un autre serveur, il faudra recommencer
                    // pour ces calculs.
                    case REFUSED:
                        String name = res.getServerName();
                        System.out.println(String.format("Le serveur '%s' a refusé la tâche. Renvoi.", res.getServerName()));
                        ClientTask task = new ClientTask(name, csStubs.get(name), new ArrayList<>(res.getSublist()), username, password, res.getChunk());
                        ecs.submit(task);
                        break;
                    case AUTH_FAILED:
                        csStubs.remove(res.getServerName());
                        resultsToValidate.remove(res.getChunk());
                        ops.addAll(res.getSublist());
                        System.out.println(String.format("Échec de l'authentification sur le serveur '%s'", res.getServerName()));
                        if (csStubs.isEmpty()) {
                            System.out.println("Aucun serveur de calcul n'est présentement disponible. Arrêt du répartiteur.");
                            exit(1);
                        }
                        break;
                    case NO_NAMESERVER:
                        csStubs.remove(res.getServerName());
                        resultsToValidate.remove(res.getChunk());
                        ops.addAll(res.getSublist());
                        System.out.println(String.format("Échec de la communication avec le serveur de noms à partir de '%s'", res.getServerName()));
                        if (csStubs.isEmpty()) {
                            System.out.println("Aucun serveur de noms n'est disponible. Arrêt du répartiteur.");
                            exit(1);
                        }
                        break;
                    case RMI_EXCEPTION:
                        csStubs.remove(res.getServerName());
                        resultsToValidate.remove(res.getChunk());
                        ops.addAll(res.getSublist());
                        System.out.println(String.format("Une erreur RMI est survenue sur le serveur '%s'", res.getServerName()));
                        if (csStubs.isEmpty()) {
                            System.out.println("Aucun serveur de calcul n'est disponible. Arrêt du répartiteur.");
                            exit(1);
                        }
                        break;
                }
            } catch (InterruptedException | ExecutionException e) {
                System.out.println("Erreur : Un thread a été interrompu ou n'a pas pu s'exécuter correctement.");
            }
        }
        // On retourne le résultat total.
        return total;
    }
}
