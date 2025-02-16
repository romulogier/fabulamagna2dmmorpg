//TiledMapRenderer.java
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.util.Base64;

public class TiledMapRenderer extends JPanel {
    private BufferedImage tilesetImage;
    private int mapWidth;
    private int mapHeight;
    private int tileWidth;
    private int tileHeight;
    private int[][] layers;
    private final List<Rectangle> colliders;
    private final Map<Integer, Rectangle> tilesetCoordinates;
    private Camera camera;
    
    public void setCamera(Camera camera) {
        this.camera = camera;
    }
    
    public int getMapWidth() {
        return mapWidth;
    }
    
    public int getMapHeight() {
        return mapHeight;
    }
    
    public int getTileWidth() {
        return tileWidth;
    }
    
    public int getTileHeight() {
        return tileHeight;
    }
    
    public TiledMapRenderer() {
        this.colliders = new ArrayList<>();
        this.tilesetCoordinates = new HashMap<>();
    }
    
    private float parseFloatAttribute(Element element, String attribute, float defaultValue) {
        String value = element.getAttribute(attribute);
        if (value == null || value.trim().isEmpty()) {
            System.out.println("Atenção: " + attribute + " atributo está vazio e utilizará o valor definido: " + defaultValue);
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            System.out.println("Atenção: " + attribute + " atributo está vazio e utilizará o valor definido: " + defaultValue);
            return defaultValue;
        }
    }
    
    // Método público para carregar colisores de um mapa
    public List<Rectangle> loadColliders(String mapPath) {
        List<Rectangle> tempColliders = new ArrayList<>();
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(mapPath));
            
