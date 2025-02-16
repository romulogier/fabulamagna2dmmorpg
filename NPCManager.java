import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class NPCManager {
    private Map<Integer, NPC> npcs;
    private NPCLoader npcLoader;
    private int nextNpcId;
    private Timer updateTimer;
    private static final int UPDATE_INTERVAL = 50; // 20 updates por segundo
    
    public NPCManager() {
        this.npcs = new HashMap<>();
        this.npcLoader = new JSONNPCLoader();
        System.out.println("NPCManager criado!");
        this.nextNpcId = 1000; // IDs começando em 1000 para NPCs
        
        // Inicializa o timer de atualização
        this.updateTimer = new Timer();
        this.updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateNPCs();
            }
        }, 0, UPDATE_INTERVAL);
    }
    
    public void loadNPCsFromMap(String mapFile) {
        System.out.println("Tentando carregar NPCs do arquivo: " + mapFile);
        List<NPC> loadedNPCs = npcLoader.loadNPCsFromMap(mapFile);
        for (NPC npc : loadedNPCs) {
            addNPC(npc);
        }
    }
    
    public void addNPC(NPC npc) {
        npcs.put(npc.getId(), npc);
        System.out.println("NPC adicionado -> ID: " + npc.getId() + ", Tipo: " + npc.getNPCType());
    }
    
    public NPC createNPC(int x, int y, String type, NPC.NPCBehavior behavior) {
        NPC npc = new NPC(nextNpcId++, x, y, type, behavior);
        addNPC(npc);
        return npc;
    }
    
    private void updateNPCs() {
        for (NPC npc : npcs.values()) {
            npc.update();
        }
    }

    public NPC loadNPC(String npcConfigFile) {
        NPC npc = npcLoader.loadNPC(npcConfigFile);
        if (npc != null) {
            addNPC(npc);
        }
        return npc;
    }
    
    public Map<Integer, NPC> getNPCs() {
        return new HashMap<>(npcs);
    }
    
    public void removeNPC(int id) {
        npcs.remove(id);
    }
    
    public void stop() {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
    }
}