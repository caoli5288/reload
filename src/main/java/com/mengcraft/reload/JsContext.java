package com.mengcraft.reload;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.mengcraft.reload.util.Artifact;
import lombok.SneakyThrows;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;

public class JsContext {

    private static final JsContext INSTANCE = new JsContext();
    private ScriptEngine jse;

    JsContext() {
        jse = new ScriptEngineManager(JsContext.class.getClassLoader()).getEngineByExtension("js");
        if (jse == null) {
            // Classpath has no js engine
            try {
                Artifact.load(JsContext.class.getClassLoader(), Lists.newArrayList("org.openjdk.nashorn:nashorn-core:15.7",
                        "org.ow2.asm:asm:7.3.1",
                        "org.ow2.asm:asm-commons:7.3.1",
                        "org.ow2.asm:asm-tree:7.3.1",
                        "org.ow2.asm:asm-util:7.3.1"));
                jse = new ScriptEngineManager(JsContext.class.getClassLoader()).getEngineByExtension("js");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Preconditions.checkNotNull(jse, "No js engine found");
        // Project specific
        try {
            jse.eval("yes = true; no = false;");
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }

    @SneakyThrows
    public Object eval(Bindings bindings, String js) {
        return jse.eval(js, bindings);
    }

    public Object eval(String js) {
        try {
            return jse.eval(js);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JsContext getInstance() {
        return INSTANCE;
    }

    public Bindings createBindings() {
        return jse.createBindings();
    }
}
