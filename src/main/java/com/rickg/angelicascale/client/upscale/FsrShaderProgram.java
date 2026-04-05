package com.rickg.angelicascale.client.upscale;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import com.rickg.angelicascale.AngelicaScaleMod;

public final class FsrShaderProgram {

    private final String name;
    private final String vertexResourcePath;
    private final String fragmentResourcePath;
    private final Map<String, Integer> uniformLocations = new HashMap<String, Integer>();
    private final IntBuffer uniform4Buffer = BufferUtils.createIntBuffer(4);

    private int programId;
    private boolean loadAttempted;
    private boolean available;

    public FsrShaderProgram(String name, String vertexResourcePath, String fragmentResourcePath) {
        this.name = name;
        this.vertexResourcePath = vertexResourcePath;
        this.fragmentResourcePath = fragmentResourcePath;
    }

    public boolean ensureLoaded() {
        if (this.loadAttempted) {
            return this.available;
        }

        this.loadAttempted = true;

        try {
            this.programId = this.createProgram();
            this.available = this.programId != 0;
        } catch (IOException e) {
            AngelicaScaleMod.LOG.error("Failed to load shader resources for {}.", this.name, e);
            this.available = false;
        } catch (RuntimeException e) {
            AngelicaScaleMod.LOG.error("Failed to compile shader program {}.", this.name, e);
            this.available = false;
        }

        return this.available;
    }

    public boolean isAvailable() {
        return this.ensureLoaded();
    }

    public void use() {
        OpenGlHelper.func_153161_d(this.programId);
    }

    public void stop() {
        OpenGlHelper.func_153161_d(0);
    }

    public void setUniform1i(String name, int value) {
        int location = this.getUniformLocation(name);

        if (location >= 0) {
            OpenGlHelper.func_153163_f(location, value);
        }
    }

    public void setUniform4i(String name, int[] values) {
        int location = this.getUniformLocation(name);

        if (location < 0) {
            return;
        }

        this.uniform4Buffer.clear();
        this.uniform4Buffer.put(values[0]).put(values[1]).put(values[2]).put(values[3]);
        this.uniform4Buffer.flip();
        OpenGlHelper.func_153162_d(location, this.uniform4Buffer);
    }

    public void destroy() {
        if (this.programId != 0) {
            OpenGlHelper.func_153187_e(this.programId);
            this.programId = 0;
        }

        this.uniformLocations.clear();
        this.available = false;
        this.loadAttempted = false;
    }

    private int getUniformLocation(String name) {
        Integer cached = this.uniformLocations.get(name);

        if (cached != null) {
            return cached.intValue();
        }

        int location = OpenGlHelper.func_153194_a(this.programId, name);
        this.uniformLocations.put(name, Integer.valueOf(location));
        return location;
    }

    private int createProgram() throws IOException {
        int vertexShader = 0;
        int fragmentShader = 0;
        int program = 0;

        try {
            vertexShader = this.compileShader(GL20.GL_VERTEX_SHADER, this.vertexResourcePath);
            fragmentShader = this.compileShader(GL20.GL_FRAGMENT_SHADER, this.fragmentResourcePath);
            program = OpenGlHelper.func_153183_d();

            if (program == 0) {
                throw new RuntimeException("OpenGL returned program id 0");
            }

            OpenGlHelper.func_153178_b(program, vertexShader);
            OpenGlHelper.func_153178_b(program, fragmentShader);
            OpenGlHelper.func_153179_f(program);

            if (OpenGlHelper.func_153175_a(program, GL20.GL_LINK_STATUS) == 0) {
                throw new RuntimeException(OpenGlHelper.func_153166_e(program, 32768));
            }

            return program;
        } catch (RuntimeException e) {
            if (program != 0) {
                OpenGlHelper.func_153187_e(program);
            }

            throw e;
        } finally {
            if (vertexShader != 0) {
                OpenGlHelper.func_153180_a(vertexShader);
            }

            if (fragmentShader != 0) {
                OpenGlHelper.func_153180_a(fragmentShader);
            }
        }
    }

    private int compileShader(int shaderType, String resourcePath) throws IOException {
        String source = this.loadShaderSource(resourcePath, new HashSet<String>());
        int shader = OpenGlHelper.func_153195_b(shaderType);

        if (shader == 0) {
            throw new RuntimeException("OpenGL returned shader id 0");
        }

        byte[] sourceBytes = source.getBytes(StandardCharsets.UTF_8);
        ByteBuffer sourceBuffer = BufferUtils.createByteBuffer(sourceBytes.length + 1);
        sourceBuffer.put(sourceBytes);
        sourceBuffer.put((byte) 0);
        sourceBuffer.flip();

        OpenGlHelper.func_153169_a(shader, sourceBuffer);
        OpenGlHelper.func_153170_c(shader);

        if (OpenGlHelper.func_153157_c(shader, GL20.GL_COMPILE_STATUS) == 0) {
            String infoLog = OpenGlHelper.func_153158_d(shader, 32768);
            OpenGlHelper.func_153180_a(shader);
            throw new RuntimeException(infoLog);
        }

        return shader;
    }

    private String loadShaderSource(String resourcePath, Set<String> includeStack) throws IOException {
        String normalizedPath = normalizePath(resourcePath);

        if (!includeStack.add(normalizedPath)) {
            throw new IOException("Recursive shader include detected: " + normalizedPath);
        }

        try {
            String source = readResource(normalizedPath);
            StringBuilder builder = new StringBuilder(source.length() + 256);
            String[] lines = source.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);

            for (String line : lines) {
                String trimmed = line.trim();

                if (trimmed.startsWith("#include \"") && trimmed.endsWith("\"")) {
                    String includeName = trimmed.substring("#include \"".length(), trimmed.length() - 1);
                    builder.append(this.loadShaderSource(resolveInclude(normalizedPath, includeName), includeStack));
                } else {
                    builder.append(line).append('\n');
                }
            }

            return builder.toString();
        } finally {
            includeStack.remove(normalizedPath);
        }
    }

    private static String readResource(String resourcePath) throws IOException {
        InputStream inputStream = FsrShaderProgram.class.getResourceAsStream(resourcePath);

        if (inputStream == null) {
            throw new IOException("Missing shader resource: " + resourcePath);
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            inputStream.close();
        }
    }

    private static String resolveInclude(String currentPath, String includeName) {
        int lastSeparator = currentPath.lastIndexOf('/');
        String basePath = lastSeparator >= 0 ? currentPath.substring(0, lastSeparator + 1) : "/";
        return normalizePath(basePath + includeName);
    }

    private static String normalizePath(String path) {
        String replaced = path.replace('\\', '/');
        boolean absolute = replaced.startsWith("/");
        String[] parts = replaced.split("/");
        Deque<String> normalized = new ArrayDeque<String>();

        for (String part : parts) {
            if (part.length() == 0 || ".".equals(part)) {
                continue;
            }

            if ("..".equals(part)) {
                if (!normalized.isEmpty()) {
                    normalized.removeLast();
                }

                continue;
            }

            normalized.addLast(part);
        }

        StringBuilder builder = new StringBuilder();

        if (absolute) {
            builder.append('/');
        }

        boolean first = true;

        for (String part : normalized) {
            if (!first) {
                builder.append('/');
            }

            builder.append(part);
            first = false;
        }

        return builder.toString();
    }
}
