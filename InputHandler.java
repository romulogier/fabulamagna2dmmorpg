import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class InputHandler implements KeyListener {
    private boolean[] keys;
    private boolean[] previousKeys;
    private Runnable onInputChanged;

    public InputHandler(Runnable onInputChanged) {
        this.keys = new boolean[4]; // W, A, S, D
        this.previousKeys = new boolean[4];
        this.onInputChanged = onInputChanged;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        System.arraycopy(keys, 0, previousKeys, 0, keys.length);
        updateKey(e.getKeyCode(), true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        System.arraycopy(keys, 0, previousKeys, 0, keys.length);
        updateKey(e.getKeyCode(), false);
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    private void updateKey(int keyCode, boolean pressed) {
        switch (keyCode) {
            case KeyEvent.VK_W: keys[0] = pressed; break;
            case KeyEvent.VK_A: keys[1] = pressed; break;
            case KeyEvent.VK_S: keys[2] = pressed; break;
            case KeyEvent.VK_D: keys[3] = pressed; break;
        }
        onInputChanged.run();
    }

    public boolean isKeyPressed(int index) {
        return keys[index];
    }

    // Retorna true se teclas opostas estão pressionadas
    public boolean hasOppositeKeysPressed() {
        return (keys[0] && keys[2]) || // W e S
               (keys[1] && keys[3]);   // A e D
    }

    // Retorna o número de teclas pressionadas
    public int getNumberOfKeysPressed() {
        int count = 0;
        for (boolean key : keys) {
            if (key) count++;
        }
        return count;
    }
}