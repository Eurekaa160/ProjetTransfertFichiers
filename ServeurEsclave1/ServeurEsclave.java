import java.io.*;
import java.net.*;
import java.util.Properties;

public class ServeurEsclave {
    public static void main(String[] args) {
        try {
            // Charger la configuration
            Properties config = new Properties();
            try (FileInputStream fis = new FileInputStream("../config.properties")) {
                config.load(fis);
            }

            // Vérifier les arguments d'entrée
            if (args.length != 1) {
                System.err.println("Usage : java ServeurEsclave <id>");
                System.exit(1);
            }

            String serverKey = "slave" + args[0]; // Exemple : slave1

            // Lire les paramètres depuis la configuration
            int port = Integer.parseInt(config.getProperty(serverKey + ".port", "-1"));
            String directoryPath = getStorageDirectory(config, serverKey);

            if (port == -1 || directoryPath == null || directoryPath.isEmpty()) {
                throw new IllegalArgumentException("Configuration invalide pour : " + serverKey);
            }

            // Assurer que le répertoire de stockage existe
            File storageDir = new File(directoryPath);
            if (!storageDir.exists() && !storageDir.mkdirs()) {
                throw new IOException("Impossible de créer le répertoire : " + storageDir.getAbsolutePath());
            }

            System.out.println("Serveur esclave " + args[0] + " démarré sur le port " + port);
            System.out.println("Répertoire de stockage : " + storageDir.getAbsolutePath());

            // Créer un socket serveur avec l'option SO_REUSEADDR pour permettre la réutilisation rapide du port
            try (ServerSocket serverSocket = new ServerSocket()) {
                serverSocket.setReuseAddress(true);  // Activer la réutilisation de l'adresse
                serverSocket.bind(new InetSocketAddress(port)); // Lier le serveur au port

                while (true) {
                    System.out.println("En attente de connexion du serveur principal...");
                    try (Socket socket = serverSocket.accept();
                         DataInputStream in = new DataInputStream(socket.getInputStream())) {

                        // Récupérer le nom du fichier et sa taille
                        String originalFileName = in.readUTF();
                        long fileSize = in.readLong();

                        System.out.println("Nom du fichier reçu : " + originalFileName);
                        System.out.println("Taille attendue : " + fileSize + " octets");

                        // Construire le fichier avec un suffixe unique
                        File outputFile = createUniqueFile(storageDir, originalFileName);

                        System.out.println("Nom avec suffixe : " + outputFile.getAbsolutePath());

                        // Recevoir les données et les écrire dans le fichier
                        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            long totalRead = 0;

                            System.out.println("Début de la réception du fichier...");
                            while (totalRead < fileSize && (bytesRead = in.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                                totalRead += bytesRead;
                                System.out.printf("Progression : %.2f%%\n", (totalRead / (double) fileSize) * 100);
                            }

                            if (totalRead == fileSize) {
                                System.out.println("Fichier reçu avec succès : " + outputFile.getAbsolutePath());
                            } else {
                                System.err.println("Erreur : La taille du fichier reçu ne correspond pas à la taille attendue.");
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Erreur lors de la réception : " + e.getMessage());
                    }
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Erreur du serveur esclave : " + e.getMessage());
        }
    }

    /**
     * Récupère le répertoire de stockage défini dans la configuration.
     * 
     * @param config    Propriétés chargées
     * @param serverKey Clé spécifique au serveur
     * @return Chemin du répertoire de stockage
     */
    private static String getStorageDirectory(Properties config, String serverKey) {
        return config.getProperty(serverKey + ".directory", "/default/directory");
    }

    /**
     * Crée un fichier avec un suffixe unique pour éviter les conflits.
     * 
     * @param directory Répertoire de stockage
     * @param fileName  Nom original du fichier
     * @return Fichier avec un suffixe unique
     */
    private static File createUniqueFile(File directory, String fileName) {
        int partNumber = 1;
        String name = fileName;
        File file = new File(directory, name);

        // Vérifie si le fichier existe et génère un nom unique
        while (file.exists()) {
            int dotIndex = fileName.lastIndexOf(".");
            if (dotIndex == -1) {
                name = fileName + "_partie" + partNumber;
            } else {
                String baseName = fileName.substring(0, dotIndex);
                String extension = fileName.substring(dotIndex);
                name = baseName + "_partie" + partNumber + extension;
            }
            file = new File(directory, name);
            partNumber++;
        }

        return file;
    }
}
