import javax.swing.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.awt.Point;

public class GameClient {
    private JFrame frame;
    private SpriteRenderer renderer;
    private TiledMapRenderer mapRenderer;
    private NetworkManager network;
    private InputHandler input;
    private NPCManager npcManager;  // Novo campo para gerenciar NPCs
    private Map<Integer, GameCore.PlayerState> players;
    private Map<Integer, Long> lastPlayerAnimationTimes;
    private int playerId;
    private boolean isMoving;
    private static final int ANIMATION_CHECK_INTERVAL = 16;
    private static final int INTERPOLATION_INTERVAL = 16;
    private Camera camera;
    private Map<Integer, Long> lastDirectionChangeTime = new HashMap<>();

    public GameClient() {
        players = new HashMap<>();
        lastPlayerAnimationTimes = new HashMap<>();
        network = new NetworkManager();
        renderer = new SpriteRenderer();
        mapRenderer = new TiledMapRenderer();
        input = new InputHandler(this::handleInput);
        npcManager = new NPCManager();  // Inicializa o gerenciador de NPCs
        isMoving = false;
    }

    public void start() {
        initializeWindow();
        connectToServer();
        startGameLoop();
    }

    private void initializeWindow() {
        frame = new JFrame("MMO Simples");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(frame.getSize());
        mapRenderer.loadMap("resources/maps/mapa1.tmx");
        camera = new Camera(
            frame.getWidth(),
            frame.getHeight(),
            mapRenderer.getMapWidth() * mapRenderer.getTileWidth(),
            mapRenderer.getMapHeight() * mapRenderer.getTileHeight()
        );
        mapRenderer.setCamera(camera);
        renderer.setCamera(camera);
        mapRenderer.setBounds(0, 0, frame.getWidth(), frame.getHeight());
        layeredPane.add(mapRenderer, Integer.valueOf(0));
        renderer.setBounds(0, 0, frame.getWidth(), frame.getHeight());
        renderer.setOpaque(false);
        layeredPane.add(renderer, Integer.valueOf(1));
        frame.add(layeredPane);
        frame.addKeyListener(input);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Carregar NPCs do mapa
        npcManager.loadNPCsFromMap("resources/npcs/mapa1_npcs.json");
    }

    private void connectToServer() {
        try {
            network.connect(this::handleServerMessage);
            playerId = Integer.parseInt(network.readInitialMessage());
            System.out.println("Conectado com ID: " + playerId);
        } catch (IOException e) {
            System.out.println("Erro ao conectar: " + e.getMessage());
            System.exit(1);
        }
    }

