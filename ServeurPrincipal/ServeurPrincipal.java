import java.io.*;
import java.net.*;
import java.util.Properties;

public class ServeurPrincipal {
    public static void main(String[] args) 
    {
        Properties config = new Properties();

        // Charger la configuration
        try (FileInputStream fis = new FileInputStream("../config.properties")) {
            config.load(fis);
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du fichier de configuration : " + e.getMessage());
            return;
        }

        int port = Integer.parseInt(config.getProperty("serveurPrincipal.port", "8085"));  // Port d'écoute du serveur principal

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("ServeurPrincipal démarré sur le port " + port);

            while (true) {
                System.out.println("En attente de connexion d'un client...");
                Socket socket = serverSocket.accept(); // Attente de la connexion du client
                System.out.println("Connexion acceptée de " + socket.getInetAddress());

                // Gestion des flux
                try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
                    String fileName = in.readUTF();  // Lire le nom du fichier
                    long fileSize = in.readLong();   // Lire la taille du fichier

                    System.out.println("Fichier reçu : " + fileName + " (taille : " + fileSize + " octets)");

                    // Réception du fichier et division en 3 parties
                    byte[] fileData = new byte[(int) fileSize];
                    in.readFully(fileData);  // Lire tout le fichier

                    // Diviser le fichier en 3 parties égales
                    byte[] part1 = new byte[fileData.length / 3];
                    byte[] part2 = new byte[fileData.length / 3];
                    byte[] part3 = new byte[fileData.length - (fileData.length / 3) * 2];

                    System.arraycopy(fileData, 0, part1, 0, part1.length);
                    System.arraycopy(fileData, part1.length, part2, 0, part2.length);
                    System.arraycopy(fileData, part1.length + part2.length, part3, 0, part3.length);

                    // Lire les adresses IP et les ports des serveurs esclaves depuis la configuration
                    String slave1Ip = config.getProperty("serveurEsclave1.ip", "localhost");
                    int slave1Port = Integer.parseInt(config.getProperty("serveurEsclave1.port", "30004"));
                    String slave2Ip = config.getProperty("serveurEsclave2.ip", "localhost");
                    int slave2Port = Integer.parseInt(config.getProperty("serveurEsclave2.port", "30000"));
                    String slave3Ip = config.getProperty("serveurEsclave3.ip", "localhost");
                    int slave3Port = Integer.parseInt(config.getProperty("serveurEsclave3.port", "30009"));

                    // Envoyer les parties aux serveurs esclaves
                    sendFileToSlave(slave1Ip, slave1Port, part1);  // Serveur esclave 1
                    sendFileToSlave(slave2Ip, slave2Port, part2);  // Serveur esclave 2
                    sendFileToSlave(slave3Ip, slave3Port, part3);  // Serveur esclave 3

                    System.out.println("Fichier divisé et envoyé aux serveurs esclaves.");
                } catch (IOException e) {
                    System.err.println("Erreur avec le client : " + e.getMessage());
                }

                socket.close(); // Fermer la connexion avec le client
            }
        } catch (IOException e) {
            System.err.println("Erreur du ServeurPrincipal : " + e.getMessage());
        }
    }

    // Méthode pour envoyer une partie du fichier à un serveur esclave
    private static void sendFileToSlave(String slaveIp, int slavePort, byte[] filePart) {
        try (Socket socket = new Socket(slaveIp, slavePort);
             OutputStream out = socket.getOutputStream();
             DataOutputStream dataOut = new DataOutputStream(out)) {

            // Envoyer la taille de la partie du fichier
            dataOut.writeLong(filePart.length);
            dataOut.write(filePart);  // Envoyer les données de la partie du fichier

            System.out.println("Partie du fichier envoyée à " + slaveIp + ":" + slavePort);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'envoi de la partie du fichier à " + slaveIp + ":" + slavePort);
        }
    }


}
