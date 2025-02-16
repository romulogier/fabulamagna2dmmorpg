// Entity.java
import java.awt.Point;

public abstract class Entity {
    protected final int id;
    protected Point position;
    protected Point targetPosition;
    protected String direction;
    protected int spriteIndex;
    protected float movementSpeed;
    protected EntityState state;
    
    public enum EntityState {
        IDLE,
        MOVING,
        INTERACTING
    }
    
    public Entity(int id, int x, int y) {
        this.id = id;
        this.position = new Point(x, y);
        this.targetPosition = new Point(x, y);
        this.direction = "down";
        this.spriteIndex = 0;
        this.movementSpeed = 1.0f;
        this.state = EntityState.IDLE;
    }
    
    public void updateInterpolation() {
        if (!position.equals(targetPosition)) {
            float dx = targetPosition.x - position.x;
            float dy = targetPosition.y - position.y;
            
            position.x += dx * movementSpeed;
            position.y += dy * movementSpeed;
            
            if (Math.abs(dx) < 1.5 && Math.abs(dy) < 1.5) {
                position.x = targetPosition.x;
                position.y = targetPosition.y;
                state = EntityState.IDLE;
            }
        }
    }
    
    public abstract void update();
    
    // Getters e setters
    public int getId() { return id; }
    public Point getPosition() { return position; }
    public void setPosition(Point position) { this.position = position; }
    public void setTargetPosition(Point target) { 
        this.targetPosition = target; 
        this.state = EntityState.MOVING;
    }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public int getSpriteIndex() { return spriteIndex; }
    public void setSpriteIndex(int spriteIndex) { this.spriteIndex = spriteIndex; }
    public EntityState getState() { return state; }
}