            NodeList objectGroups = doc.getElementsByTagName("objectgroup");
            for (int i = 0; i < objectGroups.getLength(); i++) {
                Element objectGroup = (Element) objectGroups.item(i);
                if ("colisores".equals(objectGroup.getAttribute("name"))) {
                    NodeList objects = objectGroup.getElementsByTagName("object");
                    for (int j = 0; j < objects.getLength(); j++) {
                        Element object = (Element) objects.item(j);
                        
                        int x = (int) parseFloatAttribute(object, "x", 0);
                        int y = (int) parseFloatAttribute(object, "y", 0);
                        int width = (int) parseFloatAttribute(object, "width", 32);
                        int height = (int) parseFloatAttribute(object, "height", 32);
                        
                        tempColliders.add(new Rectangle(x, y, width, height));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar colisores: " + mapPath);
            e.printStackTrace();
        }
        
        return tempColliders;
    }
    
    public void loadMap(String mapPath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(mapPath));
            
            // Carregar propriedades básicas do mapa
            Element mapElement = doc.getDocumentElement();
            mapWidth = Integer.parseInt(mapElement.getAttribute("width"));
            mapHeight = Integer.parseInt(mapElement.getAttribute("height"));
            tileWidth = Integer.parseInt(mapElement.getAttribute("tilewidth"));
            tileHeight = Integer.parseInt(mapElement.getAttribute("tileheight"));
            
            // Carregar tileset
            NodeList tilesets = doc.getElementsByTagName("tileset");
            Element tileset = (Element) tilesets.item(0);
            Element image = (Element) tileset.getElementsByTagName("image").item(0);
            String tilesetPath = image.getAttribute("source");
            
            // Ajustar caminho do tileset (assumindo que está em uma pasta resources)
            tilesetPath = tilesetPath.replace("../mapresources/", "/resources/");
            tilesetImage = ImageIO.read(getClass().getResourceAsStream(tilesetPath));
            
            // Inicializar coordenadas do tileset
            int tilesetCols = tilesetImage.getWidth() / tileWidth;
            int tilesetRows = tilesetImage.getHeight() / tileHeight;
            for (int y = 0; y < tilesetRows; y++) {
                for (int x = 0; x < tilesetCols; x++) {
                    int tileId = y * tilesetCols + x + 1;
                    tilesetCoordinates.put(tileId, new Rectangle(
                        x * tileWidth,
                        y * tileHeight,
                        tileWidth,
                        tileHeight
                    ));
                }
            }
            
            // Carregar camadas
            NodeList layerNodes = doc.getElementsByTagName("layer");
            layers = new int[layerNodes.getLength()][mapWidth * mapHeight];
            
            for (int l = 0; l < layerNodes.getLength(); l++) {
                Element layer = (Element) layerNodes.item(l);
                Element data = (Element) layer.getElementsByTagName("data").item(0);
                String encoding = data.getAttribute("encoding");
                
                if ("base64".equals(encoding)) {
                    String base64Data = data.getTextContent().trim();
                    byte[] decoded = Base64.getDecoder().decode(base64Data);
                    
                    // Converter bytes em IDs de tiles
                    for (int i = 0; i < mapWidth * mapHeight; i++) {
                        int id = decoded[i * 4] & 0xFF |
                                (decoded[i * 4 + 1] & 0xFF) << 8 |
                                (decoded[i * 4 + 2] & 0xFF) << 16 |
                                (decoded[i * 4 + 3] & 0xFF) << 24;
                        layers[l][i] = id;
                    }
                }
            }
            
            // Carregar colisores
            NodeList objectGroups = doc.getElementsByTagName("objectgroup");
            for (int i = 0; i < objectGroups.getLength(); i++) {
                Element objectGroup = (Element) objectGroups.item(i);
                if ("colisores".equals(objectGroup.getAttribute("name"))) {
                    NodeList objects = objectGroup.getElementsByTagName("object");
                    for (int j = 0; j < objects.getLength(); j++) {
                        Element object = (Element) objects.item(j);
                        
                        int x = (int) parseFloatAttribute(object, "x", 0);
                        int y = (int) parseFloatAttribute(object, "y", 0);
                        int width = (int) parseFloatAttribute(object, "width", 32);
                        int height = (int) parseFloatAttribute(object, "height", 32);
                        
                        this.colliders.add(new Rectangle(x, y, width, height));
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erro ao carregar o mapa: " + mapPath);
            e.printStackTrace();
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (camera == null) return;
        
        Point cameraPos = camera.getPosition();
        
        // Calcula a área visível do mapa
        int startTileX = cameraPos.x / tileWidth;
        int startTileY = cameraPos.y / tileHeight;
        int endTileX = startTileX + (getWidth() / tileWidth) + 2;
        int endTileY = startTileY + (getHeight() / tileHeight) + 2;
        
        // Limita às dimensões do mapa
        startTileX = Math.max(0, startTileX);
        startTileY = Math.max(0, startTileY);
        endTileX = Math.min(mapWidth, endTileX);
        endTileY = Math.min(mapHeight, endTileY);
        
        // Renderiza apenas os tiles visíveis
        for (int[] layer : layers) {
            for (int y = startTileY; y < endTileY; y++) {
                for (int x = startTileX; x < endTileX; x++) {
                    int tileId = layer[y * mapWidth + x];
                    if (tileId != 0) {
                        Rectangle tileCoords = tilesetCoordinates.get(tileId);
                        if (tileCoords != null) {
                            g.drawImage(tilesetImage,
                                x * tileWidth - cameraPos.x,
                                y * tileHeight - cameraPos.y,
                                (x + 1) * tileWidth - cameraPos.x,
                                (y + 1) * tileHeight - cameraPos.y,
                                tileCoords.x,
                                tileCoords.y,
                                tileCoords.x + tileCoords.width,
                                tileCoords.y + tileCoords.height,
                                null);
                        }
                    }
                }
            }
        }
    }
    
    public List<Rectangle> getColliders() {
        return new ArrayList<>(this.colliders);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(mapWidth * tileWidth, mapHeight * tileHeight);
    }
}