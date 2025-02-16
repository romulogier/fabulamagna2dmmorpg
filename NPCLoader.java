// NPCLoader.java
import java.util.List;

public interface NPCLoader {
    NPC loadNPC(String npcConfigFile);
    List<NPC> loadNPCsFromMap(String mapFile);
}