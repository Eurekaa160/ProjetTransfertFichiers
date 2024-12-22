import java.io.*;
import java.net.*;
import javax.swing.*;
import java.util.Properties;

public class FileClient {
    public static void main(String[] args) {
        Properties config = new Properties();

        // Charger la configuration
        try (FileInputStream fis = new FileInputStream("../config.properties")) {
            config.load(fis);
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du fichier de configuration : " + e.getMessage());
            return;
        }

        // Lire l'adresse IP et le port du serveur à partir du fichier de configuration
        String serverAddress = config.getProperty("serveurPrincipal.ip", "127.0.0.1"); // Adresse IP du serveur
        int port = Integer.parseInt(config.getProperty("serveurPrincipal.port", "8085")); // Port du serveur

        // Utiliser JFileChooser pour choisir un fichier
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile(); // Fichier sélectionné
            System.out.println("Fichier choisi : " + file.getAbsolutePath());

            try (Socket socket = new Socket(serverAddress, port)) {
                System.out.println("Connecté au serveur : " + serverAddress);

                // Envoyer le fichier au serveur
                sendFile(socket, file);

            } catch (IOException e) {
                System.err.println("Erreur de connexion au serveur : " + e.getMessage());
            }
        } else {
            System.out.println("Aucun fichier sélectionné.");
        }
    }

    private static void sendFile(Socket socket, File file) {
        try (
            // Flux de sortie pour envoyer les données au serveur
            OutputStream out = socket.getOutputStream();
            DataOutputStream dataOut = new DataOutputStream(out);
            FileInputStream fileIn = new FileInputStream(file)
        ) {
            // Envoyer le nom du fichier
            dataOut.writeUTF(file.getName());

            // Envoyer la taille du fichier
            long fileSize = file.length();
            dataOut.writeLong(fileSize);

            // Envoyer les données du fichier
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            System.out.println("Fichier envoyé avec succès.");

        } catch (IOException e) {
            System.err.println("Erreur lors de l'envoi du fichier : " + e.getMessage());
        }
    }
}
