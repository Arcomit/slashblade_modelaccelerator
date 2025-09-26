package mod.acomit.slashblade_modelaccelerator.obj;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @Author: Arcomit
 * @CreateTime: 2025-08-05 12:44
 * @Description: Wavefront OBJ模型解析加载器
 */
@OnlyIn(Dist.CLIENT)
public class ObjReader {

    private static final Pattern VERTEX_PATTERN        = Pattern.compile("(v( (-)?\\d+(\\.\\d+)?){3,4} *\\n)|(v( (-)?\\d+(\\.\\d+)?){3,4} *$)");
    private static final Pattern VERTEX_NORMAL_PATTERN = Pattern.compile("(vn( (-)?\\d+(\\.\\d+)?){3,4} *\\n)|(vn( (-)?\\d+(\\.\\d+)?){3,4} *$)");
    private static final Pattern VERTEX_UV_PATTERN     = Pattern.compile("(vt( (-)?\\d+\\.\\d+){2,3} *\\n)|(vt( (-)?\\d+(\\.\\d+)?){2,3} *$)");

    private static final Pattern FACE_VERTEX_UV_NORMAL_INDEX_PATTERN = Pattern.compile("(f( \\d+/\\d+/\\d+){3,4} *\\n)|(f( \\d+/\\d+/\\d+){3,4} *$)");
    private static final Pattern FACE_VERTEX_UV_INDEX_PATTERN        = Pattern.compile("(f( \\d+/\\d+){3,4} *\\n)|(f( \\d+/\\d+){3,4} *$)");
    private static final Pattern FACE_VERTEX_NORMAL_INDEX_PATTERN    = Pattern.compile("(f( \\d+//\\d+){3,4} *\\n)|(f( \\d+//\\d+){3,4} *$)");
    private static final Pattern FACE_VERTEX_INDEX_PATTERN           = Pattern.compile("(f( \\d+){3,4} *\\n)|(f( \\d+){3,4} *$)");

    /**
     * 根据Resource读取OBJ文件并解析其内容。
     * @param  resource            资源对象。
     * @throws IOException         如果读取或解析过程中发生错误。
     * @throws ModelParseException 如果解析过程中遇到格式错误。
     */
    public  ObjReader(Resource resource) throws IOException, ModelParseException {
        parse(resource.open());
    }

    /**
     * 根据ResourceLocation读取OBJ文件并解析其内容。
     * @param  resourceLocation    资源位置。
     * @throws IOException         如果读取或解析过程中发生错误。
     * @throws ModelParseException 如果解析过程中遇到格式错误。
     */
    public  ObjReader(ResourceLocation resourceLocation) throws IOException, ModelParseException {
        parse(Minecraft.getInstance().getResourceManager().open(resourceLocation));
    }

    @Getter private final ObjModel             model        = new ObjModel   ();
    private         final List<SimpleVector3f> positions    = new ArrayList<>();
    private         final List<SimpleVector3f> normals      = new ArrayList<>();
    private         final List<SimpleVector3f> uvs          = new ArrayList<>();
    private               ObjGroup             currentGroup = model.getGroups().computeIfAbsent("Default", ObjGroup::new);

    private void parse(InputStream inputStream) throws IOException, ModelParseException {
        BufferedReader reader  = new BufferedReader(new InputStreamReader(inputStream));
        int            lineNum = 0;
        String         currentLine;

        while ((currentLine = reader.readLine()) != null) {
            lineNum++;
            currentLine = currentLine.replaceAll("\\s+", " ").trim();
            processLine(currentLine, lineNum);
        }
    }

    private void processLine(String currentLine, int lineNum) throws ModelParseException {
        if (currentLine.isEmpty() || currentLine.startsWith("#")) {
            return; // 跳过空行和注释
        }

        String[] tokens = currentLine.split("\\s+");
        if (tokens.length == 0) {
            return;
        }

        switch (tokens[0]) {
            case "v":  parsePosition(tokens, lineNum); break;
            case "vt": parseUv      (tokens, lineNum); break;
            case "vn": parseNormal  (tokens, lineNum); break;
            case "f" : parseFace    (tokens, lineNum); break;
            case "g" :
            case "o" : startNewGroup(tokens, lineNum); break;
            default  :                                 break;
        }
    }

    private void parsePosition(String[] tokens, int lineNum) throws ModelParseException {
        if (!VERTEX_PATTERN.matcher(String.join(" ", tokens)).matches()) {
            throw new ModelParseException("Invalid vertex format", lineNum);
        }

        try {
            float x = Float.parseFloat(tokens[1]);
            float y = Float.parseFloat(tokens[2]);
            float z = tokens.length >= 4 ? Float.parseFloat(tokens[3]) : 0.0f;

            positions.add(new SimpleVector3f(x, y, z));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new ModelParseException("Vertex data parsing failure", lineNum, e);
        }
    }

