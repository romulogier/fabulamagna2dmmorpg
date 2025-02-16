import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.Point;
import java.awt.Rectangle;
import org.w3c.dom.*;
import javax.xml.parsers.*;

public class GameServer {
    private List<ClientConnection> clients;
    private NPCManager npcManager;  // Novo campo para gerenciar NPCs
    private int nextPlayerId;
    private int mapWidth;
    private int mapHeight;
    private Timer movementTimer;
    private List<Rectangle> mapColliders;
    private static final int MOVE_DELAY = 16;

    public GameServer() {
        clients = Collections.synchronizedList(new ArrayList<>());
        nextPlayerId = 1;
        mapWidth = 40 * 32;
        mapHeight = 30 * 32;
        
        // Carregar colisores do mapa
        mapColliders = loadMapColliders("resources/maps/mapa1.tmx");
        
        // Inicializar NPCManager
        npcManager = new NPCManager();
        npcManager.loadNPCsFromMap("resources/npcs/mapa1_npcs.json");
        
        movementTimer = new Timer();
        movementTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronized(clients) {
                    boolean anyPlayerMoved = false;
                    for (ClientConnection client : clients) {
                        if (client.updateMovement()) {
                            anyPlayerMoved = true;
                        }
                    }

                    // Atualizar NPCs
                    boolean anyNPCMoved = updateNPCs();

                    if (anyPlayerMoved || anyNPCMoved) {
                        broadcastGameState();
                    }
                }
            }
        }, 0, MOVE_DELAY);
    }

    // Método para atualizar NPCs
    private boolean updateNPCs() {
        boolean anyNPCMoved = false;
        Map<Integer, NPC> npcs = npcManager.getNPCs();
        
        for (NPC npc : npcs.values()) {
            npc.update();  // Chama o método de update do NPC
            
            // Verifica se o NPC se moveu (baseado no estado)
            if (npc.getState() == Entity.EntityState.MOVING) {
                anyNPCMoved = true;
            }
        }
        
        return anyNPCMoved;
    }

    private void broadcastGameState() {
        List<String> states = new ArrayList<>();
        
        synchronized (clients) {
            // Processar estados dos players
            for (ClientConnection client : clients) {
                GameCore.PlayerState state = client.getPlayerState();
                    states.add(String.format("P,%d,%d,%d,%s,%d,%b",
                    state.getId(),
                    state.getPosition().x,
                    state.getPosition().y,
                    state.getDirection(),
                    state.getSpriteIndex(),
                    client.isMoving()
            ));
            }
            
            // Processar estados dos NPCs
            Map<Integer, NPC> npcs = npcManager.getNPCs();
            for (NPC npc : npcs.values()) {
                states.add(String.format("N,%d,%d,%d,%s,%d,%b,%s",
                    npc.getId(),
                    npc.getPosition().x,
                    npc.getPosition().y,
                    npc.getDirection(),
                    npc.getSpriteIndex(),
                    npc.getState() == Entity.EntityState.MOVING,
                    npc.getNPCType()
            ));
            }
        }
        
        String gameState = String.join(",", states);
        broadcast(gameState);
    }

    // Método para carregar colisores do mapa
    private List<Rectangle> loadMapColliders(String mapPath) {
        List<Rectangle> colliders = new ArrayList<>();
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(mapPath));
            
            NodeList objectGroups = doc.getElementsByTagName("objectgroup");
            for (int i = 0; i < objectGroups.getLength(); i++) {
                Element objectGroup = (Element) objectGroups.item(i);
                if ("colisores".equals(objectGroup.getAttribute("name"))) {
                    NodeList objects = objectGroup.getElementsByTagName("object");
                    for (int j = 0; j < objects.getLength(); j++) {
                        Element object = (Element) objects.item(j);
                        
                        int x = parseIntAttribute(object, "x", 0);
                        int y = parseIntAttribute(object, "y", 0);
                        int width = parseIntAttribute(object, "width", 32);
                        int height = parseIntAttribute(object, "height", 32);
                        
                        colliders.add(new Rectangle(x, y, width, height));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar colisores: " + mapPath);
            e.printStackTrace();
        }
        
        return colliders;
    }

    // Método auxiliar para parsing de atributos
    private int parseIntAttribute(Element element, String attribute, int defaultValue) {
        String value = element.getAttribute(attribute);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // Método de verificação de colisão
    private boolean checkCollision(Point pos, List<Rectangle> colliders) {
        Rectangle playerRect = new Rectangle(
            pos.x + 10,          // Desloca um pouco mais para dentro no eixo X
            pos.y + 32,          // Começa na base do sprite 
            12,                  // Largura ainda menor
            14                   // Altura um pouco menor que antes
        );
        
        for (Rectangle collider : colliders) {
            Rectangle bufferedCollider = new Rectangle(
                collider.x - 2,
                collider.y - 2,
                collider.width + 4,
                collider.height + 4
            );
            
            if (playerRect.intersects(bufferedCollider)) {
                return true; // Colidiu com um objeto
            }
        }
        return false;
    }

    private void broadcast(String message) {
        synchronized (clients) {
            clients.forEach(client -> sendMessage(client.getSocket(), message));
        }
    }

    private void sendMessage(Socket socket, String message) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        } catch (IOException e) {
            System.out.println("Erro ao enviar mensagem: " + e.getMessage());
        }
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Servidor iniciado na porta 12345");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado");
                ClientConnection client = new ClientConnection(clientSocket, nextPlayerId++);
                clients.add(client);
                new Thread(client).start();
                sendMessage(clientSocket, String.valueOf(client.getPlayerId()));
                
                // Broadcast estado inicial incluindo NPCs
                broadcastGameState();
            }
        } catch (IOException e) {
            System.out.println("Erro no servidor: " + e.getMessage());
        }
    }

    private enum MovementState {
        IDLE,               // Parado
        COMPLETING_SQM,     // Completando o quadrado atual
        PAUSED,            // Em pausa forçada após completar
        MOVING             // Em movimento normal
    }

    private class ClientConnection implements Runnable {
        private Socket socket;
        private GameCore.PlayerState playerState;
        private static final int PIXELS_PER_SQM = 32;
        private static final long DIRECTION_CHANGE_PAUSE_DURATION = 200;
        private int pixelsMovedInCurrentSQM = 0;
        private String currentDirection = null;
        private String nextDirection = null;
        private String activeInput = null;
        private MovementState movementState = MovementState.IDLE;
        private long pauseEndTime = 0;
        private long lastInputTime;
        private static final long INPUT_BUFFER_DURATION = 300; // 300ms para buffer de input

        public ClientConnection(Socket socket, int playerId) {
            this.socket = socket;
            this.playerState = new GameCore.PlayerState(
                playerId,
                new Random().nextInt(mapWidth - 50),
                new Random().nextInt(mapHeight - 50)
            );
        }

        public Socket getSocket() { return socket; }
        public int getPlayerId() { return playerState.getId(); }
        public GameCore.PlayerState getPlayerState() { return playerState; }
        public boolean isMoving() { return movementState == MovementState.MOVING || movementState == MovementState.COMPLETING_SQM; }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String input;
                while ((input = in.readLine()) != null) {
                    if (input.isEmpty()) {
                        activeInput = null;
                        continue;
                    }
                    
                    lastInputTime = System.currentTimeMillis();
                    activeInput = input;

                    if (movementState == MovementState.COMPLETING_SQM || 
                        movementState == MovementState.PAUSED) {
                        if (!input.equals(currentDirection)) {
                            nextDirection = input;
                        }
                        continue;
                    }

                    if (movementState == MovementState.MOVING && !input.equals(currentDirection)) {
                        movementState = MovementState.COMPLETING_SQM;
                        nextDirection = input;
                        continue;
                    }

                    handleMovement(input);
                }
            } catch (IOException e) {
                System.out.println("Cliente desconectado: " + e.getMessage());
            } finally {
                removeClient(this);
            }
        }

        public boolean updateMovement() {
            switch (movementState) {
                case COMPLETING_SQM:
                    if (pixelsMovedInCurrentSQM >= PIXELS_PER_SQM) {
                        movementState = MovementState.PAUSED;
                        pauseEndTime = System.currentTimeMillis() + DIRECTION_CHANGE_PAUSE_DURATION;
                        pixelsMovedInCurrentSQM = 0;
                        return true;
                    }
                    moveInDirection(currentDirection);
                    pixelsMovedInCurrentSQM += GameCore.GameState.getMoveStep();
                    return true;

                case PAUSED:
                    if (System.currentTimeMillis() >= pauseEndTime) {
                        currentDirection = null;
                        movementState = MovementState.IDLE;
                        
                        if (nextDirection != null && 
                            System.currentTimeMillis() - lastInputTime < INPUT_BUFFER_DURATION) {
                            handleMovement(nextDirection);
                            nextDirection = null;
                        }
                    }
                    return true;

                case MOVING:
                    if (pixelsMovedInCurrentSQM >= PIXELS_PER_SQM) {
                        pixelsMovedInCurrentSQM = 0;
                        
                        if (activeInput == null && 
                            System.currentTimeMillis() - lastInputTime >= INPUT_BUFFER_DURATION) {
                            movementState = MovementState.IDLE;
                            currentDirection = null;
                            return true;
                        }
                    }
                    
                    moveInDirection(currentDirection);
                    pixelsMovedInCurrentSQM += GameCore.GameState.getMoveStep();
                    return true;

                case IDLE:
                    if (activeInput != null || 
                        (nextDirection != null && 
                         System.currentTimeMillis() - lastInputTime < INPUT_BUFFER_DURATION)) {
                        handleMovement(activeInput != null ? activeInput : nextDirection);
                    }
                    return false;
            }
            return false;
        }
        
        private void moveInDirection(String direction) {
            Point pos = playerState.getPosition();
            switch (direction) {
                case "W": pos.y -= GameCore.GameState.getMoveStep(); break;
                case "A": pos.x -= GameCore.GameState.getMoveStep(); break;
                case "S": pos.y += GameCore.GameState.getMoveStep(); break;
                case "D": pos.x += GameCore.GameState.getMoveStep(); break;
            }
        }

        private boolean handleMovement(String input) {
            Point pos = playerState.getPosition();
            Point newPos = new Point(pos);
            
            switch (input) {
                case "W": newPos.y -= GameCore.GameState.getMoveStep(); break;
                case "A": newPos.x -= GameCore.GameState.getMoveStep(); break;
                case "S": newPos.y += GameCore.GameState.getMoveStep(); break;
                case "D": newPos.x += GameCore.GameState.getMoveStep(); break;
            }
            
            boolean canMove = false;
            switch (input) {
                case "W": canMove = newPos.y > 0; break;
                case "A": canMove = newPos.x > 0; break;
                case "S": canMove = newPos.y < mapHeight - 50; break;
                case "D": canMove = newPos.x < mapWidth - 50; break;
            }
            
            if (canMove) {
                boolean collision = checkCollision(newPos, mapColliders);
                
                movementState = MovementState.IDLE;
                currentDirection = null;
                
                if (!collision) {
                    movementState = MovementState.MOVING;
                    currentDirection = input;
                    pixelsMovedInCurrentSQM = 0;
                    playerState.setDirection(getDirectionString(input));
                    return true;
                }
            }
            
            return false;
        }

        private String getDirectionString(String input) {
            switch (input) {
                case "W": return "top";
                case "A": return "left";
                case "S": return "down";
                case "D": return "right";
                default: return "down";
            }
        }
    }

    private void removeClient(ClientConnection client) {
        clients.remove(client);
        try {
            client.getSocket().close();
        } catch (IOException e) {
            System.out.println("Erro ao desconectar cliente: " + e.getMessage());
        }
        broadcastGameState();
    }

    public static void main(String[] args) {
        new GameServer().start();
    }
}