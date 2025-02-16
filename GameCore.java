import java.awt.Point;

public class GameCore {
    public static class PlayerState {
        private Point position;          // posição atual visual
        private Point targetPosition;    // posição alvo (recebida do servidor)
        private String direction;
        private int spriteIndex;
        private final int id;
        private static final float INTERPOLATION_SPEED = 0.15f; // Ajustado para ser mais suave
        
        public PlayerState(int id, int x, int y) {
            this.id = id;
            this.position = new Point(x, y);
            this.targetPosition = new Point(x, y);
            this.direction = "down";
            this.spriteIndex = 0;
        }
        
        public void setTargetPosition(Point target, String newDirection) {
            if (!direction.equals(newDirection)) {
                float smoothFactor = 0.5f; // Reduzido para ser mais suave
                this.position = new Point(
                    position.x + (int)((target.x - position.x) * smoothFactor),
                    position.y + (int)((target.y - position.y) * smoothFactor)
                );
            }
            this.targetPosition = target;
            this.direction = newDirection;
        }
        
        public void updateInterpolation() {
            if (!position.equals(targetPosition)) {
                double dx = targetPosition.x - position.x;
                double dy = targetPosition.y - position.y;
                
                // Usa double para cálculos mais precisos e evitar microtravamentos
                double newX = position.x + dx * INTERPOLATION_SPEED;
                double newY = position.y + dy * INTERPOLATION_SPEED;
                
                // Arredonda para o pixel mais próximo
                position.x = (int)Math.round(newX);
                position.y = (int)Math.round(newY);
                
                // Snap to target se estiver muito próximo
                if (Math.abs(dx) < 1.5 && Math.abs(dy) < 1.5) {
                    position.x = targetPosition.x;
                    position.y = targetPosition.y;
                }
            }
        }
        
        public Point getPosition() { return position; }
        public Point getTargetPosition() { return targetPosition; }
        public void setPosition(Point position) { this.position = position; }
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
        public int getSpriteIndex() { return spriteIndex; }
        public void setSpriteIndex(int spriteIndex) { this.spriteIndex = spriteIndex; }
        public int getId() { return id; }
    }

    public static class GameState {
        private static final int MOVE_STEP = 2;
        private static final int ANIMATION_SPEED = 150;
        private static final int SCREEN_WIDTH = 800;
        private static final int SCREEN_HEIGHT = 600;
        
        public static int getMoveStep() { return MOVE_STEP; }
        public static int getAnimationSpeed() { return ANIMATION_SPEED; }
        public static int getScreenWidth() { return SCREEN_WIDTH; }
        public static int getScreenHeight() { return SCREEN_HEIGHT; }
    }
}