    private void parseNormal(String[] tokens, int lineNum) throws ModelParseException {
        if (!VERTEX_NORMAL_PATTERN.matcher(String.join(" ", tokens)).matches()) {
            throw new ModelParseException("Invalid normal vector format", lineNum);
        }

        try {
            float x = Float.parseFloat(tokens[1]);
            float y = Float.parseFloat(tokens[2]);
            float z = Float.parseFloat(tokens[3]);

            normals.add(new SimpleVector3f(x, y, z));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new ModelParseException("Normal data parsing failure", lineNum, e);
        }
    }

    private void parseUv(String[] tokens, int lineNum) throws ModelParseException {
        if (!VERTEX_UV_PATTERN.matcher(String.join(" ", tokens)).matches()) {
            throw new ModelParseException("Invalid UV coordinate format", lineNum);
        }

        try {
            float u = Float.parseFloat(tokens[1]);
            float v = 1.0f - Float.parseFloat(tokens[2]); // 反转V
            float w = tokens.length >= 4 ? Float.parseFloat(tokens[3]) : 0.0f;

            uvs.add(new SimpleVector3f(u, v, w));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new ModelParseException("UV coordinates data parsing failure", lineNum, e);
        }
    }

    private void parseFace(String[] tokens, int lineNum) throws ModelParseException {
        if (tokens.length >= 5){
            throw new ModelParseException("Non-triangular face detected", lineNum);
            // TODO: 自动模型三角化
        }

        ObjFace  face1 = new ObjFace();
        String[] subTokens;
        // f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3 ...
        if (FACE_VERTEX_UV_NORMAL_INDEX_PATTERN.matcher(String.join(" ", tokens)).matches()) {
            face1.vertices      = new SimpleVector3f[tokens.length - 1];
            face1.vertexUvs = new SimpleVector3f[tokens.length - 1];
            face1.vertexNormals = new SimpleVector3f[tokens.length - 1];

            for (int i = 0; i < tokens.length - 1; ++i) {
                subTokens              = tokens[i + 1].split("/");
                face1.vertices     [i] = positions.get(Integer.parseInt(subTokens[0]) - 1);
                face1.vertexUvs[i] = uvs      .get(Integer.parseInt(subTokens[1]) - 1);
                face1.vertexNormals[i] = normals  .get(Integer.parseInt(subTokens[2]) - 1);
            }

            face1.faceNormal = face1.computeUnitNormal();
        }

        // f v1/vt1 v2/vt2 v3/vt3 ...
        if (FACE_VERTEX_UV_INDEX_PATTERN.matcher(String.join(" ", tokens)).matches()) {
            face1.vertices    = new SimpleVector3f[tokens.length - 1];
            face1.vertexUvs = new SimpleVector3f[tokens.length - 1];

            for (int i = 0; i < tokens.length - 1; ++i) {
                subTokens            = tokens[i + 1].split("/");
                face1.vertices   [i] = positions.get(Integer.parseInt(subTokens[0]) - 1);
                face1.vertexUvs[i] = uvs      .get(Integer.parseInt(subTokens[1]) - 1);
            }

            face1.faceNormal = face1.computeUnitNormal();
        }

        // f v1//vn1 v2//vn2 v3//vn3 ...
        if (FACE_VERTEX_NORMAL_INDEX_PATTERN.matcher(String.join(" ", tokens)).matches()) {
            face1.vertices      = new SimpleVector3f[tokens.length - 1];
            face1.vertexNormals = new SimpleVector3f[tokens.length - 1];

            for (int i = 0; i < tokens.length - 1; ++i) {
                subTokens              = tokens[i + 1].split("/");
                face1.vertices     [i] = positions.get(Integer.parseInt(subTokens[0]) - 1);
                face1.vertexNormals[i] = normals  .get(Integer.parseInt(subTokens[1]) - 1);
            }

            face1.faceNormal = face1.computeUnitNormal();
        }

        // f v1 v2 v3 ...
        if (FACE_VERTEX_INDEX_PATTERN.matcher(String.join(" ", tokens)).matches()) {
            face1.vertices = new SimpleVector3f[tokens.length - 1];

            for (int i = 0; i < tokens.length - 1; ++i) {
                face1.vertices[i] = positions.get(Integer.parseInt(tokens[i + 1]) - 1);
            }

            face1.faceNormal = face1.computeUnitNormal();
        }

        if (face1.vertices != null) {
            currentGroup.getFaces().add(face1);
            currentGroup.setVertexCount(currentGroup.getVertexCount() + face1.vertices.length);
        } else {
            throw new ModelParseException("Invalid face data", lineNum);
        }
    }

    private void startNewGroup(String[] tokens, int lineNum) {
        String groupName    = tokens.length > 1 ? tokens[1] : "group_" + lineNum;
               currentGroup = model.getGroups().computeIfAbsent(groupName, ObjGroup::new);
    }
}
