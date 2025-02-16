// NPC.java
import java.awt.Point;
import java.util.List;
import java.util.ArrayList;

public class NPC extends Entity {
    protected String npcType;
    protected boolean isMovable;
    protected NPCBehavior behavior;
    
    public enum NPCBehavior {
        STATIONARY,
        WANDER,
        FOLLOW_PATH
    }
    
    public NPC(int id, int x, int y, String npcType, NPCBehavior behavior) {
        super(id, x, y);
        this.npcType = npcType;
        this.behavior = behavior;
        this.isMovable = behavior != NPCBehavior.STATIONARY;
    }
    
    @Override
    public void update() {
        switch (behavior) {
            case WANDER:
                updateWanderBehavior();
                break;
            case FOLLOW_PATH:
                updatePathBehavior();
                break;
            case STATIONARY:
                break;
        }
        
        updateInterpolation();
    }
    
    private void updateWanderBehavior() {
        if (state == EntityState.IDLE && Math.random() < 0.02) {
            int dx = (int)(Math.random() * 3) - 1;
            int dy = (int)(Math.random() * 3) - 1;
            
            Point newTarget = new Point(
                position.x + dx * 32,
                position.y + dy * 32
            );
            
            setTargetPosition(newTarget);
            setDirection(calculateDirection(dx, dy));
        }
    }
    
    private void updatePathBehavior() {
        // Implementação futura de seguir caminho
    }
    
    private String calculateDirection(int dx, int dy) {
        if (dx == 0 && dy < 0) return "top";
        if (dx == 0 && dy > 0) return "down";
        if (dx < 0 && dy == 0) return "left";
        if (dx > 0 && dy == 0) return "right";
        return direction;
    }
    
    public String getNPCType() { return npcType; }
    public NPCBehavior getBehavior() { return behavior; }
}