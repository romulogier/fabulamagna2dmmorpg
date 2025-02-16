import java.awt.Point;

public class Camera {
    private Point position;
    private Point targetPosition;
    private final int viewportWidth;
    private final int viewportHeight;
    private final int mapWidth;
    private final int mapHeight;
    private static final float CAMERA_INTERPOLATION_SPEED = 0.1f; // mais suave que o player
    
    public Camera(int viewportWidth, int viewportHeight, int mapWidth, int mapHeight) {
        this.position = new Point(0, 0);
        this.targetPosition = new Point(0, 0);
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
    }
    
    public void centerOn(Point target) {
        // Calcula a posição alvo da câmera
        int targetX = target.x - viewportWidth / 2;
        int targetY = target.y - viewportHeight / 2;
        
        // Ajusta a posição alvo para não mostrar área fora do mapa
        targetX = Math.max(0, Math.min(targetX, mapWidth - viewportWidth));
        targetY = Math.max(0, Math.min(targetY, mapHeight - viewportHeight));
        
        // Define a posição alvo
        this.targetPosition = new Point(targetX, targetY);
    }
    
    public void updateInterpolation() {
        if (!position.equals(targetPosition)) {
            // Interpolação linear
            position.x = position.x + (int)((targetPosition.x - position.x) * CAMERA_INTERPOLATION_SPEED);
            position.y = position.y + (int)((targetPosition.y - position.y) * CAMERA_INTERPOLATION_SPEED);
            
            // Se estiver muito próximo do alvo, snap para a posição final
            if (Math.abs(position.x - targetPosition.x) < 2 && Math.abs(position.y - targetPosition.y) < 2) {
                position.x = targetPosition.x;
                position.y = targetPosition.y;
            }
        }
    }
    
    public Point getPosition() {
        return position;
    }
    
    public Point worldToScreen(Point worldPos) {
        return new Point(
            worldPos.x - position.x,
            worldPos.y - position.y
        );
    }
    
    public Point screenToWorld(Point screenPos) {
        return new Point(
            screenPos.x + position.x,
            screenPos.y + position.y
        );
    }
}