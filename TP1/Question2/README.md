# INF8480 TP1 Question 2

Voici, en quelques lignes, comment construire et exécuter le TP.

## Construire (build)

La question 2 du TP se construit environ de la même façon que les fichiers fournis pour la question 1. Les fichiers `.jar` ainsi que le dossier `bin` fournis dans la remise devraient être suffisants pour exécuter correctement le projet, mais si pour une quelconque raison cela ne fonctionne pas, voici comment le reconstruire :

### Ant

Il est nécessaire d'utiliser **Ant en version 1.10.x** et le **JDK en version 8** pour construire le TP. Il suffira ainsi de se trouver dans le répertoire `Question 2`, où se trouve `build.xml`, puis d'appeler `ant` à partir de la ligne de commandes. Si le tout réussit, on aura un affichage similaire à ceci :

```
$ ant
Buildfile: C:\Users\Felix\Documents\INF8480\INF8480\TP1\Question2\build.xml

init:
    [mkdir] Created dir: C:\Users\Felix\Documents\INF8480\INF8480\TP1\Question2\bin

build-class:
    [javac] Compiling 4 source files to C:\Users\Felix\Documents\INF8480\INF8480\TP1\Question2\bin

build-jar:
      [jar] Building jar: C:\Users\Felix\Documents\INF8480\INF8480\TP1\Question2\shared.jar
      [jar] Building jar: C:\Users\Felix\Documents\INF8480\INF8480\TP1\Question2\server.jar
      [jar] Building jar: C:\Users\Felix\Documents\INF8480\INF8480\TP1\Question2\client.jar

BUILD SUCCESSFUL
Total time: 0 seconds
```
Il faudra ensuite vérifier que les fichiers `server`, `client` et `policy` sont bien présents à la racine du répertoire.

## Exécution

L'exécution du TP est très simple :

### Serveur

1. Modifier l'adresse IP contenue dans le script du serveur (`server`), si nécessaire (par exemple, pour la changer à `127.0.0.1`). Cela est nécessaire pour une exécution correcte des fonctionnalités RMI.
2. Démarrer le registre RMI en effectuant `cd bin` suivi de `rmiregistry &` (sur Linux, sur Windows ce sera plutôt `start rmiregistry`).
3. Retourner à la racine du TP (`cd ..`) puis démarrer le serveur avec la commande `./server`. Un message `File server ready.` devrait alors s'afficher.

### Client

1. Si nécessaire, modifier la constante `DISTANT_HOSTNAME` dans `FileClient.java`, puis reconstruire le projet avec `ant`. Cela sera nécessaire si on veut tester une exécution RMI locale (donc on mettra `127.0.0.1`). Par défaut, la valeur de cette constante est l'adresse IP flottante de notre machine virtuelle sur OpenStack.
2. Lancer une commande du client en effectuant `./client *commande*`, où `*commande*` sera un choix parmi `create`, `get`, `lock`, `syncLocalDirectory`, `list` et `push`. À noter que toutes les commandes à l'exception de `list` et `syncLocalDirectory` nécessitent un argument supplémentaire spécifiant le nom du fichier.
