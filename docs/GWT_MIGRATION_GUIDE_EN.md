# Tactics 5x5 - TeaVM to GWT Migration Guide

## 📋 Overview

This document describes how to migrate the Tactics 5x5 web client from TeaVM to GWT (Google Web Toolkit).

### Why Migrate?

| Aspect | TeaVM | GWT |
|--------|-------|-----|
| LibGDX Support | Third-party | **Official** |
| Stability | Reflection issues | **Mature & Stable** |
| Documentation | Limited | **Extensive** |
| Community | Small | **Large** |

> ⚠️ **Note**: As of late 2024, LibGDX's gdx-liftoff tool actually [recommends TeaVM](https://github.com/libgdx/gdx-liftoff/releases) for most web targeting use cases. GWT remains an option for projects that need mature, official support.

### Migration Scope

| Module | Impact |
|--------|--------|
| Core Engine (src/) | ❌ No changes |
| client-libgdx/core | ⚠️ Minor modifications |
| client-libgdx/desktop | ❌ No changes |
| client-libgdx/android | ❌ No changes |
| client-libgdx/teavm | ❌ Keep (unused) |
| client-libgdx/html | ✅ **New module** |

---

## 📁 Current Project Information

```
LibGDX Version: 1.12.1
Java Version: 11 (source/target)
Runtime: OpenJDK 17

Existing Modules:
├── core/      - Shared client code
├── desktop/   - Desktop backend (LWJGL3)
├── android/   - Android backend
└── teavm/     - Web backend (TeaVM) - to be replaced
```

---

## 🚀 Migration Steps

### Phase 1: Add GWT Module

#### Step 1.1: Update settings.gradle

```groovy
// client-libgdx/settings.gradle
rootProject.name = 'tactics-client'
include 'core', 'desktop', 'android', 'teavm', 'html'  // Add 'html'
```

#### Step 1.2: Update Root build.gradle

Add the following after the `allprojects` block in `client-libgdx/build.gradle`:

```groovy
project(":html") {
    apply plugin: "java-library"
    apply plugin: "gwt"
    apply plugin: "war"

    dependencies {
        implementation project(":core")
        implementation "com.badlogicgames.gdx:gdx:$gdxVersion:sources"
        implementation "com.badlogicgames.gdx:gdx-backend-gwt:$gdxVersion"
        implementation "com.badlogicgames.gdx:gdx-backend-gwt:$gdxVersion:sources"
    }
}
```

#### Step 1.3: Add GWT Plugin

Add at the top of `client-libgdx/build.gradle`:

```groovy
buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath 'org.gretty:gretty:3.1.0'
        // Option 1: Original plugin (older but stable)
        classpath 'org.wisepersist:gwt-gradle-plugin:1.1.19'
        // Option 2: Updated fork (supports Gradle 8.x, GWT 2.12+)
        // classpath 'org.docstr:gwt-gradle-plugin:2.2.5'
    }
}
```

