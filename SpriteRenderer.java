import javax.swing.*;
import java.awt.*;
import java.awt.image.VolatileImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpriteRenderer extends JPanel {
    private Image spriteSheet;
    private Image npcSpriteSheet;
    private final Map<String, Rectangle> playerFrameCoordinates;
    private final Map<String, Rectangle> npcFrameCoordinates;
    private final Map<Integer, GameCore.PlayerState> players;
    private final Map<Integer, NPC> npcs;
    private Camera camera;
    private final RenderingHints renderingHints;
    private VolatileImage backBuffer;
    private final Object renderLock = new Object();
    
    public SpriteRenderer() {
        // Use ConcurrentHashMap para evitar problemas de concorrência
        players = new ConcurrentHashMap<>();
        npcs = new ConcurrentHashMap<>();
        playerFrameCoordinates = new HashMap<>();
        npcFrameCoordinates = new HashMap<>();
        
        // Pré-configura RenderingHints
        renderingHints = new RenderingHints(
            RenderingHints.KEY_ANTIALIASING, 
            RenderingHints.VALUE_ANTIALIAS_ON
        );
        renderingHints.put(
            RenderingHints.KEY_INTERPOLATION, 
            RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );
        
        // Carrega recursos de forma assíncrona
        SwingWorker<Void, Void> resourceLoader = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                loadResources();
                return null;
            }
        };
        resourceLoader.execute();
        
        // Configura o painel
        setOpaque(false);
        setDoubleBuffered(true);
    }
    
    private void loadResources() {
        spriteSheet = new ImageIcon(getClass().getResource("/resources/player2.png")).getImage();
        npcSpriteSheet = new ImageIcon(getClass().getResource("/resources/npcs.png")).getImage();
        initializeFrameCoordinates();
    }
    
    private void initializeFrameCoordinates() {
        // Player frames
        playerFrameCoordinates.put("down1", new Rectangle(1, 0, 33, 49));
        playerFrameCoordinates.put("down2", new Rectangle(35, 0, 33, 49));
        playerFrameCoordinates.put("top1", new Rectangle(0, 145, 33, 49));
        playerFrameCoordinates.put("top2", new Rectangle(32, 145, 33, 49));
        playerFrameCoordinates.put("left1", new Rectangle(0, 48, 33, 49));
        playerFrameCoordinates.put("left2", new Rectangle(33, 48, 33, 49));
        playerFrameCoordinates.put("right1", new Rectangle(0, 96, 33, 49));
        playerFrameCoordinates.put("right2", new Rectangle(34, 96, 33, 49));
        
        // NPC frames
        npcFrameCoordinates.put("merchant_down1", new Rectangle(1, 0, 33, 49));
        npcFrameCoordinates.put("merchant_down2", new Rectangle(35, 0, 33, 49));
        npcFrameCoordinates.put("merchant_left1", new Rectangle(0, 48, 33, 49));
        npcFrameCoordinates.put("merchant_left2", new Rectangle(33, 48, 33, 49));
        npcFrameCoordinates.put("merchant_right1", new Rectangle(0, 96, 33, 49));
        npcFrameCoordinates.put("merchant_right2", new Rectangle(34, 96, 33, 49));
        npcFrameCoordinates.put("merchant_top1", new Rectangle(0, 145, 33, 49));
        npcFrameCoordinates.put("merchant_top2", new Rectangle(32, 145, 33, 49));
    }
    
    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        createBackBuffer();
    }
    
    private void createBackBuffer() {
        if (getWidth() <= 0 || getHeight() <= 0) return;
        
        if (backBuffer == null || backBuffer.getWidth() != getWidth() || 
            backBuffer.getHeight() != getHeight() || backBuffer.contentsLost()) {
            
            if (backBuffer != null) {
                backBuffer.flush();
            }
            
            GraphicsConfiguration gc = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();
            backBuffer = gc.createCompatibleVolatileImage(getWidth(), getHeight(), Transparency.TRANSLUCENT);
        }
    }
    
    public void setCamera(Camera camera) {
        this.camera = camera;
    }
    
    public void updatePlayers(Map<Integer, GameCore.PlayerState> newPlayers) {
        synchronized(renderLock) {
            players.clear();
            players.putAll(newPlayers);
        }
        repaint();
    }
    
    public void updateNPCs(Map<Integer, NPC> newNPCs) {
        synchronized(renderLock) {
            npcs.clear();
            npcs.putAll(newNPCs);
        }
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (camera == null || backBuffer == null) return;
        
        // Verificar e revalidar o backbuffer se necessário
        if (backBuffer.validate(getGraphicsConfiguration()) == VolatileImage.IMAGE_INCOMPATIBLE) {
            createBackBuffer();
        }
        
        // Renderizar para o backbuffer
        do {
            Graphics2D g2d = backBuffer.createGraphics();
            g2d.setRenderingHints(renderingHints);
            
            // Limpar o backbuffer
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setComposite(AlphaComposite.SrcOver);
            
            // Renderizar entidades
            synchronized(renderLock) {
                renderPlayers(g2d);
                renderNPCs(g2d);
            }
            
            g2d.dispose();
        } while (backBuffer.contentsLost());
        
        // Desenhar o backbuffer na tela
        g.drawImage(backBuffer, 0, 0, null);
    }
    
    private void renderPlayers(Graphics2D g2d) {
        Rectangle viewport = new Rectangle(
            camera.getPosition().x,
            camera.getPosition().y,
            getWidth(),
            getHeight()
        );
        
        for (GameCore.PlayerState player : players.values()) {
            Point worldPos = player.getPosition();
            if (isInViewport(worldPos, viewport)) {
                Point screenPos = camera.worldToScreen(worldPos);
                String frameKey = player.getDirection() + (player.getSpriteIndex() + 1);
                Rectangle frameRect = playerFrameCoordinates.get(frameKey);
                
                if (frameRect != null) {
                    g2d.drawImage(spriteSheet,
                        screenPos.x,
                        screenPos.y,
                        screenPos.x + frameRect.width,
                        screenPos.y + frameRect.height,
                        frameRect.x,
                        frameRect.y,
                        frameRect.x + frameRect.width,
                        frameRect.y + frameRect.height,
                        null);
                }
            }
        }
    }
    
    private void renderNPCs(Graphics2D g2d) {
        Rectangle viewport = new Rectangle(
            camera.getPosition().x,
            camera.getPosition().y,
            getWidth(),
            getHeight()
        );
        
        for (NPC npc : npcs.values()) {
            Point worldPos = npc.getPosition();
            if (isInViewport(worldPos, viewport)) {
                Point screenPos = camera.worldToScreen(worldPos);
                String frameKey = npc.getNPCType() + "_" + npc.getDirection() + (npc.getSpriteIndex() + 1);
                Rectangle frameRect = npcFrameCoordinates.get(frameKey);
                
                if (frameRect != null) {
                    g2d.drawImage(npcSpriteSheet,
                        screenPos.x,
                        screenPos.y,
                        screenPos.x + frameRect.width,
                        screenPos.y + frameRect.height,
                        frameRect.x,
                        frameRect.y,
                        frameRect.x + frameRect.width,
                        frameRect.y + frameRect.height,
                        null);
                }
            }
        }
    }
    
    private boolean isInViewport(Point worldPos, Rectangle viewport) {
        return viewport.contains(worldPos.x, worldPos.y);
    }
}