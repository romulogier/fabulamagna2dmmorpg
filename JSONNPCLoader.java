import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

public class JSONNPCLoader implements NPCLoader {
    @Override
    public List<NPC> loadNPCsFromMap(String mapFile) {
        List<NPC> npcs = new ArrayList<>();
        try {
            System.out.println("1. Iniciando carregamento do arquivo");
            InputStream is = getClass().getResourceAsStream(mapFile);
            System.out.println("2. InputStream obtido: " + (is != null ? "sim" : "não"));
            
            if (is == null) return npcs;
            
            String jsonString = readStream(is);
            System.out.println("3. Conteúdo do arquivo: " + jsonString);
            
            Map<String, Object> mapData = parseJSON(jsonString);
            System.out.println("4. JSON parseado: " + mapData);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> npcArray = (List<Map<String, Object>>) mapData.get("npcs");
            System.out.println("5. Array de NPCs obtido: " + npcArray);
            
            if (npcArray != null) {
                for (Map<String, Object> npcData : npcArray) {
                    NPC npc = createNPCFromMap(npcData);
                    System.out.println("6. NPC criado: " + npc);
                    if (npc != null) {
                        npcs.add(npc);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar NPCs do mapa: " + e.getMessage());
            e.printStackTrace();
        }
        return npcs;
    }

    @Override
    public NPC loadNPC(String npcConfigFile) {
        List<NPC> npcs = loadNPCsFromMap(npcConfigFile);
        if (!npcs.isEmpty()) {
            return npcs.get(0);
        }
        return null;
    }
    
    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
    
    private Map<String, Object> parseJSON(String json) {
        Map<String, Object> result = new HashMap<>();
        json = json.trim();
        
        if (json.startsWith("{")) {
            // Remove as chaves externas
            json = json.substring(1, json.length() - 1).trim();
            
            // Procura pela chave "npcs"
            if (json.contains("\"npcs\"")) {
                // Encontra o array
                int startArray = json.indexOf("[");
                int endArray = json.lastIndexOf("]");
                
                if (startArray != -1 && endArray != -1) {
                    String arrayContent = json.substring(startArray + 1, endArray).trim();
                    result.put("npcs", parseObjectArray(arrayContent));
                }
            } else {
                // É um objeto individual
                parseKeyValuePairs(json, result);
            }
        }
        return result;
    }

    private void parseKeyValuePairs(String json, Map<String, Object> result) {
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            pair = pair.trim();
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replace("\"", "");
                String value = keyValue[1].trim().replace("\"", "");
                result.put(key, value);
                // Converte para número se possível
                try {
                    result.put(key, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    // Mantém como string se não for número
                }
            }
        }
    }

    private List<Map<String, Object>> parseObjectArray(String arrayContent) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        int start = 0;
        int bracketCount = 0;
        StringBuilder currentObject = new StringBuilder();
        
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            
            if (c == '{') {
                bracketCount++;
            } else if (c == '}') {
                bracketCount--;
                if (bracketCount == 0) {
                    currentObject.append(c);
                    String objStr = currentObject.toString().trim();
                    if (!objStr.isEmpty()) {
                        Map<String, Object> obj = new HashMap<>();
                        parseKeyValuePairs(objStr.substring(1, objStr.length() - 1), obj);
                        result.add(obj);
                    }
                    currentObject = new StringBuilder();
                }
            }
            
            if (bracketCount > 0 || c == '{') {
                currentObject.append(c);
            }
        }
        
        return result;
    }
    
    
    private NPC createNPCFromMap(Map<String, Object> npcData) {
        try {
            int id = ((Number) npcData.get("id")).intValue();
            int x = ((Number) npcData.get("x")).intValue();
            int y = ((Number) npcData.get("y")).intValue();
            String type = (String) npcData.get("type");
            String behaviorStr = (String) npcData.get("behavior");
            
            NPC.NPCBehavior behavior = NPC.NPCBehavior.valueOf(behaviorStr.toUpperCase());
            
            return new NPC(id, x, y, type, behavior);
        } catch (Exception e) {
            System.err.println("Erro ao criar NPC a partir do mapa: " + e.getMessage());
            return null;
        }
    }
}