    private void handleServerMessage(String message) {
        if (message.startsWith("Jogador")) {
            System.out.println(message);
            return;
        }
    
        Map<Integer, GameCore.PlayerState> newPlayers = new HashMap<>();
        Map<Integer, NPC> newNPCs = new HashMap<>();
        
        long currentTime = System.currentTimeMillis();
        
        String[] entityData = message.split(",");
        
        for (int i = 0; i < entityData.length; i += 7) {  // Avança de 7 em 7 para players, ou 8 para NPCs
            if (i + 6 >= entityData.length) break;
            
            String type = entityData[i];
            int id = Integer.parseInt(entityData[i + 1]);
            int x = Integer.parseInt(entityData[i + 2]);
            int y = Integer.parseInt(entityData[i + 3]);
            String direction = entityData[i + 4];
            int spriteIndex = Integer.parseInt(entityData[i + 5]);
            
            if (type.equals("P")) {
                boolean isPlayerMoving = Boolean.parseBoolean(entityData[i + 6]);
                GameCore.PlayerState state = processPlayerState(
                    id, x, y, isPlayerMoving, currentTime, direction
                );
                newPlayers.put(id, state);
            } else if (type.equals("N")) {
                if (i + 7 >= entityData.length) break;
                String npcType = entityData[i + 7];
                NPC npc = npcManager.getNPCs().get(id);
                if (npc == null) {
                    npc = new NPC(id, x, y, npcType, NPC.NPCBehavior.WANDER);
                    npcManager.addNPC(npc);
                }
                npc.setPosition(new Point(x, y));
                npc.setDirection(direction);
                npc.setSpriteIndex(spriteIndex);
                npc.state = Boolean.parseBoolean(entityData[i + 6]) ? 
                    Entity.EntityState.MOVING : Entity.EntityState.IDLE;
                newNPCs.put(id, npc);
                i++; // Ajusta o índice para o campo extra do NPC
            }
        }
        
        // Atualiza players e NPCs
        if (!players.equals(newPlayers)) {
            players = newPlayers;
            SwingUtilities.invokeLater(() -> {
                renderer.updatePlayers(players);
                renderer.updateNPCs(newNPCs);
            });
        }
    }
    // Método extraído para processamento de estado do player
    private GameCore.PlayerState processPlayerState(int id, int x, int y, boolean isPlayerMoving, long currentTime, String direction) {
        GameCore.PlayerState state;
        if (!players.containsKey(id)) {
            state = new GameCore.PlayerState(id, x, y);
            lastPlayerAnimationTimes.put(id, currentTime);
            lastDirectionChangeTime.put(id, currentTime);
        } else {
            state = players.get(id);
            Point currentPos = state.getPosition();
            Point newPos = new Point(x, y);
            
            double distance = currentPos.distance(newPos);
            
            if (distance > 100) {
                state.setPosition(newPos);
                state.setTargetPosition(newPos, direction);
            } else {
                long timeSinceLastDirectionChange = currentTime - lastDirectionChangeTime.getOrDefault(id, currentTime);
                
                if (!state.getDirection().equals(direction)) {
                    if (timeSinceLastDirectionChange < 200) {
                        direction = state.getDirection();
                    } else {
                        lastDirectionChangeTime.put(id, currentTime);
                    }
                }
                
                state.setDirection(direction);
                state.setTargetPosition(newPos, direction);
            }
            
            if (isPlayerMoving) {
                long lastTime = lastPlayerAnimationTimes.getOrDefault(id, currentTime);
                if (currentTime - lastTime >= GameCore.GameState.getAnimationSpeed()) {
                    state.setSpriteIndex((state.getSpriteIndex() + 1) % 2);
                    lastPlayerAnimationTimes.put(id, currentTime);
                }
            } else {
                state.setSpriteIndex(0);
            }
        }
        return state;
    }
    

    private void handleInput() {
        // Código de input existente (sem modificações)
        GameCore.PlayerState currentPlayer = players.get(playerId);
        if (currentPlayer == null) return;
    
        if (input.hasOppositeKeysPressed()) {
            isMoving = false;
            network.sendMessage("");
            return;
        }
    
        String move = "";
        
        if (input.isKeyPressed(0)) {
            move = "W";
            isMoving = true;
        }
        if (input.isKeyPressed(1)) {
            move = "A";
            isMoving = true;
        }
        if (input.isKeyPressed(2)) {
            move = "S";
            isMoving = true;
        }
        if (input.isKeyPressed(3)) {
            move = "D";
            isMoving = true;
        }
        
        if (isMoving && !move.isEmpty()) {
            network.sendMessage(move);
        } else {
            isMoving = false;
            network.sendMessage("");
        }
    }

    private void updateInterpolation() {
        for (GameCore.PlayerState player : players.values()) {
            player.updateInterpolation();
            if (player.getId() == playerId) {
                camera.centerOn(player.getPosition());
            }
        }
        
        // Atualiza interpolação dos NPCs
        for (NPC npc : npcManager.getNPCs().values()) {
            npc.updateInterpolation();
        }
        
        camera.updateInterpolation();
        SwingUtilities.invokeLater(() -> {
            renderer.updatePlayers(players);
            renderer.updateNPCs(npcManager.getNPCs());
        });
    }

    private void startGameLoop() {
        // Thread dedicada para processamento de input e lógica
        Thread gameThread = new Thread(() -> {
            while (true) {
                handleInput();
                updateInterpolation();
                
                try {
                    // ~60 fps (16.66ms)
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        
        gameThread.setPriority(Thread.MAX_PRIORITY);
        gameThread.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GameClient().start();
        });
    }
}