> ⚠️ **Plugin Choice**:
> - `org.wisepersist:gwt-gradle-plugin:1.1.19` - Stable but older
> - `org.docstr:gwt-gradle-plugin:2.2.5` - [Latest fork](https://github.com/jiakuan/gwt-gradle-plugin), supports Gradle 8.x+
>
> Choose based on your Gradle version. LibGDX 1.12.x typically uses Gradle 8.x.

---

### Phase 2: Create HTML Module Structure

#### Step 2.1: Create Directory Structure

```bash
mkdir -p client-libgdx/html/src/main/java/com/tactics/client/gwt
mkdir -p client-libgdx/html/src/main/webapp
mkdir -p client-libgdx/html/src/main/webapp/WEB-INF
```

#### Step 2.2: Create html/build.gradle

```groovy
// client-libgdx/html/build.gradle

plugins {
    id 'java-library'
    id 'war'
    id 'org.gretty' version '3.1.0'
}

// Apply GWT plugin from buildscript (don't use plugins block for GWT)
apply plugin: 'gwt'

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

sourceSets {
    main {
        java {
            srcDirs = ['src/main/java', '../core/src/main/java']
        }
        resources {
            srcDirs = ['src/main/resources', '../assets']
        }
    }
}

gwt {
    gwtVersion = '2.10.0'
    modules 'com.tactics.client.GdxDefinition'
    devModules 'com.tactics.client.GdxDefinitionSuperdev'
    
    compiler {
        strict = true
        style = 'OBF'  // DETAILED, PRETTY, or OBF (obfuscated)
        optimize = 9
        enableClosureCompiler = true
        disableCastChecking = true
    }
}

dependencies {
    implementation project(":core")
    implementation "com.badlogicgames.gdx:gdx:${gdxVersion}"
    implementation "com.badlogicgames.gdx:gdx:${gdxVersion}:sources"
    implementation "com.badlogicgames.gdx:gdx-backend-gwt:${gdxVersion}"
    implementation "com.badlogicgames.gdx:gdx-backend-gwt:${gdxVersion}:sources"
}

gretty {
    contextPath = '/'
    extraResourceBase 'src/main/webapp'
}

task superDev(type: org.wisepersist.gradle.plugins.gwt.GwtSuperDev) {
    workDir = file("${buildDir}/superdev")
    doFirst {
        gwt.modules = gwt.devModules
    }
}

task dist(dependsOn: [compileGwt]) {
    doLast {
        copy {
            from('src/main/webapp') {
                exclude 'index.html'
            }
            into "${buildDir}/dist"
        }
        copy {
            from('src/main/webapp') {
                include 'index.html'
            }
            into "${buildDir}/dist"
            filter { line -> 
                line.replace('<!--GWTNOSCRIPT-->', '<script src="html/html.nocache.js"></script>')
            }
        }
        copy {
            from("${buildDir}/gwt/out")
            into "${buildDir}/dist"
        }
    }
}

// Clean task
task cleanDist(type: Delete) {
    delete "${buildDir}/dist"
}
```

---

### Phase 3: Create GWT Module Definition Files

#### Step 3.1: Main Module Definition

Create `html/src/main/java/com/tactics/client/GdxDefinition.gwt.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE module PUBLIC "-//Google Inc.//DTD Google Web Toolkit 2.10.0//EN"
        "http://www.gwtproject.org/doctype/2.10.0/gwt-module.dtd">
<module rename-to="html">
    <inherits name='com.badlogic.gdx.backends.gdx_backends_gwt' />
    <inherits name="com.google.gwt.json.JSON" />
    
    <!-- Entry point -->
    <entry-point class='com.tactics.client.gwt.GwtLauncher' />
    
    <!-- Source paths -->
    <source path="client" />
    <source path="shared" />
    
    <!-- Enable reflection for specific classes if needed -->
    <extend-configuration-property name="gdx.reflect.include" value="com.tactics" />
    
    <!-- Set default locale -->
    <set-property name="user.agent" value="gecko1_8, safari" />
    
    <!-- Optimize -->
    <set-property name="compiler.stackMode" value="strip" />
    <set-configuration-property name="devModeRedirectEnabled" value="true" />
</module>
```

#### Step 3.2: SuperDev Module Definition

Create `html/src/main/java/com/tactics/client/GdxDefinitionSuperdev.gwt.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE module PUBLIC "-//Google Inc.//DTD Google Web Toolkit 2.10.0//EN"
        "http://www.gwtproject.org/doctype/2.10.0/gwt-module.dtd">
<module rename-to="html">
    <inherits name="com.tactics.client.GdxDefinition" />
    
    <set-configuration-property name="devModeRedirectEnabled" value="true"/>
    <set-property name="compiler.useSourceMaps" value="true" />
</module>
```

---

### Phase 4: Create GWT Launcher

#### Step 4.1: GwtLauncher.java

Create `html/src/main/java/com/tactics/client/gwt/GwtLauncher.java`:

```java
package com.tactics.client.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.tactics.client.TacticsGame;

public class GwtLauncher extends GwtApplication {
    
    @Override
    public GwtApplicationConfiguration getConfig() {
        GwtApplicationConfiguration config = new GwtApplicationConfiguration(true);
        config.padVertical = 0;
        config.padHorizontal = 0;
        config.antialiasing = true;
        return config;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new TacticsGame();
    }
}
```

---

### Phase 5: Create GWT WebSocket Client

#### Step 5.1: GwtWebSocketClient.java

Create `html/src/main/java/com/tactics/client/gwt/GwtWebSocketClient.java`:

```java
package com.tactics.client.gwt;

import com.google.gwt.core.client.JavaScriptObject;
import com.tactics.client.net.IWebSocketClient;
import com.tactics.client.net.WebSocketListener;

import java.util.LinkedList;
import java.util.Queue;

/**
 * GWT-specific WebSocket implementation.
 * Implements IWebSocketClient interface for platform abstraction.
 */
public class GwtWebSocketClient implements IWebSocketClient {

    private JavaScriptObject socket;
    private WebSocketListener listener;
    private boolean connected = false;
    private boolean autoReconnect = false;
    private String lastUrl;

    // Thread-safe message queue (GWT is single-threaded, but keeps API consistent)
    private final Queue<String> messageQueue = new LinkedList<>();

    @Override
    public void connect(String url) {
        this.lastUrl = url;
        createWebSocket(url);
    }

    @Override
    public void send(String message) {
        if (connected && socket != null) {
            sendNative(socket, message);
        }
    }

    @Override
    public void disconnect() {
        autoReconnect = false;  // Disable auto-reconnect on manual disconnect
        if (socket != null) {
            closeNative(socket);
            socket = null;
            connected = false;
        }
    }

    @Override
    public void setListener(WebSocketListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void setAutoReconnect(boolean enabled) {
        this.autoReconnect = enabled;
    }

    @Override
    public String pollMessage() {
        return messageQueue.poll();
    }

    // Native JavaScript methods (JSNI)

    private native void createWebSocket(String url) /*-{
        var self = this;
        var ws = new WebSocket(url);

        ws.onopen = function(event) {
            self.@com.tactics.client.gwt.GwtWebSocketClient::onOpen()();
        };

        ws.onmessage = function(event) {
            var data = event.data;
            self.@com.tactics.client.gwt.GwtWebSocketClient::onMessage(Ljava/lang/String;)(data);
        };

        ws.onclose = function(event) {
            self.@com.tactics.client.gwt.GwtWebSocketClient::onClose()();
        };

        ws.onerror = function(event) {
            self.@com.tactics.client.gwt.GwtWebSocketClient::onError(Ljava/lang/String;)("WebSocket error");
        };

        this.@com.tactics.client.gwt.GwtWebSocketClient::socket = ws;
    }-*/;

    private native void sendNative(JavaScriptObject socket, String message) /*-{
        if (socket && socket.readyState === 1) {
            socket.send(message);
        }
    }-*/;

    private native void closeNative(JavaScriptObject socket) /*-{
        if (socket) {
            socket.close();
        }
    }-*/;

    // Callbacks from JavaScript

    private void onOpen() {
        connected = true;
        if (listener != null) {
            listener.onConnected();
        }
    }

    private void onMessage(String data) {
        if (data != null) {
            // Add to queue for thread-safe polling
            messageQueue.add(data);
            // Also notify listener for immediate handling if needed
            if (listener != null) {
                listener.onMessage(data);
            }
        }
    }

    private void onClose() {
        connected = false;
        if (listener != null) {
            listener.onDisconnected();
        }
        // Auto-reconnect logic
        if (autoReconnect && lastUrl != null) {
            scheduleReconnect();
        }
    }

    private void onError(String error) {
        if (listener != null) {
            listener.onError(error);
        }
    }

    private native void scheduleReconnect() /*-{
        var self = this;
        $wnd.setTimeout(function() {
            var url = self.@com.tactics.client.gwt.GwtWebSocketClient::lastUrl;
            if (url) {
                self.@com.tactics.client.gwt.GwtWebSocketClient::connect(Ljava/lang/String;)(url);
            }
        }, 3000);  // Reconnect after 3 seconds
    }-*/;
}
```

---

### Phase 6: Create Webapp Resources

#### Step 6.1: index.html

Create `html/src/main/webapp/index.html`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
    <title>Tactics 5x5</title>
    <style>
        html, body {
            margin: 0;
            padding: 0;
            width: 100%;
            height: 100%;
            overflow: hidden;
            background-color: #1a1a2e;
        }
        canvas {
            display: block;
            touch-action: none;
        }
        #loading {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            color: white;
            font-family: Arial, sans-serif;
            font-size: 24px;
        }
    </style>
