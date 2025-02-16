// NetworkManager.java
import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class NetworkManager {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;
    
    public void connect(Consumer<String> onMessageReceived) throws IOException {
        socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        listenerThread = new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    onMessageReceived.accept(message);
                }
            } catch (IOException e) {
                System.out.println("Desconectado do servidor.");
            }
        });
        listenerThread.start();
    }
    
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
    
    public String readInitialMessage() throws IOException {
        return in != null ? in.readLine() : null;
    }
    
    public void disconnect() {
        try {
            if (listenerThread != null) {
                listenerThread.interrupt();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Erro ao desconectar: " + e.getMessage());
        }
    }
}