</head>
<body>
    <div id="loading">Loading Tactics 5x5...</div>
    
    <!--GWTNOSCRIPT-->
    <script src="html/html.nocache.js"></script>
</body>
</html>
```

#### Step 6.2: WEB-INF/web.xml

Create `html/src/main/webapp/WEB-INF/web.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE web-app PUBLIC 
    "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN" 
    "http://java.sun.com/dtd/web-app_2_3.dtd">
<web-app>
    <welcome-file-list>
        <welcome-file>index.html</welcome-file>
    </welcome-file-list>
</web-app>
```

---

### Phase 7: Modify Core Module

> **Note**: The core module already has well-designed interfaces. This phase requires minimal changes.

#### Step 7.1: Update TextRenderer Platform Detection

Location: `core/src/main/java/com/tactics/client/util/TextRenderer.java`

The current implementation only checks for TeaVM. Update to also detect GWT:

```java
public class TextRenderer {

    private static Boolean isWeb = null;
    private static final boolean IS_WEB;

    static {
        IS_WEB = detectWebBuild();
    }

    /**
     * Check if running in a web environment (TeaVM or GWT).
     * @return true if web build, false for desktop/android
     */
    public static boolean isWebBuild() {
        return IS_WEB;
    }

    private static boolean detectWebBuild() {
        // Check for GWT first
        try {
            Class.forName("com.google.gwt.core.client.GWT");
            return true;
        } catch (ClassNotFoundException e1) {
            // Check for TeaVM
            try {
                Class.forName("org.teavm.jso.JSObject");
                return true;
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }

    // ... rest of the code remains unchanged
}
```

#### Step 7.2: Existing Interfaces (No Changes Needed)

The following interfaces already exist and are correct:

**`core/src/main/java/com/tactics/client/net/IWebSocketClient.java`**:
```java
package com.tactics.client.net;

public interface IWebSocketClient {
    void connect(String url);
    void send(String message);
    void disconnect();
    void setListener(WebSocketListener listener);
    boolean isConnected();
    void setAutoReconnect(boolean enabled);
    String pollMessage();
}
```

**`core/src/main/java/com/tactics/client/net/WebSocketListener.java`**:
```java
package com.tactics.client.net;

public interface WebSocketListener {
    void onConnected();
    void onMessage(String message);
    void onDisconnected();  // Note: No reason parameter
    void onError(String error);
}
```

#### Step 7.3: Existing WebSocketFactory (No Changes Needed)

The factory pattern already exists at `core/src/main/java/com/tactics/client/net/WebSocketFactory.java`:

```java
package com.tactics.client.net;

public class WebSocketFactory {

    private static IWebSocketClient instance;
    private static WebSocketClientCreator creator;

    public interface WebSocketClientCreator {
        IWebSocketClient create();
    }

    public static void registerCreator(WebSocketClientCreator creator) {
        WebSocketFactory.creator = creator;
    }

    public static IWebSocketClient create() {
        if (creator == null) {
            throw new IllegalStateException(
                "No WebSocket client creator registered. " +
                "Call WebSocketFactory.registerCreator() in your platform launcher.");
        }
        return creator.create();
    }

    public static IWebSocketClient getInstance() {
        if (instance == null) {
            instance = create();
        }
        return instance;
    }

    public static void clearInstance() {
        if (instance != null) {
            instance.disconnect();
            instance = null;
        }
    }
}
```

#### Step 7.4: Update GwtLauncher to Register WebSocket Client

Update `html/src/main/java/com/tactics/client/gwt/GwtLauncher.java`:

```java
package com.tactics.client.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.tactics.client.TacticsGame;
import com.tactics.client.net.WebSocketFactory;

public class GwtLauncher extends GwtApplication {

    @Override
    public GwtApplicationConfiguration getConfig() {
        GwtApplicationConfiguration config = new GwtApplicationConfiguration(true);
        config.padVertical = 0;
        config.padHorizontal = 0;
        config.antialiasing = true;
        return config;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        // Register GWT-specific WebSocket client creator
        WebSocketFactory.registerCreator(GwtWebSocketClient::new);
        return new TacticsGame();
    }
}
```

> **Pattern Note**: This follows the same pattern used by Desktop and TeaVM launchers:
> - Desktop: `WebSocketFactory.registerCreator(DesktopWebSocketClient::new);`
> - TeaVM: `WebSocketFactory.registerCreator(TeaVMWebSocketClient::new);`

---

### Phase 8: Handle GWT Special Cases

#### Step 8.1: GWT Unsupported Features

The following features require special handling in GWT:

| Feature | Status | Solution |
|---------|--------|----------|
| Reflection | ⚠️ Limited | Use GWT.create() |
| BitmapFont | ⚠️ Requires font files | Use .fnt + .png |
| Multi-threading | ❌ Not supported | Use callbacks |
| File system | ❌ Not supported | Use assets |

#### Step 8.2: Create GWT Reflection Configuration (if needed)

If using JSON serialization, add to `GdxDefinition.gwt.xml`:

```xml
<extend-configuration-property name="gdx.reflect.include" value="com.tactics.client.model" />
```

---

### Phase 9: Compile and Test

#### Step 9.1: Compile GWT

```bash
cd /mnt/d/blueStack/Tactics5x5/client-libgdx

# Full compile
./gradlew html:compileGwt

# Or use dist task
./gradlew html:dist
```

#### Step 9.2: Development Mode (Super Dev Mode)

```bash
./gradlew html:superDev
```

Then open http://localhost:9876

#### Step 9.3: Test WAR

```bash
./gradlew html:war
```

#### Step 9.4: Quick Test

```bash
# Run with Gretty
./gradlew html:appRun

# Or use Python HTTP server
cd html/build/dist
python3 -m http.server 8000
```

---

## 📋 Checklist

### Phase 1-2: Basic Setup
- [x] Update settings.gradle (add 'html' module)
- [x] Update root build.gradle (add GWT/Gretty plugins)
- [x] Create html directory structure
- [x] Create html/build.gradle

### Phase 3-4: GWT Configuration
- [x] Create GdxDefinition.gwt.xml
- [x] Create GdxDefinitionSuperdev.gwt.xml
- [x] Create GwtLauncher.java

### Phase 5: WebSocket
- [x] Create GwtWebSocketClient.java (implements `IWebSocketClient`)
- [x] Verify `WebSocketFactory.registerCreator()` pattern in launcher

### Phase 6: Webapp
- [x] Create index.html
- [x] Create web.xml

### Phase 7: Core Modifications
- [x] Update TextRenderer platform detection (add GWT check)
- [x] Verify `IWebSocketClient` interface exists ✓ (already exists)
- [x] Verify `WebSocketListener` interface exists ✓ (already exists)
- [x] Verify `WebSocketFactory` exists ✓ (already exists)

### Phase 8-9: Testing
- [x] Successfully run ./gradlew html:compileGwt
- [ ] Browser test without errors
- [ ] WebSocket connection successful
- [ ] Complete game flow test (Draft → Battle → Result)

---

## 🔧 Troubleshooting

### Q1: Compile Error "No source code available"

**Cause**: GWT requires .java source files

**Solution**: Ensure `build.gradle` `sourceSets` includes core sources:
```groovy
srcDirs = ['src/main/java', '../core/src/main/java']
```

### Q2: Runtime Error "Class not found"

**Cause**: GWT reflection limitations

**Solution**: Add to `.gwt.xml`:
```xml
<extend-configuration-property name="gdx.reflect.include" value="your.package" />
```

### Q3: WebSocket Connection Failed

**Cause**: CORS or URL error

**Solution**: 
1. Ensure server allows CORS
2. Verify WebSocket URL is correct (`ws://localhost:8080/ws`)

### Q4: Font Display Issues

**Cause**: BitmapFont requires .fnt and .png files

**Solution**:
1. Use Hiero tool to generate font files
2. Or use placeholder shapes (current approach)

> ✅ **Good News**: This project already has `default.fnt` and `default.png` in the assets folder.

### Q5: LinkedList Not Found / Collection Issues

**Cause**: GWT has limited JRE emulation

**Solution**: Use GWT-compatible collections. `LinkedList` is supported, but some methods may not be.

### Q6: Method References (::) Not Working

**Cause**: Lambda/method reference support depends on GWT version

**Solution**:
- Ensure GWT 2.10+ (included in LibGDX 1.12+)
- If issues persist, use anonymous classes:
```java
// Instead of:
WebSocketFactory.registerCreator(GwtWebSocketClient::new);

// Use:
WebSocketFactory.registerCreator(new WebSocketClientCreator() {
    @Override
    public IWebSocketClient create() {
        return new GwtWebSocketClient();
    }
});
```

---

## ⚠️ GWT Limitations

Important limitations to be aware of:

| Feature | Support | Notes |
|---------|---------|-------|
| Reflection | ⚠️ Limited | Use `gdx.reflect.include` config |
| Multi-threading | ❌ None | JavaScript is single-threaded |
| File system | ❌ None | Use assets only |
| System.nanoTime() | ❌ None | Use `TimeUtils.nanoTime()` |
| Sound before interaction | ❌ None | Requires user click first |
| Fullscreen on iOS | ❌ None | Browser limitation |

---

## 📚 References

- [LibGDX HTML5/GWT Backend](https://libgdx.com/wiki/html5-backend-and-gwt-specifics)
- [GWT Official Documentation](http://www.gwtproject.org/doc/latest/DevGuide.html)
- [LibGDX Versions & Dependencies](https://libgdx.com/dev/versions/)
- [gdx-liftoff Releases](https://github.com/libgdx/gdx-liftoff/releases)
- [gwt-gradle-plugin (original)](https://github.com/nickhristov/gwt-gradle-plugin)
- [gwt-gradle-plugin (updated fork)](https://github.com/jiakuan/gwt-gradle-plugin) - Supports Gradle 8.x+

---

## 📅 Time Estimate

| Phase | Time |
|-------|------|
| Phase 1-2: Basic Setup | 1 hour |
| Phase 3-4: GWT Configuration | 1 hour |
| Phase 5: WebSocket | 1-2 hours |
| Phase 6: Webapp | 30 minutes |
| Phase 7: Core Modifications | 1 hour |
| Phase 8-9: Testing & Debugging | 2-3 hours |
| **Total** | **6-8 hours** |

---

## 📝 Document Revision History

| Date | Changes |
|------|---------|
| 2025-12-10 | Initial version |
| 2025-12-10 | **Review & Corrections**: Fixed interface names (`IWebSocketClient`, `WebSocketListener`), updated method signatures, added missing methods (`setAutoReconnect`, `pollMessage`), corrected package paths (`net` not `network`), added GWT plugin version options, added GWT limitations section |

---

*Document updated: 2025-12-10*
*LibGDX Version: 1.12.1*
*GWT Version: 2.10.0 (LibGDX 1.13+ uses GWT 2.11.0)*
